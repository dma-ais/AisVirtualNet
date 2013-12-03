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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.virtualnet.common.message.AuthenticationReplyMessage;
import dk.dma.ais.virtualnet.common.message.ReserveMmsiReplyMessage;
import dk.dma.ais.virtualnet.common.message.TargetTableMessage;
import dk.dma.ais.virtualnet.common.security.Password;

/**
 * Client for doing rest request to the server
 */
public class RestClient {

    private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

    private final WebTarget service;

    public RestClient(String hostname, int port) {
        Client client = ClientBuilder.newClient();
        service = client.target(UriBuilder.fromUri(String.format("http://%s:%d/rest", hostname, port)).build());
    }

    public AuthenticationReplyMessage authenticate(String username, String password) throws RestException {
        String hashed = Password.hashPassword(password);
        LOG.info("Authenticate username: " + username + " password: " + hashed);
        try {
            return service.path("authenticate").queryParam("username", username)
                    .queryParam("password", hashed).request(MediaType.APPLICATION_JSON).get(AuthenticationReplyMessage.class);
        } catch (Exception e) {
            LOG.error("RestClient failed: " + e.getMessage());
            throw new RestException(e);
        }
    }

    public ReserveMmsiReplyMessage reserveMmsi(Integer mmsi, String authToken) throws RestException {
        LOG.info("Resverse mmsi: " + mmsi + " authToken: " + authToken);
        try {
            return service.path("reserve_mmsi").queryParam("mmsi", Integer.toString(mmsi))
                    .queryParam("authToken", authToken).request(MediaType.APPLICATION_JSON).get(ReserveMmsiReplyMessage.class);
        } catch (Exception e) {
            LOG.error("RestClient failed: " + e.getMessage());
            throw new RestException(e);
        }
    }

    public TargetTableMessage getTargetTable(String username, String password) throws RestException {
        String hashed = Password.hashPassword(password);
        try {
            return service.path("target_table").queryParam("username", username)
                    .queryParam("password", hashed).request(MediaType.APPLICATION_JSON).get(TargetTableMessage.class);
        } catch (Exception e) {
            LOG.error("RestClient failed: " + e.getMessage());
            throw new RestException(e);
        }
    }

    public String test() {
        try {
            return service.path("test").request(MediaType.TEXT_PLAIN).get(String.class);
        } catch (Exception e) {
            LOG.error("RestClient failed: " + e.getMessage());
            return null;
        }
    }

}
