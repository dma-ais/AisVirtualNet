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

import javax.xml.bind.JAXBException;

import org.junit.Test;

public class ConfigurationTest {
    
    @Test
    public void makeConfiguration() throws FileNotFoundException, JAXBException {
        String filename = "src/main/resources/transponder.xml";
        TransponderConfiguration conf = new TransponderConfiguration();
        conf.setOwnMmsi(219230000);
        conf.setServerUrl("ws://localhost:8080/");
        conf.setUsername("anonymous");
        conf.setPort(8001);
        
        TransponderConfiguration.save(filename, conf);        
        conf = TransponderConfiguration.load(filename);
    }

}
