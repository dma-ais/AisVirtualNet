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

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.junit.Test;

public class ConfigurationTest {
    
    @Test
    public void makeConfiguration() throws FileNotFoundException, JAXBException {
        String filename = "src/main/resources/transponder.xml";
        TransponderConfiguration conf = new TransponderConfiguration();
        conf.setOwnMmsi(219230000);
        conf.setServerHost("localhost");
        conf.setServerPort(8080);
        conf.setUsername("anonymous");
        conf.setPort(8001);
        
        TransponderConfiguration.save(filename, conf);        
        conf = TransponderConfiguration.load(filename);
    }

}
