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
package dk.dma.ais.virtualnet.transponder;

import java.io.FileNotFoundException;
import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.google.inject.Injector;

import dk.dma.commons.app.AbstractDaemon;

/**
 * Command line version of transponder
 */
public class TransponderDaemon extends AbstractDaemon {

    static final Logger LOG = LoggerFactory.getLogger(TransponderDaemon.class);

    @Parameter(names = "-conf", description = "Transponder configuration file")
    String confFile = "transponder.xml";

    Transponder transponder;

    @Override
    protected void runDaemon(Injector injector) throws Exception {
        // Set default exception handler
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LOG.error("Uncaught exception in thread " + t.getClass().getCanonicalName() + ": " + e.getMessage(), e);
                shutdown();
                System.exit(-1);
            }
        });

        // Load configuration
        TransponderConfiguration conf;
        try {
            conf = TransponderConfiguration.load(confFile);
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage());
            return;
        }

        // Start transponder
        transponder = new Transponder(conf);
        transponder.start();
        transponder.join();
    }

    @Override
    public void shutdown() {
        LOG.info("Shutting down");
        if (transponder != null) {
            transponder.shutdown();
        }
        super.shutdown();
    }

    public static void main(String[] args) throws Exception {
        new TransponderDaemon().execute(args);
    }

}
