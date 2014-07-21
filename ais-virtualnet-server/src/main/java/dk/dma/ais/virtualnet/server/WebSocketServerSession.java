/* Copyright (c) 2011 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.ais.virtualnet.server;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import net.jcip.annotations.ThreadSafe;

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
@ServerEndpoint(value = "/")
public class WebSocketServerSession extends WebSocketSession implements IQueueEntryHandler<AisPacket> {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketServerSession.class);

    private final OverflowLogger overflowLogger = new OverflowLogger(LOG);

    private static final long OVERFLOW_TIMEOUT = 10 * 1000; // 10 sec

    private final AisVirtualNetServer server;

    private volatile boolean authenticated;

    private volatile String authToken;

    private volatile MessageQueueReader<AisPacket> queueReader;

    private long overflowStart;

    public WebSocketServerSession(AisVirtualNetServer server) {
        this.server = server;
    }

    @OnOpen
    public void onWebSocketConnect(Session session) {
        // Setup message queue and message queue reader
        queueReader = new MessageQueueReader<>(this, new BlockingMessageQueue<AisPacket>(), 10);
        queueReader.start();
        super.onWebSocketConnect(session);
        server.addClient(this);
    }

    @OnClose
    public void onWebSocketClose(CloseReason reason) {
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
        super.onWebSocketClose(reason);
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
