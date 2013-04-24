package dk.dma.ais.virtualnet.transponder;

import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.websocket.api.Session;

import dk.dma.ais.virtualnet.common.message.Message;
import dk.dma.ais.virtualnet.common.websocket.WebSocketSession;

public class WebSocketClientSession extends WebSocketSession {
    
    private final CountDownLatch closed = new CountDownLatch(1);
    
    private final ServerConnection connection;
    
    public WebSocketClientSession(ServerConnection connection) {
        this.connection = connection;
    }
    
    @Override
    public void onWebSocketConnect(Session session) {        
        super.onWebSocketConnect(session);
        // Send credentials
        Message msg = new Message();
        msg.setUsername(connection.getConf().getUsername());
        msg.setPassword(connection.getConf().getPassword());
        sendMessage(msg);
    }
    
    @Override
    public void onWebSocketClose(int statusCode, String reason) {        
        super.onWebSocketClose(statusCode, reason);
        closed.countDown();
    }

    @Override
    protected void handleMessage(Message message) {
        connection.receive(message.getPacket());
    }
    
    public CountDownLatch getClosed() {
        return closed;
    }
    
    

}
