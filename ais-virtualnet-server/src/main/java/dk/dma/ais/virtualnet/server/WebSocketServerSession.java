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

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.virtualnet.common.message.Message;
import dk.dma.ais.virtualnet.common.websocket.WebSocketSession;

// TODO threadsafe?
public class WebSocketServerSession extends WebSocketSession {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketServerSession.class);
    
    private final AisVirtualNetServer server;
    private volatile boolean authenticated; 
    
    public WebSocketServerSession(AisVirtualNetServer server) {
        this.server = server;
    }
    
    @Override
    public void onWebSocketConnect(Session session) {
        server.addClient(this);
        super.onWebSocketConnect(session);
    }
    
    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        server.removeClient(this);
        super.onWebSocketClose(statusCode, reason);
    }
    
    @Override
    public void sendPacket(AisPacket packet) {
        if (!authenticated) {            
            return;
        }
        super.sendPacket(packet);
    }
    
    @Override
    protected void handleMessage(Message message) {
        // Maybe message has credentials
        if (message.hasCredentials()) {
            authenticated = server.authenticate(message.getUsername(), message.getPassword());
            LOG.info("Authentication result: " + authenticated);
        }
        String strPacket = message.getPacket();
        if (strPacket == null) {
            return;
        }
        if (!authenticated) {
            LOG.error("Client sending messages without authentication");
            return;
        }
        LOG.info("Received message from client:\n" + strPacket);
        server.distribute(AisPacket.from(strPacket));
    }

}
