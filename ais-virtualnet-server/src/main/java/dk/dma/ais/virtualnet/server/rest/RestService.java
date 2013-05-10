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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.virtualnet.common.message.AuthenticationReplyMessage;
import dk.dma.ais.virtualnet.common.message.ReserveMmsiReplyMessage;
import dk.dma.ais.virtualnet.common.message.StatusMessage;
import dk.dma.ais.virtualnet.common.message.TargetTableMessage;
import dk.dma.ais.virtualnet.common.message.ReserveMmsiReplyMessage.ReserveResult;
import dk.dma.ais.virtualnet.server.AisVirtualNetServer;

/**
 * JAX-RS rest services
 */
@Path("/")
public class RestService {
    
    private static final Logger LOG = LoggerFactory.getLogger(RestService.class);
    
    @Context
    private AisVirtualNetServer server;
    
    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    public StatusMessage status() {
        return server.getStatus();
    }


    @GET
    @Path("target_table")
    @Produces(MediaType.APPLICATION_JSON)
    public TargetTableMessage getTargetTable(@QueryParam("username") String username, @QueryParam("password") String password) {
        LOG.info("Getting target table for user: " + username + " password: " + password);
        if (server.getAuthenticator().authenticate(username, password) == null) {
            LOG.error("\tFailed to authenticate user");
            return new TargetTableMessage();
        }
        return server.getTargetTable().getAliveTargetTableMessage();
    }
    
    @GET
    @Path("authenticate")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationReplyMessage authenticate(@QueryParam("username") String username, @QueryParam("password") String password) {
        LOG.info("Authenticating user: " + username);
        AuthenticationReplyMessage reply = new AuthenticationReplyMessage();
        String authToken = server.getAuthenticator().authenticate(username, password);
        reply.setAuthToken(authToken);
        if (authToken == null) {
            reply.setErrorMessage("\tWrong credentials");
        }
        LOG.info("\tAuthentication token: " + authToken);
        return reply;
    }

    @GET
    @Path("validate")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationReplyMessage validate(@QueryParam("authToken") String authToken) {
        LOG.info("Validating token: " + authToken);
        AuthenticationReplyMessage reply = new AuthenticationReplyMessage();
        if (server.getAuthenticator().validate(authToken)) {
            reply.setAuthToken(authToken);
        } else {
            LOG.info("\tFailed to validate token: " + authToken);
            reply.setErrorMessage("Invalid token");
        }
        return reply;
    }
    
    @GET
    @Path("reserve_mmsi")
    @Produces(MediaType.APPLICATION_JSON)
    public ReserveMmsiReplyMessage reserverMmsi(@QueryParam("mmsi") Integer mmsi, @QueryParam("authToken") String authToken) {
        LOG.info("Reserving mmsi: " + mmsi + " authToken: " + authToken);
        ReserveResult result;
        if (!server.getAuthenticator().validate(authToken)) {
            result = ReserveResult.NOT_AUTHENTICATED;
        } else {
            result = server.getMmsiBroker().reserve(mmsi, authToken);
        }
        LOG.info("\tReserve result: " + result);
        return new ReserveMmsiReplyMessage(result);
    }

}
