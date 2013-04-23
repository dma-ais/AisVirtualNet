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

import net.jcip.annotations.ThreadSafe;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.bus.AisBus;
import dk.dma.ais.virtualnet.server.configuration.ServerConfiguration;

/**
 * The virtual AIS network
 */
@ThreadSafe
public class AisVirtualNetServer extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(AisVirtualNetServer.class);

    private final ServerConfiguration conf;
    private final AisBus aisBus;
    private Server server; 

    public AisVirtualNetServer(ServerConfiguration conf) {
        this.conf = conf;

        // Create AisBus
        aisBus = conf.getAisbusConfiguration().getInstance();

    }

    @Override
    public void start() {
        // TEST BELOW HERE
        server = new Server(conf.getPort());
        // Sets setReuseAddress
        ServerConnector connector = (ServerConnector) server.getConnectors()[0];
        connector.setReuseAddress(true);

        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.setCreator(new WebSocketCreator() {
                    public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
                        // Make a connection manager that handles active connections (or do it directly in this server)
                        // Make a derived WebSocketServerSession that has a handle to the server so connections can be uregistreed
                        // Or use static collections directly in WebSocketServerSession
                        return new WebSocketSession();
                    }
                });
            }
        };
        
        server.setHandler(wsHandler);
        
        try {
            server.start();
            LOG.info("Ready to accept incoming sockets");
        } catch (Exception e) {
            LOG.error("Failed to start server", e);
            try {
                server.stop();
            } catch (Exception e1) {                
            }
            return;
        }
        
        // Start aisbus
        aisBus.startConsumers();
        aisBus.startProviders();
        aisBus.start();
        
        
        super.start();
    }
    
    public void shutdown() {
        try {
            server.stop();
        } catch (Exception e) {
            LOG.error("Failed to stop web server", e);
        }
        aisBus.cancel();        
        this.interrupt();
        try {
            this.join(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                return;
            }

        }

    }

}
