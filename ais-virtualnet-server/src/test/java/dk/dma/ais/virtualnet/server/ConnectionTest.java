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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.configuration.bus.AisBusConfiguration;
import dk.dma.ais.virtualnet.transponder.Transponder;
import dk.dma.ais.virtualnet.transponder.TransponderConfiguration;

public class ConnectionTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionTest.class);

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
        LOG.info("AisVirtualNet server started");
        
        Thread.sleep(3000);
    }

    @Test
    public void transponderTest() throws Exception {
        TransponderConfiguration conf = new TransponderConfiguration();
        conf.setOwnMmsi(219230000);
        conf.setServerUrl("ws://localhost:" + TESTPORT);
        conf.setUsername("ole");
        Transponder transponder = new Transponder(conf);
        LOG.info("Starting transponder");
        transponder.start();
        LOG.info("Transponder started");
        
        transponder.join(3000);
        LOG.info("Shutting down transponder");
        transponder.shutdown();
        LOG.info("Transponder shutdown");
        Thread.sleep(3000);
        
        LOG.info("Shutting down AisVirtualNet server");
        server.shutdown();
        Thread.sleep(3000);

    }

}
