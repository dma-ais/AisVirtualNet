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
package dk.dma.ais.virtualnet.common.message;

import dk.dma.ais.packet.AisPacket;

/**
 * Message class for messages sent between virtual transponder and virtual AIS 
 * network over websocket connection
 */
public class WsMessage {
    
    private String packet;
    private String authToken;
    
    public WsMessage() {
        
    }
    
    public WsMessage(AisPacket packet) {
        this.packet = packet.getStringMessage();
    }
    
    public String getPacket() {
        return packet;
    }
    
    public void setPacket(String packet) {
        this.packet = packet;
    }
    
    public String getAuthToken() {
        return authToken;
    }
    
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

}
