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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.beust.jcommander.Parameter;

import dk.dma.ais.configuration.bus.AisBusConfiguration;

/**
 * Class to represent AisVirtualNetServer server configuration. To be marshalled and unmarshalled by JAXB.
 */
@XmlRootElement
public class ServerConfiguration {

    private AisBusConfiguration aisbusConfiguration;

    private int port = 8080;

    @Parameter(names = "-conf", description = "AisVirtualNetServer server configuration file")
    String confFile = "server.xml";

    public ServerConfiguration() {

    }

    @XmlElement(name = "aisbus")
    public AisBusConfiguration getAisbusConfiguration() {
        return aisbusConfiguration;
    }

    public void setAisbusConfiguration(AisBusConfiguration aisbusConfiguration) {
        this.aisbusConfiguration = aisbusConfiguration;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public static void save(String filename, ServerConfiguration conf) throws JAXBException, FileNotFoundException {
        JAXBContext context = JAXBContext.newInstance(ServerConfiguration.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        m.marshal(conf, new FileOutputStream(new File(filename)));
    }

    public static ServerConfiguration load(String filename) throws JAXBException, FileNotFoundException {
        JAXBContext context = JAXBContext.newInstance(ServerConfiguration.class);
        Unmarshaller um = context.createUnmarshaller();
        return (ServerConfiguration) um.unmarshal(new FileInputStream(new File(filename)));
    }

}
