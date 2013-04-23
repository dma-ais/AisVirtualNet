package dk.dma.ais.virtualnet.server;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketClientSession extends WebSocketSession {
    
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketClientSession.class);
    
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
    protected void handleMessage(Message message) {
        LOG.info("Received message: " + message.getAisPacket());
        
        // TODO
    }
    
    

}
