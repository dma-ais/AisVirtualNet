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
package dk.dma.ais.virtualnet.server;

import net.jcip.annotations.ThreadSafe;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.bus.OverflowLogger;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.queue.BlockingMessageQueue;
import dk.dma.ais.queue.IQueueEntryHandler;
import dk.dma.ais.queue.MessageQueueOverflowException;
import dk.dma.ais.queue.MessageQueueReader;
import dk.dma.ais.virtualnet.common.message.WsMessage;
import dk.dma.ais.virtualnet.common.websocket.WebSocketSession;

@ThreadSafe
public class WebSocketServerSession extends WebSocketSession implements IQueueEntryHandler<AisPacket> {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketServerSession.class);
    private final OverflowLogger overflowLogger = new OverflowLogger(LOG);

    private static final long OVERFLOW_TIMEOUT = 30 * 1000; // 30 sec

    private final AisVirtualNetServer server;
    private volatile boolean authenticated;
    private volatile String authToken;
    private volatile MessageQueueReader<AisPacket> queueReader;
    private long overflowStart;

    public WebSocketServerSession(AisVirtualNetServer server) {
        this.server = server;
    }

    @Override
    public void onWebSocketConnect(Session session) {
        // Setup message queue and message queue reader
        queueReader = new MessageQueueReader<AisPacket>(this, new BlockingMessageQueue<AisPacket>(), 10);
        queueReader.start();
        super.onWebSocketConnect(session);
        server.addClient(this);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        MessageQueueReader<AisPacket> qr = queueReader;
        String at = authToken;
        if (qr != null) {
            qr.cancel();
        }
        queueReader = null;
        server.removeClient(this);
        if (at != null) {
            server.getMmsiBroker().release(at);
        }
        super.onWebSocketClose(statusCode, reason);
    }

    public void enqueuePacket(AisPacket packet) {
        if (queueReader != null) {
            try {
                queueReader.getQueue().push(packet);
            } catch (MessageQueueOverflowException e) {
                overflowLogger.log("Write queue is full");
                if (overflowStart == 0) {
                    overflowStart = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - overflowStart > OVERFLOW_TIMEOUT) {
                    close();
                }
                return;
            }
            overflowStart = 0;
        }
    }

    @Override
    public void receive(AisPacket packet) {
        sendPacket(packet);
    }

    @Override
    public void sendPacket(AisPacket packet) {
        if (!authenticated) {
            return;
        }
        super.sendPacket(packet);
    }

    @Override
    protected void handleMessage(WsMessage wsMessage) {
        // Maybe message a token
        if (wsMessage.getAuthToken() != null) {
            authToken = wsMessage.getAuthToken();
            authenticated = server.checkToken(wsMessage.getAuthToken());
            LOG.info("Authentication result: " + authenticated);
            // Maybe activate MMSI reservation
            if (authenticated) {
                if (!server.getMmsiBroker().activate(wsMessage.getAuthToken())) {
                    LOG.error("Failed to activate MMSI reservation");
                    close();
                    return;
                }
            }
        }
        String strPacket = wsMessage.getPacket();
        if (strPacket == null) {
            return;
        }
        if (!authenticated) {
            LOG.error("Client sending messages without authentication");
            close();
            return;
        }
        LOG.info("Received message from client:\n" + strPacket);
        server.distribute(AisPacket.from(strPacket));
    }

}
