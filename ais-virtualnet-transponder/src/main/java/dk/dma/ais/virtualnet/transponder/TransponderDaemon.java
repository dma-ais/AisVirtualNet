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

    private static final Logger LOG = LoggerFactory.getLogger(TransponderDaemon.class);

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
    protected void shutdown() {
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
