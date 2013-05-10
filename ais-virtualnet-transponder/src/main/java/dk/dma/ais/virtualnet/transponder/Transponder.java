/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.ais.virtualnet.transponder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.binary.SixbitException;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage6;
import dk.dma.ais.message.AisMessage7;
import dk.dma.ais.message.AisMessageException;
import dk.dma.ais.message.AisStaticCommon;
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.sentence.Abk;
import dk.dma.ais.sentence.Abm;
import dk.dma.ais.sentence.Bbm;
import dk.dma.ais.sentence.Sentence;
import dk.dma.ais.sentence.SentenceException;
import dk.dma.ais.sentence.Vdm;
import dk.dma.ais.transform.CropVdmTransformer;
import dk.dma.ais.transform.VdmVdoTransformer;
import dk.dma.ais.virtualnet.common.message.TargetTableMessage;
import dk.dma.enav.model.geometry.Position;

/**
 * Virtual transponder
 */
@ThreadSafe
public class Transponder extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(Transponder.class);

    private final TransponderConfiguration conf;
    private final TransponderStatus status;
    private final ServerConnection serverConnection;
    private final ServerSocket serverSocket;
    private final TransponderOwnMessage ownMessage;
    private final VdmVdoTransformer vdoTransformer;
    private final CropVdmTransformer cropTransformer;

    private volatile Socket socket;
    private volatile PrintWriter out;
    private Abm abm = new Abm();
    private Bbm bbm = new Bbm();
    private Abk abk = new Abk();
    private int sequence;

    public Transponder(TransponderConfiguration conf) throws IOException {
        this.conf = conf;
        status = new TransponderStatus();
        serverConnection = new ServerConnection(this, conf);
        serverSocket = new ServerSocket(conf.getPort());
        ownMessage = new TransponderOwnMessage(this, conf.getOwnPosInterval());
        vdoTransformer = new VdmVdoTransformer(conf.getOwnMmsi());
        cropTransformer = new CropVdmTransformer();
    }

    /**
     * Data received from network
     * 
     * @param packet
     */
    public void receive(String strPacket) {
        // Make packet and get ais message
        AisPacket packet = AisPacket.from(strPacket);
        AisMessage message;
        try {
            message = packet.getAisMessage();
        } catch (AisMessageException | SixbitException e) {
            LOG.info("Failed to parse message: " + e.getMessage());
            return;
        }

        // Determine own
        boolean own = message.getUserId() == conf.getOwnMmsi();

        // Convert to VDO or VDM
        packet = vdoTransformer.transform(packet);
        if (packet == null) {
            LOG.error("Failed to convert packet " + strPacket);
            return;
        }

        // Crop everything else that VDM/VDO
        packet = cropTransformer.transform(packet);
        if (packet == null) {
            LOG.error("Failed to crop packet " + strPacket);
            return;
        }

        // Maybe the transponder needs to send a binary acknowledge back to the network
        if (message.getMsgId() == 6) {
            AisMessage6 msg6 = (AisMessage6) message;
            if (msg6.getDestination() == conf.getOwnMmsi()) {
                sendBinAck(msg6);
            }
        }

        // Get name from own static
        if (own && message instanceof AisStaticCommon) {
            String name = ((AisStaticCommon) message).getName();
            if (name != null) {
                status.setShipName(name);
            }
        }

        // Handle position
        if (message instanceof IVesselPositionMessage) {
            IVesselPositionMessage posMsg = (IVesselPositionMessage) message;
            if (own) {
                // Save own position message
                ownMessage.setOwnMessage(packet);
                // Save own position if valid
                if (posMsg.isPositionValid()) {
                    status.setOwnPos(posMsg.getPos().getGeoLocation());
                }
            } else {
                // Is this message valid and within radius
                if (!posMsg.isPositionValid()) {
                    return;
                }
                synchronized (this) {
                    Position pos = posMsg.getPos().getGeoLocation();
                    if (status.getOwnPos() == null || pos.rhumbLineDistanceTo(status.getOwnPos()) > conf.getReceiveRadius()) {
                        return;
                    }
                }
            }
        }

        if (status.isClientConnected()) {
            send(packet.getStringMessage());
        }

    }

    /**
     * Send message to client
     * 
     * @param str
     */
    public void send(String str) {
        if (status.isClientConnected()) {
            out.print(str + "\r\n");
            out.flush();
        }
    }

    @Override
    public void start() {
        serverConnection.start();
        ownMessage.start();
        super.start();
    }

    public void shutdown() {
        ownMessage.interrupt();
        this.interrupt();
        serverConnection.shutdown();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
        }
        try {
            this.join(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            status.setClientConnected(false);

            // Wait for connections
            LOG.info("Waiting for connection on port " + conf.getPort());
            try {
                socket = serverSocket.accept();
                LOG.info("Client connected");
            } catch (IOException e) {
                if (!isInterrupted()) {
                    LOG.error("Failed to accept client connection", e);
                }
                return;
            }

            try {
                out = new PrintWriter(socket.getOutputStream());
                status.setClientConnected(true);
                readFromAI();
            } catch (IOException e) {
            }

            try {
                socket.close();
            } catch (IOException e1) {
            }

            LOG.info("Lost connection to client");

        }
    }

    private void readFromAI() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            LOG.info("Read from client: " + line);

            // Ignore everything else than sentences
            if (!Sentence.hasSentence(line)) {
                continue;
            }

            try {
                if (Abm.isAbm(line)) {
                    int result = abm.parse(line);
                    if (result == 0) {
                        handleAbm();
                    } else {
                        continue;
                    }
                }
                if (Bbm.isBbm(line)) {
                    int result = bbm.parse(line);
                    if (result == 0) {
                        handleBbm();
                    } else {
                        continue;
                    }
                }
                if (Vdm.isVdm(line)) {
                    // TODO handle multi line vdm and send unaltered to the network
                }
                abm = new Abm();
                bbm = new Bbm();

            } catch (SixbitException | SentenceException e) {
                LOG.info("ABM or BBM failed: " + e.getMessage() + " line: " + line);
            }

        }

    }

    private void sendBinAck(AisMessage6 msg6) {
        AisMessage7 msg7 = new AisMessage7();
        msg7.setUserId(conf.getOwnMmsi());
        msg7.setDest1(msg6.getUserId());
        msg7.setSeq1(msg6.getSeqNum());
        LOG.info("Sending binary acknowledge: " + msg7);
        sendMessage(msg7, msg6.getSeqNum());
    }

    private void sendMessage(AisMessage message, Integer seq) {
        if (seq == null) {
            seq = sequence;
            sequence = (sequence + 1) % 4;
        }
        String[] sentences;
        try {
            sentences = Vdm.createSentences(message, seq);
        } catch (SixbitException e) {
            LOG.error("Failed to encode message: " + message, e);
            return;
        }
        AisPacket packet = AisPacket.from(StringUtils.join(sentences, "\r\n"));
        LOG.info("Sending VDM to network: " + packet.getStringMessage());
        serverConnection.send(packet);
    }

    private void handleBbm() {
        LOG.info("Reveived complete BBM");
        abk = new Abk();
        abk.setChannel(bbm.getChannel());
        abk.setMsgId(bbm.getMsgId());
        abk.setSequence(bbm.getSequence());

        // Send AisMessage from Bbm
        try {
            sendMessage(bbm.getAisMessage(conf.getOwnMmsi(), 0), bbm.getSequence());
            abk.setResult(Abk.Result.BROADCAST_SENT);
        } catch (Exception e) {
            LOG.info("Error decoding BBM: " + e.getMessage());
            // Something must be wrong with Bbm
            abk.setResult(Abk.Result.COULD_NOT_BROADCAST);
        }

        sendAbk();
    }

    private void handleAbm() {
        LOG.info("Reveived complete ABM");
        abk = new Abk();
        abk.setChannel(abm.getChannel());
        abk.setMsgId(abm.getMsgId());
        abk.setSequence(abm.getSequence());
        abk.setDestination(abm.getDestination());

        // Get AisMessage from Abm
        try {
            sendMessage(abm.getAisMessage(conf.getOwnMmsi(), 0, 0), abm.getSequence());
            abk.setResult(Abk.Result.ADDRESSED_SUCCESS);
        } catch (Exception e) {
            LOG.info("Error decoding ABM: " + e.getMessage());
            // Something must be wrong with Abm
            abk.setResult(Abk.Result.COULD_NOT_BROADCAST);
        }

        sendAbk();
    }

    private void sendAbk() {
        String encoded = abk.getEncoded() + "\r\n";
        LOG.info("Sending ABK: " + encoded);
        send(encoded);
    }

    public static TargetTableMessage getTargets(String host, int port, String username, String password) throws RestException {
        RestClient restClient = new RestClient(host, port);
        return restClient.getTargetTable(username, password);
    }

    public TransponderStatus getStatus() {
        return status;
    }

}
