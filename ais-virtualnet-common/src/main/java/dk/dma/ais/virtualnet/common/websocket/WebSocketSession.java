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

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.virtualnet.common.message.WsMessage;

@ThreadSafe
public abstract class WebSocketSession implements WebSocketListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketSession.class);
    
    private final Gson gson = new Gson();

    private final CountDownLatch connected = new CountDownLatch(1);

    @GuardedBy("this")
    private Session session;

    public WebSocketSession() {
        
    }
    
    /**
     * Method to handle incoming messages
     */
    protected abstract void handleMessage(WsMessage wsMessage);
    
    @Override
    public synchronized void onWebSocketConnect(Session session) {
        LOG.info("Client connected: " + session.getRemoteAddress());
        this.session = session;
        getConnected().countDown();
    }
    
    @Override
    public synchronized void onWebSocketClose(int statusCode, String reason) {
        LOG.info("Client connection closed: " + session.getRemoteAddress());
        session = null;
    }

    @Override
    public synchronized void onWebSocketBinary(byte[] payload, int offset, int len) {
        LOG.error("Received binary data");
        try {
            session.close(1, "Expected text only");
        } catch (IOException e) {
            LOG.error("Failed to close web sokcet", e);
        }
    }

    @Override
    public synchronized void onWebSocketError(Throwable t) {
        LOG.error("Websocket error: " + t.getMessage());
    }

    @Override
    public synchronized void onWebSocketText(String message) {
        // Try to deserialize into message
        WsMessage msg = gson.fromJson(message, WsMessage.class);
        // TODO handle exception
        handleMessage(msg);        
    }
    
    public final synchronized void close() {
        LOG.info("Closing web socket");
        try {
            if (session != null) {
                session.close();
            }
        } catch (IOException e) {
            LOG.error("Failed to close web socket: " + e.getMessage());
        }
    }
    
    public synchronized void sendPacket(AisPacket packet) {
        sendMessage(new WsMessage(packet));
    }
    
    protected final synchronized void sendMessage(WsMessage wsMessage) {
        sendText(gson.toJson(wsMessage));
    }
    
    private synchronized void sendText(String text) {
        RemoteEndpoint r = null;
        if (session != null && session.isOpen()) {
            // Guard against session closed before getRemote call
            try {
                r = session.getRemote();
            } catch (WebSocketException e) {
                LOG.error("Could not get remote for session", e);
            }
        }

        if (r != null) {
            try {
                r.sendString(text);
            } catch (IOException e) {
                // ignore
            }
        } else {
            LOG.error("Could not send to web socket");
        }
    }

    public CountDownLatch getConnected() {
        return connected;
    }


}
