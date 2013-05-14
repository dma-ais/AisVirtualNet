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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.virtualnet.common.message.TargetTableMessage;
import dk.dma.ais.virtualnet.common.table.TargetTableEntry;
import dk.dma.ais.virtualnet.transponder.ITransponderStatusListener;
import dk.dma.ais.virtualnet.transponder.RestException;
import dk.dma.ais.virtualnet.transponder.Transponder;
import dk.dma.ais.virtualnet.transponder.TransponderConfiguration;
import dk.dma.ais.virtualnet.transponder.TransponderStatus;

/**
 * Transponder frame
 */
public class TransponderFrame extends JFrame implements ActionListener, ITransponderStatusListener, WindowListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(TransponderFrame.class);

    private final String conffile;
    private final boolean embedded;

    private Transponder transponder;
    private TransponderConfiguration conf;

    // Control buttons
    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");
    private final JButton closeExitButton = new JButton("Exit/Close");
    private final JButton selectVesselButton = new JButton("...");

    // Input fields
    private final JTextField mmsi = new JTextField(9);
    private final JTextField resendInterval = new JTextField(2);
    private final JTextField serverHost = new JTextField(20);
    private final JTextField serverPort = new JTextField(5);
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JTextField port = new JTextField();
    private final JTextField receiveRadius = new JTextField();

    // Status labels
    private final JLabel clientStatusIconLabel = new JLabel();
    private final JLabel serverStatusIconLabel = new JLabel();
    private final JLabel serverErrorLabel = new JLabel();
    private final JLabel ownShipPosIconLabel = new JLabel();
    private final JLabel shipNameLbl = new JLabel();

    // Icons
    private static final ImageIcon UNKNOWN_ICON = new ImageIcon(TransponderFrame.class.getResource("/images/UNKNOWN.png"));
    private static final ImageIcon ERROR_ICON = new ImageIcon(TransponderFrame.class.getResource("/images/ERROR.png"));
    private static final ImageIcon OK_ICON = new ImageIcon(TransponderFrame.class.getResource("/images/OK.png"));

    private final List<JComponent> lockedWhileRunningComponents = Arrays.asList(new JComponent[] { mmsi, resendInterval,
            serverHost, serverPort, username, password, port, receiveRadius });

    private final List<JLabel> iconLabels = Arrays.asList(new JLabel[] { clientStatusIconLabel, serverStatusIconLabel,
            ownShipPosIconLabel });

    public TransponderFrame() {
        this("transponder.xml");
    }
        
    public TransponderFrame(String conffile) {
        this(conffile, false);
    }
    
    public TransponderFrame(boolean embeeded) {
        this("transponder.xml", embeeded);
    }

    public TransponderFrame(String conffile, boolean embedded) {
        super();
        this.conffile = conffile;
        this.embedded = embedded;
        setSize(new Dimension(412, 500));

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(this);

        setTitle("AisVirtualNet transponder");
        setLocationRelativeTo(null);

        startButton.addActionListener(this);
        stopButton.addActionListener(this);
        selectVesselButton.addActionListener(this);
        closeExitButton.addActionListener(this);
        closeExitButton.setText(embedded ? "Close" : "Exit");

        layoutGui();

        loadConf();
        saveConf();

        // Update GUI components with configuration values
        updateValues();

        // Update controls
        updateEnabled();
    }

    /**
     * Update status components with transponder state
     * 
     * @param status
     */
    private void updateStatus(TransponderStatus status) {
        if (transponder == null) {
            return;
        }
        // Determine client status
        clientStatusIconLabel.setIcon(status.isClientConnected() ? OK_ICON : ERROR_ICON);
        // Determine server status
        serverStatusIconLabel.setIcon(status.isServerConnected() ? OK_ICON : ERROR_ICON);
        // Set possible server error
        serverErrorLabel.setText(status.getServerError() != null ? status.getServerError() : "");
        // Own pos indicating
        ownShipPosIconLabel.setIcon(status.getOwnPos() != null ? OK_ICON : ERROR_ICON);
        // Own ship name
        shipNameLbl.setText(status.getShipName());
    }

    /**
     * Update configuration from GUI components
     */
    private void updateConf() {
        conf.setOwnMmsi(getInt(mmsi));
        conf.setOwnPosInterval(getInt(resendInterval));
        conf.setServerHost(getString(serverHost));
        conf.setServerPort(getInt(serverPort));
        conf.setUsername(getString(username));
        conf.setPassword(getString(password));
        conf.setPort(getInt(port));
        conf.setReceiveRadius(getInt(receiveRadius) * 1852);
    }

    private String getString(JTextField field) {
        return field.getText();
    }

    private int getInt(JTextField field) {
        try {
            return Integer.parseInt(field.getText());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer: " + field.getText());
        }
    }

    /**
     * Update values in components from conf
     */
    private void updateValues() {
        setVal(mmsi, conf.getOwnMmsi());
        setVal(resendInterval, conf.getOwnPosInterval());
        setVal(serverHost, conf.getServerHost());
        setVal(serverPort, conf.getServerPort());
        setVal(username, conf.getUsername());
        setVal(password, conf.getPassword());
        setVal(port, conf.getPort());
        setVal(receiveRadius, conf.getReceiveRadius() / 1852);
    }

    private void setVal(JTextField field, String val) {
        field.setText(val);
    }

    private void setVal(JTextField field, int val) {
        setVal(field, Integer.toString(val));
    }

    private void updateEnabled() {
        startButton.setEnabled(transponder == null);
        stopButton.setEnabled(transponder != null);
        selectVesselButton.setEnabled(transponder == null);
        for (JComponent comp : lockedWhileRunningComponents) {
            comp.setEnabled(transponder == null);
        }
        if (transponder == null) {
            for (JLabel iconLabel : iconLabels) {
                iconLabel.setIcon(UNKNOWN_ICON);
            }
            serverErrorLabel.setText("");
            shipNameLbl.setText("");
        }
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
        } else if (e.getSource() == stopButton) {
            stopTransponder();
        } else if (e.getSource() == selectVesselButton) {
            selectTarget();
        } else if (e.getSource() == closeExitButton) {
            shutdown();
        }

    }

    private void selectTarget() {
        try {
            updateConf();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Transponder error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Cursor currentCursor = getCursor();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        TargetTableMessage message;
        try {
            message = Transponder.getTargets(conf.getServerHost(), conf.getServerPort(), conf.getUsername(), conf.getPassword());
        } catch (RestException e) {
            LOG.error("Failed to get list of targets");
            JOptionPane.showMessageDialog(this, "Failed to get list of targets: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            setCursor(currentCursor);
            return;
        }
        setCursor(currentCursor);

        // Sort by name
        List<TargetTableEntry> targets = message.getTargets();
        Collections.sort(targets, new TargetTableEntry.NameSort());

        // Show select dialog
        SelectTargetDialog selectTargetDialog = new SelectTargetDialog(this, targets);
        selectTargetDialog.setVisible(true);
        if (selectTargetDialog.getSelectedTarget() != null) {
            conf.setOwnMmsi(selectTargetDialog.getSelectedTarget().intValue());
            updateValues();
        }
    }

    public void startTransponder() {
        if (transponder != null) {
            LOG.error("Trying to start transponder already started");
            return;
        }
        try {
            updateConf();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Transponder error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            transponder = new Transponder(conf);
        } catch (IOException e) {
            transponder = null;
            JOptionPane.showMessageDialog(this, e.getMessage(), "Transponder error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        saveConf();
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
        getContentPane().setLayout(null);

        JPanel mmsiPanel = new JPanel();
        mmsiPanel.setBounds(6, 6, 400, 66);
        mmsiPanel.setLayout(null);
        mmsiPanel.setBorder(new TitledBorder(null, "MMSI", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        mmsi.setBounds(16, 22, 105, 28);
        mmsiPanel.add(mmsi);
        selectVesselButton.setBounds(121, 23, 47, 29);
        mmsiPanel.add(selectVesselButton);
        resendInterval.setBounds(344, 22, 38, 28);
        mmsiPanel.add(resendInterval);
        JLabel omLbl = new JLabel("Own message interval (s)");
        omLbl.setBounds(180, 28, 157, 16);
        mmsiPanel.add(omLbl);
        getContentPane().add(mmsiPanel);

        JPanel serverPanel = new JPanel();
        serverPanel.setLayout(null);
        serverPanel.setBounds(6, 84, 400, 88);
        serverPanel.setBorder(new TitledBorder(null, "Server", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        JLabel srvHostLbl = new JLabel("Host");
        srvHostLbl.setBounds(16, 23, 37, 16);
        serverPanel.add(srvHostLbl);
        JLabel srvPortLbl = new JLabel("Port");
        srvPortLbl.setBounds(291, 20, 37, 16);
        serverPanel.add(srvPortLbl);
        serverPort.setBounds(324, 17, 58, 22);
        serverPanel.add(serverPort);
        JLabel usernameLbl = new JLabel("Username");
        serverPanel.add(usernameLbl);
        usernameLbl.setBounds(16, 53, 69, 16);
        username.setBounds(89, 50, 115, 22);
        serverPanel.add(username);
        JLabel passwordLbl = new JLabel("Password");
        passwordLbl.setBounds(216, 50, 69, 16);
        serverPanel.add(passwordLbl);
        password.setBounds(280, 47, 102, 22);
        serverPanel.add(password);
        getContentPane().add(serverPanel);
        serverHost.setBounds(89, 20, 190, 22);
        serverPanel.add(serverHost);

        JPanel transponderPanel = new JPanel();
        transponderPanel.setBorder(new TitledBorder(null, "Transponder", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        transponderPanel.setLayout(null);
        transponderPanel.setBounds(6, 184, 400, 60);
        JLabel portLbl = new JLabel("Port");
        portLbl.setBounds(16, 23, 34, 16);
        transponderPanel.add(portLbl);
        JLabel radiusLbl = new JLabel("Receive radius (nm) (0 = all)");
        radiusLbl.setBounds(119, 23, 193, 16);
        transponderPanel.add(radiusLbl);
        port.setBounds(49, 20, 58, 22);
        transponderPanel.add(port);
        receiveRadius.setBounds(324, 20, 58, 22);
        transponderPanel.add(receiveRadius);
        getContentPane().add(transponderPanel);

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(null);
        statusPanel.setBorder(new TitledBorder(null, "Status", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        statusPanel.setBounds(6, 256, 400, 123);
        JLabel clientLbl = new JLabel("Client connection");
        clientLbl.setBounds(16, 44, 117, 16);
        statusPanel.add(clientLbl);
        statusPanel.add(clientStatusIconLabel);
        clientStatusIconLabel.setBounds(145, 44, 16, 16);
        JLabel serverLbl = new JLabel("Server connection");
        serverLbl.setBounds(16, 23, 117, 16);
        statusPanel.add(serverLbl);
        serverStatusIconLabel.setBounds(145, 23, 16, 16);
        statusPanel.add(serverStatusIconLabel);
        serverErrorLabel.setForeground(Color.RED);
        serverErrorLabel.setText("");
        serverErrorLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
        serverErrorLabel.setBounds(182, 25, 200, 15);
        statusPanel.add(serverErrorLabel);
        JLabel ownPosLbl = new JLabel("Own position");
        ownPosLbl.setBounds(16, 66, 117, 16);
        statusPanel.add(ownPosLbl);
        ownShipPosIconLabel.setBounds(145, 66, 16, 16);
        statusPanel.add(ownShipPosIconLabel);
        JLabel nameLbl = new JLabel("Ship name");
        nameLbl.setBounds(16, 89, 117, 16);
        statusPanel.add(nameLbl);
        shipNameLbl.setBounds(145, 89, 237, 16);
        statusPanel.add(shipNameLbl);
        getContentPane().add(statusPanel);

        JPanel controlPanel = new JPanel();
        controlPanel.setBorder(new TitledBorder(null, "Controls", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        controlPanel.setBounds(6, 391, 400, 75);
        controlPanel.setLayout(null);
        startButton.setBounds(6, 25, 75, 29);
        controlPanel.add(startButton);
        stopButton.setBounds(87, 25, 75, 29);
        controlPanel.add(stopButton);
        getContentPane().add(controlPanel);
        closeExitButton.setBounds(319, 25, 75, 29);
        controlPanel.add(closeExitButton);
    }

    public void shutdown() {
        saveConf();
        if (embedded) {
            this.setVisible(false);
        } else {
            if (transponder != null) {
                stopTransponder();
            }
            this.dispose();
            System.exit(0);
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
        shutdown();
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

}
