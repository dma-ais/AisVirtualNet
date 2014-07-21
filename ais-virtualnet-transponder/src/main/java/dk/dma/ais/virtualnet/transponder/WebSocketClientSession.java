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
package dk.dma.ais.virtualnet.transponder;

import java.util.concurrent.CountDownLatch;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import dk.dma.ais.virtualnet.common.message.WsMessage;
import dk.dma.ais.virtualnet.common.websocket.WebSocketSession;

@ClientEndpoint
public class WebSocketClientSession extends WebSocketSession {

    private final CountDownLatch closed = new CountDownLatch(1);

    private final ServerConnection connection;
    private final String authToken;

    public WebSocketClientSession(ServerConnection connection, String authToken) {
        this.connection = connection;
        this.authToken = authToken;
    }

    @OnOpen
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        // Send credentials
        WsMessage msg = new WsMessage();
        msg.setAuthToken(authToken);
        sendMessage(msg);
    }

    @OnClose
    public void onWebSocketClose(CloseReason  reason) {
        super.onWebSocketClose(reason);
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
