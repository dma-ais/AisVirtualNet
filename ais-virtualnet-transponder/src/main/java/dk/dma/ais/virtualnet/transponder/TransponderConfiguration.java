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

public class TransponderConfiguration {
    
    private Integer ownMmsi;
    private int ownPosInterval = 5; // five seconds
    private int receiveRadius = 75000; // 75 km (approx 40 nm)
    private String serverUrl;
    private String username;
    private String password;
    
    public TransponderConfiguration() {
        
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
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

    public Integer getOwnMmsi() {
        return ownMmsi;
    }

    public void setOwnMmsi(Integer ownMmsi) {
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
    
}
