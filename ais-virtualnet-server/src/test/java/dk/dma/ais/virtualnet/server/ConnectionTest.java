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

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.dma.ais.configuration.bus.AisBusConfiguration;

public class ConnectionTest {

    private static final int TESTPORT = 14050;

    private AisVirtualNetServer server;

    @Before
    public void setup() throws InterruptedException {
        // Make configuration
        ServerConfiguration conf = new ServerConfiguration();
        AisBusConfiguration aisBusConf = new AisBusConfiguration();
        conf.setAisbusConfiguration(aisBusConf);
        conf.setPort(TESTPORT);

        // Make and start server instance
        server = new AisVirtualNetServer(conf);
        server.start();
    }

    @After
    public void stop() {
        server.shutdown();
    }

    @Test
    public void clientTest() throws Exception {
        WebSocketClientSession clientSession = new WebSocketClientSession("ole","ole");
        
        // Make client and connect
        WebSocketClient client = new WebSocketClient();
        System.out.println("Starting client");
        client.start();
        System.out.println("Connecting");
        client.connect(clientSession, new URI("ws://localhost:" + TESTPORT)).get();
        System.out.println("Connecting, waiting for connection");
        clientSession.getConnected().await(10, TimeUnit.SECONDS);
        System.out.println("Connected");
        
        
        
        Thread.sleep(5000);
    }

}
