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
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.virtualnet.transponder.ITransponderStatusListener;
import dk.dma.ais.virtualnet.transponder.Transponder;
import dk.dma.ais.virtualnet.transponder.TransponderConfiguration;
import dk.dma.ais.virtualnet.transponder.TransponderStatus;
import java.awt.FlowLayout;

/**
 * Transponder frame
 */
public class TransponderFrame extends JFrame implements ActionListener, ITransponderStatusListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(TransponderFrame.class);

    private final String conffile;

    private Transponder transponder;
    private TransponderConfiguration conf;
    private final JButton startButton;
    private final JButton stopButton;
    
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
        
        startButton = new JButton("Start");
        startButton.addActionListener(this);
        stopButton = new JButton("Stop");
        stopButton.addActionListener(this);
        
        layoutGui();

        loadConf();
        saveConf();
        
        // Update GUI components with configuration values
        updateValues();    

    }
    
    
    private void updateValues() {
        // TODO
    }
    
    private void updateEnabled() {
        startButton.setEnabled(transponder == null);
        stopButton.setEnabled(transponder != null);
        
        // TODO: Lock or unlock components
    }
    
    /**
     * Update status components with transponder state
     * @param status
     */
    private void updateStatus(TransponderStatus status) {
        System.out.println("updateStatus()");
        // TODO
    }

    @Override
    public void stateChanged(final TransponderStatus status) {
        SwingUtilities.invokeLater(new Runnable() {            
            @Override
            public void run() {
                updateStatus(status);
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            startTransponder();
        }
        else if (e.getSource() == stopButton) {
            stopTransponder();
        }
    }
    
    public void startTransponder() {
        if (transponder != null) {
            LOG.error("Trying to start transponder already started");
            return;
        }
        try {
            transponder = new Transponder(conf);
        } catch (IOException e) {
            transponder = null;
            JOptionPane.showMessageDialog(this, e.getMessage(), "Transponder error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        updateEnabled();
        // Make the GUI the listener of events
        transponder.getStatus().addListener(this);
        transponder.start();
    }
    
    private void stopTransponder() {
        if (transponder == null) {
            LOG.error("Trying to stop transponder already stopped");
            return;
        }
        transponder.shutdown();
        transponder = null;
        updateEnabled();
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
    
    private void layoutGui() {
        getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        // TODO do the layout
        getContentPane().add(startButton);
        getContentPane().add(stopButton);
    }

}
