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
package dk.dma.ais.virtualnet.transponder.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;

import javax.swing.JFrame;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.virtualnet.transponder.TransponderConfiguration;

/**
 * Transponder frame
 */
public class TransponderFrame extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(TransponderFrame.class);

    private final String conffile;
    
    private TransponderConfiguration conf;
    
    public TransponderFrame() {
        this("transponder.xml");
    }

    public TransponderFrame(String conffile) {
        super();
        this.conffile = conffile;
        setSize(new Dimension(500, 300));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("AisVirtualNet transponder");
        setLocationRelativeTo(null);        

        loadConf();
        saveConf();

        // Call method that fills components with current values

    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
    
    private void loadConf() {
        try {
            conf = TransponderConfiguration.load(conffile);
        } catch (FileNotFoundException e) {

        } catch (JAXBException e) {
            LOG.error("Failed to load configuration", e);
        }
        if (conf == null) {
            conf = new TransponderConfiguration();
        }
    }
    
    private void saveConf() {
        try {
            TransponderConfiguration.save(conffile, conf);
        } catch (Exception e) {
            LOG.error("Failed to save configuration", e);
        }
    }

}
