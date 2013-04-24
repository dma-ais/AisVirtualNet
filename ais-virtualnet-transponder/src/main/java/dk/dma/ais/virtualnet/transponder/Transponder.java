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
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.sentence.Abk;
import dk.dma.ais.sentence.Abm;
import dk.dma.ais.sentence.Bbm;
import dk.dma.ais.sentence.Sentence;
import dk.dma.ais.sentence.SentenceException;
import dk.dma.ais.sentence.Vdm;

/**
 * Virtual transponder
 */
@ThreadSafe
public class Transponder extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(Transponder.class);

    private final TransponderConfiguration conf;
    private final ServerConnection serverConnection;
    private final ServerSocket serverSocket;

    private volatile Socket socket;
    private volatile PrintWriter out;
    private Abm abm = new Abm();
    private Bbm bbm = new Bbm();
    private Abk abk = new Abk();

    public Transponder(TransponderConfiguration conf) throws IOException {
        this.conf = conf;
        serverConnection = new ServerConnection(this, conf);
        serverSocket = new ServerSocket(conf.getPort());
    }

    /**
     * Data received from server
     * 
     * @param packet
     */
    public void send(String strPacket) {
        // Send to client
        if (out == null) {
            return;
        }
        // Make packet and get ais message
        AisPacket packet = AisPacket.from(strPacket, System.currentTimeMillis());
        AisMessage message;
        try {
            message = packet.getAisMessage();
        } catch (AisMessageException | SixbitException e) {
            LOG.info("Failed to parse message: " + e.getMessage());
            return;
        }
        // Maybe own
        boolean own = (message.getUserId() == conf.getOwnMmsi());
        // Convert to VDO or VDM
        StringBuilder buf = new StringBuilder();
        for (String sentence : message.getVdm().getOrgLines()) {
            try {
                buf.append(Sentence.convert(sentence, "AI", (own) ? "VDO" : "VDM") + "\r\n");
            } catch (SentenceException e) {
                LOG.error("Failed to convert sentence " + sentence);
                return;
            }
        }
        // Maybe the transponder needs to send a binary acknowledge back to the network
        if (message.getMsgId() == 6) {
            AisMessage6 msg6 = (AisMessage6) message;
            if (msg6.getDestination() == conf.getOwnMmsi()) {
                sendBinAck(msg6);
            }
        }
        // Save own position
        if (own && message instanceof IVesselPositionMessage) {
            // TODO set own message in resender
        }

        out.print(buf.toString());
    }

    private void sendBinAck(AisMessage6 msg6) {
        AisMessage7 msg7 = new AisMessage7();
        msg7.setUserId(conf.getOwnMmsi());
        msg7.setDest1(msg6.getUserId());
        msg7.setSeq1(msg6.getSeqNum());
        LOG.info("Sending binary acknowledge: " + msg7);
        String[] sentences;
        try {
            sentences = Vdm.createSentences(msg7, msg6.getSeqNum());
        } catch (SixbitException e) {
            LOG.error("Failed to make binary acknowledge", e);
            return;
        }
        AisPacket packet = AisPacket.from(StringUtils.join(sentences, "\r\n"), System.currentTimeMillis());
        LOG.info("Sending encoded binary acknowledge: " + packet.getStringMessage());
        serverConnection.send(packet);
    }

    @Override
    public void start() {
        //serverConnection.start();

        // Start own message re sender

        super.start();
    }

    public void shutdown() {
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

    public void reconfigure(TransponderConfiguration conf) {
        // Maybe todo
    }

    @Override
    public void run() {
        socket = null;
        out = null;

        // Wait for connections
        LOG.info("Transponder listening on port " + conf.getPort());
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
            readFromAI();
        } catch (IOException e) {
            LOG.info("Lost connection to client");
        }

        try {
            socket.close();
        } catch (IOException e1) {
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

    private void handleBbm() {
        LOG.info("Reveived complete BBM");
        abk = new Abk();
        abk.setChannel(bbm.getChannel());
        abk.setMsgId(bbm.getMsgId());
        abk.setSequence(bbm.getSequence());

        // Get AisMessage from Bbm
        try {
            Vdm vdm = bbm.makeVdm(conf.getOwnMmsi(), 0);
            AisPacket packet = AisPacket.from(vdm.getEncoded(), System.currentTimeMillis());
            LOG.info("Sending VDM to network: " + packet.getStringMessage());
            serverConnection.send(packet);
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
            Vdm vdm = abm.makeVdm(conf.getOwnMmsi(), 0, 0);
            AisPacket packet = AisPacket.from(vdm.getEncoded(), System.currentTimeMillis());
            LOG.info("Sending VDM to network: " + packet.getStringMessage());
            serverConnection.send(packet);
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
        if (out != null) {
            out.print(encoded);
        }
    }

}
