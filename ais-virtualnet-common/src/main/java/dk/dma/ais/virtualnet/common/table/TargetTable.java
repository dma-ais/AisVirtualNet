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
package dk.dma.ais.virtualnet.common.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.jcip.annotations.ThreadSafe;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisStaticCommon;
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.virtualnet.common.message.TargetTableMessage;

/**
 * Simple table of AIS vessel targets
 */
@ThreadSafe
public class TargetTable {
    
    private final ConcurrentHashMap<Integer, TargetTableEntry> targets = new ConcurrentHashMap<>();
    
    public TargetTable() {
        
    }
    
    public void update(AisPacket packet) {
        AisMessage message = packet.tryGetAisMessage();
        if (message == null) {
            return;
        }
        if (!(message instanceof IVesselPositionMessage) && !(message instanceof AisStaticCommon)) {
            return;
        }
        TargetTableEntry newEntry = new TargetTableEntry();
        TargetTableEntry entry = targets.putIfAbsent(message.getUserId(), newEntry);
        if (entry == null) {
            entry = newEntry;
        }
        entry.update(message);
    }
    
    public Map<Integer, TargetTableEntry> allTargets() {
        return Collections.unmodifiableMap(targets);
    }
    
    /**
     * Return a message version of the target table
     * @return
     */
    public TargetTableMessage getTargetTableMessage() {
        TargetTableMessage message = new TargetTableMessage();
        for (TargetTableEntry target : targets.values()) {
            message.getTargets().add(target);
        }
        return message;
    }
    
    /**
     * Return a message version of the target table with alive targets
     * @return
     */
    public TargetTableMessage getAliveTargetTableMessage() {
        TargetTableMessage message = new TargetTableMessage();
        for (TargetTableEntry target : targets.values()) {
            if (target.isAlive()) {
                message.getTargets().add(target);
            }
        }
        return message;
    }

    /**
     * Return if target exists and is alive
     * @param mmsi
     * @return
     */
    public boolean exists(int mmsi) {
        TargetTableEntry target = targets.get(mmsi);
        return target != null && target.isAlive();  
    }

    /**
     * Remove old targets
     */
    public void cleanup() {
        List<Integer> removeTargets = new ArrayList<>();
        for (TargetTableEntry target : targets.values()) {
            if (!target.isAlive()) {
                removeTargets.add(target.getMmsi());
            }
        }
        for (Integer mmsi : removeTargets) {
            targets.remove(mmsi);
        }
        
    }

}
