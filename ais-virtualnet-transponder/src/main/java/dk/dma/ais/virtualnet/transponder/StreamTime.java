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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;

import dk.dma.ais.sentence.Sentence;
import dk.dma.ais.sentence.SentenceException;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class StreamTime {
        
    private static final int INTERVAL = 10000;
    
    private static final SimpleDateFormat DATEFORMAT; 
    
    static {
        DATEFORMAT = new SimpleDateFormat("yyyyMMdd HHmmss");
        DATEFORMAT.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
    }    
   
    private long lastSent;
    private long offset;
    
        
    public void setStreamTime(long timestamp) {
        offset = System.currentTimeMillis() - timestamp;
    }
    
    public long getTime() {
        return System.currentTimeMillis() - offset;
    }
    
    public boolean isDue() {
        return System.currentTimeMillis() - lastSent > INTERVAL;        
    }
    
    public String createPstt() {
        lastSent = System.currentTimeMillis();
        String dateStr = DATEFORMAT.format(new Date(getTime()));
        String[] dateParts = StringUtils.split(dateStr);
        String sentence = String.format("$PSTT,10A,%s,%s", dateParts[0], dateParts[1]);
        String checksum = "00";
        try {
            checksum = Sentence.getStringChecksum(Sentence.getChecksum(sentence));
        } catch (SentenceException e) {
            e.printStackTrace();
        }
        return sentence + "*" + checksum;
    }

}
