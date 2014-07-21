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

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.google.inject.Injector;

import dk.dma.ais.virtualnet.transponder.gui.TransponderFrame;
import dk.dma.commons.app.AbstractCommandLineTool;

public class TransponderGUI extends AbstractCommandLineTool {

    private static final Logger LOG = LoggerFactory.getLogger(TransponderGUI.class);

    @Parameter(names = "-conf", description = "Transponder configuration file")
    String confFile = "transponder.xml";

    @Override
    protected void run(Injector injector) throws Exception {
        // Create and show GUI
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOG.error("Failed to set look and feed: " + e.getMessage());
        }
        // Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        TransponderFrame frame = new TransponderFrame(confFile);
        frame.setVisible(true);

        // frame.startTransponder();

    }

    public static void main(String[] args) throws Exception {
        new TransponderGUI().execute(args);
    }

}
