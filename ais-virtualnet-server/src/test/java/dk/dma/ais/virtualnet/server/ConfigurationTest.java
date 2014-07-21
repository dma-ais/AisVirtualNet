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

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Test;

import dk.dma.ais.bus.AisBus;
import dk.dma.ais.configuration.bus.AisBusConfiguration;
import dk.dma.ais.configuration.bus.provider.RepeatingFileReaderProviderConfiguration;
import dk.dma.ais.configuration.filter.TaggingFilterConfiguration;
import dk.dma.ais.configuration.transform.PacketTaggingConfiguration;
import dk.dma.ais.configuration.transform.ReplayTransformConfiguration;

public class ConfigurationTest {
    
    @Test
    public void makeConfiguration() throws FileNotFoundException, JAXBException {
        String filename = "src/main/resources/server-test.xml";
        ServerConfiguration conf = new ServerConfiguration();
        AisBusConfiguration aisBusConf = new AisBusConfiguration();
        conf.setAisbusConfiguration(aisBusConf);
        
        // Repeating file provider with replay
        RepeatingFileReaderProviderConfiguration reader = new RepeatingFileReaderProviderConfiguration();
        reader.setFilename("src/test/resources/ais.txt");
        reader.getTransformers().add(new ReplayTransformConfiguration());        
        aisBusConf.getProviders().add(reader);
        
        // Only use a single base station
        TaggingFilterConfiguration tagFilterConf = new TaggingFilterConfiguration();
        PacketTaggingConfiguration tagConf = new PacketTaggingConfiguration();
        tagConf.setSourceBs(2190047);
        tagFilterConf.setFilterTagging(tagConf);
        aisBusConf.getFilters().add(tagFilterConf);        
        
        ServerConfiguration.save(filename, conf);
        
        conf = ServerConfiguration.load(filename);
        AisBus aisBus = conf.getAisbusConfiguration().getInstance();
        Assert.assertNotNull(aisBus);
    }

}
