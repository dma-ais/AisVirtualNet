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
package dk.dma.ais.virtualnet.common.websocket;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.websocket.CloseReason;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import net.jcip.annotations.ThreadSafe;

import org.eclipse.jetty.websocket.api.WebSocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.virtualnet.common.message.WsMessage;

@ThreadSafe
public abstract class WebSocketSession {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketSession.class);

    private final Gson gson = new Gson();

    private final CountDownLatch connected = new CountDownLatch(1);

    private volatile Session session;

    /**
     * Method to handle incoming messages
     */
    protected abstract void handleMessage(WsMessage wsMessage);

    public void onWebSocketConnect(Session session) {
        LOG.info("Client connected: " + session.getUserProperties());
        this.session = session;
        getConnected().countDown();
    }

    public void onWebSocketClose(CloseReason reason) {
        LOG.info("Client connection closed: " + session.getUserProperties());
        session = null;
    }

    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        Session s = session;
        LOG.error("Received binary data");
        try {
            s.close(new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR, "Cannot accept binary"));
        } catch (Exception e) {
            LOG.error("Failed to close web sokcet", e);
        }
    }

    @OnError
    public void onWebSocketError(Throwable t) {
        LOG.error("Websocket error: " + t.getMessage());
    }

    @OnMessage
    public void onWebSocketText(String message) {
        // Try to deserialize into message
        WsMessage msg = gson.fromJson(message, WsMessage.class);
        // TODO handle exception
        handleMessage(msg);
    }

    public final void close() {
        Session s = session;
        LOG.info("Closing web socket");
        try {
            if (s != null) {
                s.close();
            }
        } catch (Exception e) {
            LOG.error("Failed to close web socket: " + e.getMessage());
        }
    }

    public void sendPacket(AisPacket packet) {
        sendMessage(new WsMessage(packet));
    }

    protected final void sendMessage(WsMessage wsMessage) {
        sendText(gson.toJson(wsMessage));
    }

    private void sendText(String text) {
        Session s = session;
        RemoteEndpoint.Basic r = null;
        try {
            r = s == null ? null : s.getBasicRemote();
        } catch (WebSocketException e) {
            // Ignore
        }
        if (r != null) {
            try {
                r.sendText(text);
            } catch (IOException e) {
                LOG.error("Failed to send text");
            }
        }
    }

    public CountDownLatch getConnected() {
        return connected;
    }

}
