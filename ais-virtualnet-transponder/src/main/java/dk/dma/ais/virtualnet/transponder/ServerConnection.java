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

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.packet.AisPacket;

/**
 * Class that maintains the connection to the server
 */
public class ServerConnection extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(ServerConnection.class);

    private final Transponder transponder;
    private final TransponderConfiguration conf;

    private volatile WebSocketClientSession session;
    private volatile boolean connected;

    public ServerConnection(Transponder transponder, TransponderConfiguration conf) {
        this.transponder = transponder;
        this.conf = conf;
    }

    /**
     * Send packet to server
     */
    public void send(AisPacket packet) {
        if (connected) {
            session.sendPacket(packet);
        }
    }

    /**
     * Receive message from the server
     * 
     * @param packet
     */
    public void receive(String packet) {
        transponder.send(packet);

    }

    public void shutdown() {
        this.interrupt();
        if (session != null) {
            session.close();
        }
        try {
            this.join(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public TransponderConfiguration getConf() {
        return conf;
    }

    @Override
    public void run() {
        while (true) {
            if (isInterrupted()) {
                return;
            }            
            connected = false;
            // Make session
            session = new WebSocketClientSession(this);
            // Make client and connect
            WebSocketClient client = new WebSocketClient();
            try {
                client.start();
                client.connect(session, new URI(conf.getServerUrl())).get();
                if (!session.getConnected().await(10, TimeUnit.SECONDS)) {
                    LOG.error("Timeout waiting for connection");
                    session.close();
                } else {
                    connected = true;
                }
            } catch (Exception e) {
                LOG.error("Failed to connect web socket: " + e.getMessage());
            }

            if (!connected) {
                // Something went wrong, wait a while
                try {
                    LOG.info("Waiting to reconnect");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    return;
                }
                continue;
            }

            // Wait for disconnect
            try {
                session.getClosed().await();
            } catch (InterruptedException e) {
                session.close();
                return;
            }
            
            session.close();
            
        }

    }

}
