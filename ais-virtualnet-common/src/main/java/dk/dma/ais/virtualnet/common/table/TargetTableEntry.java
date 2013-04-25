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

import net.jcip.annotations.ThreadSafe;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisStaticCommon;
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.enav.model.geometry.Position;

@ThreadSafe
public class TargetTableEntry {

    private int mmsi;
    private String name;
    private double lat;
    private double lon;

    public TargetTableEntry() {

    }

    public synchronized void update(AisMessage message) {
        if (message instanceof IVesselPositionMessage) {
            Position pos = ((IVesselPositionMessage) message).getPos().getGeoLocation();
            if (pos != null) {
                lat = pos.getLatitude();
                lon = pos.getLongitude();
            }
        } else if (message instanceof AisStaticCommon) {
            String n = ((AisStaticCommon) message).getName();
            if (n != null) {
                name = n;
            }
        }

    }

    public synchronized int getMmsi() {
        return mmsi;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized double getLat() {
        return lat;
    }

    public synchronized double getLon() {
        return lon;
    }

}
