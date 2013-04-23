package dk.dma.ais.virtualnet.transponder;

import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.virtualnet.common.message.Message;
import dk.dma.ais.virtualnet.common.websocket.WebSocketSession;

public class WebSocketClientSession extends WebSocketSession {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketClientSession.class);
    
    private final CountDownLatch closed = new CountDownLatch(1);
    
    private final String username;
    private final String password;
    
    public WebSocketClientSession(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    @Override
    public void onWebSocketConnect(Session session) {        
        super.onWebSocketConnect(session);
        // Send credentials
        Message msg = new Message();
        msg.setUsername(username);
        msg.setPassword(password);
        sendMessage(msg);
    }
    
    @Override
    public void onWebSocketClose(int statusCode, String reason) {        
        super.onWebSocketClose(statusCode, reason);
        closed.countDown();
    }

    @Override
    protected void handleMessage(Message message) {
        LOG.info("Received message: " + message.getPacket());
        
        // TODO
    }
    
    public CountDownLatch getClosed() {
        return closed;
    }
    
    

}
