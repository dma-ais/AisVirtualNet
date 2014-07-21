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

    public static void save(String filename, TransponderConfiguration conf) throws JAXBException, FileNotFoundException {
        JAXBContext context = JAXBContext.newInstance(TransponderConfiguration.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        m.marshal(conf, new FileOutputStream(new File(filename)));
    }

    public static TransponderConfiguration load(String filename) throws JAXBException, FileNotFoundException {
        JAXBContext context = JAXBContext.newInstance(TransponderConfiguration.class);
        Unmarshaller um = context.createUnmarshaller();
        TransponderConfiguration conf = (TransponderConfiguration) um.unmarshal(new FileInputStream(new File(filename)));
        return conf;
    }

}
