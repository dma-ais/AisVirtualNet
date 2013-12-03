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
