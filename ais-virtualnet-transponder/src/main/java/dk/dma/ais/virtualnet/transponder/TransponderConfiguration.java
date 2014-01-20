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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TransponderConfiguration {

    private static final String READ_ONLY = "dma.settings.readonly";
    
    private int ownMmsi;
    private int ownPosInterval = 5; // five seconds
    private int receiveRadius = 75000; // 75 km (approx 40 nm)
    private int port = 8001;
    private String serverHost;
    private int serverPort;
    private String username;
    private String password;

    public TransponderConfiguration() {

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getOwnMmsi() {
        return ownMmsi;
    }

    public void setOwnMmsi(int ownMmsi) {
        this.ownMmsi = ownMmsi;
    }

    public int getOwnPosInterval() {
        return ownPosInterval;
    }

    public void setOwnPosInterval(int ownPosInterval) {
        this.ownPosInterval = ownPosInterval;
    }

    public int getReceiveRadius() {
        return receiveRadius;
    }

    public void setReceiveRadius(int receiveRadius) {
        this.receiveRadius = receiveRadius;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
    
    public String createServerUrl() {
        return "ws://" + serverHost + ":" + serverPort + "/ws/";
    }

    /**
     * Returns if the updated settings are persisted to disk
     * @return if the updated settings are persisted to disk
     */
    public static boolean isReadOnly() {
        return "true".equals(System.getProperty(READ_ONLY));
    }
    
    
    public static void save(String filename, TransponderConfiguration conf) throws JAXBException, FileNotFoundException {
        if (!isReadOnly()) {
            JAXBContext context = JAXBContext.newInstance(TransponderConfiguration.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            m.marshal(conf, new FileOutputStream(new File(filename)));
        }
    }

    public static TransponderConfiguration load(String filename) throws JAXBException, FileNotFoundException {
        JAXBContext context = JAXBContext.newInstance(TransponderConfiguration.class);
        Unmarshaller um = context.createUnmarshaller();
        TransponderConfiguration conf = (TransponderConfiguration) um.unmarshal(new FileInputStream(new File(filename)));
        if (isReadOnly()) {
            conf.setOwnMmsi(0);
        }
        return conf;
    }

}
