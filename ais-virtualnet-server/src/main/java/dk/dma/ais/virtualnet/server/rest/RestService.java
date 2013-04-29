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
package dk.dma.ais.virtualnet.server.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import dk.dma.ais.virtualnet.common.message.AuthenticationReplyMessage;
import dk.dma.ais.virtualnet.common.message.TargetTableMessage;
import dk.dma.ais.virtualnet.server.AisVirtualNetServer;

/**
 * JAX-RS rest services
 */
@Path("/")
public class RestService {
    
    @Context
    private AisVirtualNetServer server;

    @GET
    @Path("target_table")
    @Produces(MediaType.APPLICATION_JSON)
    public TargetTableMessage getTargetTable() {
        return server.getTargetTable().getTargetTableMessage();
    }
    
    @GET
    @Path("authenticate")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationReplyMessage authenticate(@QueryParam("username") String username, @QueryParam("password") String password) {
        // TODO authenticate
        
        AuthenticationReplyMessage reply = new AuthenticationReplyMessage();
        reply.setAuthToken("TOKEN");
        return reply;
    }
    

}
