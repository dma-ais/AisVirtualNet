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
package dk.dma.ais.virtualnet.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.google.inject.Injector;

import dk.dma.commons.app.AbstractDaemon;


/**
 * AisVirtualNetServer server daemon
 */
public class ServerDaemon extends AbstractDaemon {

    private static final Logger LOG = LoggerFactory.getLogger(ServerDaemon.class);

    @Parameter(names = "-conf", description = "AisVirtualNetServer server configuration file")
    String confFile = "server.xml";

    @Parameter(names = "-users", description = "AisVirtualNetServer server users file")
    String usersFile = "users.txt";

    AisVirtualNetServer server;

    @Override
    protected void runDaemon(Injector injector) throws Exception {
        LOG.info("Starting AisVirtualNetServer server with configuration: " + confFile);

        // Load configuration
        ServerConfiguration conf;
        try {
            conf = ServerConfiguration.load(confFile);
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage());
            return;
        }

        // Start server
        try {
            server = new AisVirtualNetServer(conf, usersFile);
        } catch (IOException e) {
            LOG.error(e.getMessage());
            return;
        }
        server.start();
    }

    @Override
    public void shutdown() {
        LOG.info("Shutting down");
        if (server != null) {
            server.shutdown();
        }
        super.shutdown();
    }

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LOG.error("Uncaught exception in thread " + t.getClass().getCanonicalName() + ": " + e.getMessage(), e);
                System.exit(1);
            }
        });
        new ServerDaemon().execute(args);
    }

}
