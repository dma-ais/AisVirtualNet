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
package dk.dma.ais.virtualnet.common.table;

import java.util.Comparator;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import net.jcip.annotations.ThreadSafe;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisStaticCommon;
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.enav.model.geometry.Position;

@ThreadSafe
@XmlRootElement
public class TargetTableEntry {

    /**
     * Default time a target is considered to be alive
     */
    private static final int DEFAULT_TTL = 10 * 60 * 1000;  // 10 min

    private int mmsi;
    private String name;
    private double lat;
    private double lon;
    private long lastMessage;

    public TargetTableEntry() {

    }

    public synchronized void update(AisMessage message) {
        mmsi = message.getUserId();
        lastMessage = System.currentTimeMillis();
        if (message instanceof IVesselPositionMessage) {
            Position pos = ((IVesselPositionMessage) message).getPos().getGeoLocation();
            if (pos != null) {
                lat = pos.getLatitude();
                lon = pos.getLongitude();
            }
        } else if (message instanceof AisStaticCommon) {
            String n = ((AisStaticCommon) message).getName();
            if (n != null) {
                name = AisMessage.trimText(n);
            }
        }

    }

    public synchronized int getMmsi() {
        return mmsi;
    }
    
    public synchronized void setMmsi(int mmsi) {
        this.mmsi = mmsi;
    }

    public synchronized String getName() {
        return name;
    }
    
    public synchronized void setName(String name) {
        this.name = name;
    }

    public synchronized double getLat() {
        return lat;
    }
    
    public synchronized void setLat(double lat) {
        this.lat = lat;
    }

    public synchronized double getLon() {
        return lon;
    }
    
    public synchronized void setLon(double lon) {
        this.lon = lon;
    }
    
    public synchronized long getLastMessage() {
        return lastMessage;
    }
    
    public synchronized void setLastMessage(long lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public synchronized boolean isAlive(int ttl) {
        return (System.currentTimeMillis() - lastMessage) < ttl;
    }
    
    @XmlTransient
    public synchronized boolean isAlive() {
        return isAlive(DEFAULT_TTL);
    }
    
    @Override
    public synchronized String toString() {
        return (name != null ? name : "N/A") + " (" + Integer.toString(mmsi) + ")";
    }
    
    /**
     * Comparator for doing name sorting
     */
    public static class NameSort implements Comparator<TargetTableEntry> {
        @Override
        public int compare(TargetTableEntry e1, TargetTableEntry e2) {
            // The string comparison (no name lowest order)
            int nc;
            if (e1.getName() == null) {
                nc = e2.getName() == null ? 0 : 1;
            } else {
                nc = e2.getName() == null ? -1 : e1.getName().compareTo(e2.getName());
            }
            if (nc != 0) {
                return nc;
            }
            // Compare mmsi
            return Integer.compare(e1.getMmsi(), e2.getMmsi());
        }
        
    }    

}
