package dk.dma.ais.virtualnet.server;

import org.eclipse.jetty.websocket.api.WebSocketAdapter;

public class VirtualNetClientSession extends WebSocketAdapter {
    
    private final AisVirtualNetServer server;
    
    public VirtualNetClientSession(AisVirtualNetServer server) {
        this.server = server;
    }

    @Override
    public void onWebSocketText(String message) {
        System.out.println("Message from client: " + message);

    }

}
