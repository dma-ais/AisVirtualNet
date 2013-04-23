package dk.dma.ais.virtualnet.server;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

public class WebSocketSession implements WebSocketListener {

    private final CountDownLatch connected = new CountDownLatch(1);

    private Session session;

    public WebSocketSession() {
        
    }
    
    @Override
    public void onWebSocketConnect(Session session) {
        System.out.println("onWebSocketConnect");
        this.session = session;
        getConnected().countDown();        
    }
    
    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        System.out.println("onWebSocketClose");
        session = null;
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        System.out.println("GOT BINARY");
        Session s = session;
        try {
            s.close(1, "Expected text only");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWebSocketError(Throwable arg0) {
        System.out.println("onWebSocketError");
    }

    @Override
    public void onWebSocketText(String message) {
        System.out.println("onWebSocketText message: " + message);
    }
    
    public final void close() {
        Session s = session;
        try {
            if (s != null) {
                s.close(2, "Close requested");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public final void sendText(String text) {
        Session s = session;
        RemoteEndpoint r = s == null ? null : s.getRemote();
        if (r != null) {
            try {
                r.sendString(text);
            } catch (IOException e) {
                e.printStackTrace();
                // ignore
            }
        } else {
            System.out.println("Could not send");
        }
    }

    public CountDownLatch getConnected() {
        return connected;
    }


}
