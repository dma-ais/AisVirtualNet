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
