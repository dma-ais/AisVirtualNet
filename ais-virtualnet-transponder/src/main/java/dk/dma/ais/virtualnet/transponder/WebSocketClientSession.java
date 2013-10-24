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

import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.websocket.api.Session;

import dk.dma.ais.virtualnet.common.message.WsMessage;
import dk.dma.ais.virtualnet.common.websocket.WebSocketSession;

public class WebSocketClientSession extends WebSocketSession {
    
    private final CountDownLatch closed = new CountDownLatch(1);
    
    private final ServerConnection connection;
    private final String authToken;
    
    public WebSocketClientSession(ServerConnection connection, String authToken) {
        this.connection = connection;
        this.authToken = authToken;
    }
    
    @Override
    public void onWebSocketConnect(Session session) {        
        super.onWebSocketConnect(session);
        // Send credentials
        WsMessage msg = new WsMessage();
        msg.setAuthToken(authToken);
        sendMessage(msg);
    }
    
    @Override
    public void onWebSocketClose(int statusCode, String reason) {        
        super.onWebSocketClose(statusCode, reason);
        closed.countDown();
    }

    @Override
    protected void handleMessage(WsMessage wsMessage) {
        connection.receive(wsMessage.getPacket());
    }
    
    public CountDownLatch getClosed() {
        return closed;
    }
    
    

}
