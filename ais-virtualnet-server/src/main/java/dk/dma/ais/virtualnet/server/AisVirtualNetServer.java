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

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.jcip.annotations.ThreadSafe;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.bus.AisBus;
import dk.dma.ais.bus.consumer.DistributerConsumer;
import dk.dma.ais.bus.provider.CollectorProvider;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.virtualnet.common.message.StatusMessage;
import dk.dma.ais.virtualnet.common.table.TargetTable;
import dk.dma.ais.virtualnet.server.rest.AisVirtualNetServerProvider;
import dk.dma.enav.util.function.Consumer;

/**
 * The virtual AIS network
 */
@ThreadSafe
public class AisVirtualNetServer extends Thread implements Consumer<AisPacket> {

    private static final Logger LOG = LoggerFactory.getLogger(AisVirtualNetServer.class);

    private final AisBus aisBus;
    private final Server server;
    private final CollectorProvider collector = new CollectorProvider();
    private final DistributerConsumer distributer = new DistributerConsumer();
    private final TargetTable targetTable = new TargetTable();
    private final Authenticator authenticator;
    private final MmsiBroker mmsiBroker;
    
    /**
     * Connected clients
     */
    private final Set<WebSocketServerSession> clients = Collections.newSetFromMap(new ConcurrentHashMap<WebSocketServerSession, Boolean>());

    public AisVirtualNetServer(ServerConfiguration conf, String usersFile) throws IOException {
        // Create web server
        server = new Server(conf.getPort());
        // Sets setReuseAddress
        ((ServerConnector) server.getConnectors()[0]).setReuseAddress(true);
        // Create and register websocket handler
        final AisVirtualNetServer virtualNetServer = this;
        WebSocketHandler wsHandler = new WebSocketHandler() {            
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.setCreator(new WebSocketCreator() {
                    public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
                        return new WebSocketServerSession(virtualNetServer);
                    }
                });
            }
        };        
        ContextHandler wsContext = new ContextHandler();
        wsContext.setContextPath("/ws");
        wsContext.setHandler(wsHandler);
        // Create and register web app context
        WebAppContext webappContext = new WebAppContext();
        webappContext.setServer(server);
        webappContext.setContextPath("/");
        webappContext.setWar("web");        
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { wsContext, webappContext });
        server.setHandler(contexts);
        
        // Create authenticator
        authenticator = new Authenticator(usersFile);
        
        // Create MMSI broker
        mmsiBroker = new MmsiBroker();
        
        // Create AisBus
        aisBus = conf.getAisbusConfiguration().getInstance();
        // Initialize distributer and register in aisbus
        distributer.getConsumers().add(this);
        distributer.init();
        aisBus.registerConsumer(distributer);
        // Initialize collector and register in aisbus        
        collector.init();
        aisBus.registerProvider(collector);        
    }
    
    public StatusMessage getStatus() {
        StatusMessage message = new StatusMessage();
        message.setMessageRate(distributer.getStatus().getInRate());
        message.setConnectedClients(clients.size());
        return message;
    }
    
    /**
     * Accept packet from AisBus
     */
    @Override
    public void accept(AisPacket packet) {
        LOG.debug("Accepted message from DistributerConsumer");
        // Maintain target table
        targetTable.update(packet);
        // Distribute packet to clients
        for (WebSocketServerSession client : clients) {
            LOG.debug("\tEnqueing at client");
            client.enqueuePacket(packet);
            LOG.debug("\t\tDone enqueing at client");
        }
    }
    
    /**
     * Distribute packet to AisBus
     * @param packet
     */
    public void distribute(AisPacket packet) {
        collector.accept(packet);
    }
    
    /**
     * Add a new client
     * @param session
     */
    public void addClient(WebSocketServerSession session) {
        LOG.info("Adding client");
        clients.add(session);        
        LOG.info("Client count: " + clients.size());
    }
    
    /**
     * Remove client
     * @param session
     */
    public void removeClient(WebSocketServerSession session) {
        LOG.info("Removing client");
        clients.remove(session);
        LOG.info("Client count: " + clients.size());
    }


    @Override
    public void start() {
        // Register server in provider
        AisVirtualNetServerProvider.setServer(this);
        
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
        LOG.info("Stopping web server");
        try {
            server.stop();
        } catch (Exception e) {
            LOG.error("Failed to stop web server", e);
        }
        
        if (aisBus != null) {
            LOG.info("Cancelling AisBus");
            aisBus.cancel();
        }
        
        LOG.info("Closing open web sockets");
        for (WebSocketServerSession client : clients) {
            client.close();
        }
        
        LOG.info("Waiting for server to stop");
        this.interrupt();
        try {
            this.join(10000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
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
            targetTable.cleanup();
        }
    }
    
    /**
     * Get current target table
     * @return
     */
    public TargetTable getTargetTable() {
        return targetTable;
    }
    
    /**
     * Get authenticator
     * @return
     */
    public Authenticator getAuthenticator() {
        return authenticator;
    }
    
    /**
     * Get MMSI broker
     * @return
     */
    public MmsiBroker getMmsiBroker() {
        return mmsiBroker;
    }

    /**
     * Check token 
     * @param authToken
     * @return
     */
    public boolean checkToken(String authToken) {
        return authenticator.validate(authToken);
    }


}
