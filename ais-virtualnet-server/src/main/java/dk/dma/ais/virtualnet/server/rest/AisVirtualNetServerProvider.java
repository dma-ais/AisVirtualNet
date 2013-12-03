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

import javax.ws.rs.ext.Provider;

import dk.dma.ais.virtualnet.server.AisVirtualNetServer;

/**
 * Provider for the server
 */
@Provider
public class AisVirtualNetServerProvider /* extends SingletonTypeInjectableProvider<Context, AisVirtualNetServer> */{

    static AisVirtualNetServer server;

    // public AisVirtualNetServerProvider() {
    // super(AisVirtualNetServer.class, server);
    // }

    public static void setServer(AisVirtualNetServer server) {
        AisVirtualNetServerProvider.server = server;
    }

}
