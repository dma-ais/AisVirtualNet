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
