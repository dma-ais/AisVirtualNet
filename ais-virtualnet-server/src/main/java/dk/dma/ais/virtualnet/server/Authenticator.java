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
package dk.dma.ais.virtualnet.server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.virtualnet.common.security.Password;

/**
 * Class for handling authentication
 */
@ThreadSafe
public class Authenticator {

    private static final Logger LOG = LoggerFactory.getLogger(Authenticator.class);

    private static final long TTL = 300000; // 5 min

    private final ConcurrentHashMap<String, Long> tokenMap = new ConcurrentHashMap<>();
    private final Map<String, String> usersMap = new HashMap<>();

    public Authenticator(String usersFile) throws IOException {
        // Load users
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(usersFile)));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            line = line.trim();
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }
            String[] parts = line.split(":");
            if (parts.length != 2) {
                LOG.error("Malformed line in " + usersFile + ": " + line);
                continue;
            }
            usersMap.put(parts[0], parts[1]);
        }
        reader.close();
    }

    /**
     * Authenticate user. Returning auth token on success and null on failure.
     * 
     * @param username
     * @param hashed
     * @return
     */
    public String authenticate(String username, String hashed) {
        if (username == null || hashed == null) {
            return null;
        }
        // Get clear text password
        String password = usersMap.get(username);
        if (password == null) {
            return null;
        }
        // Check password
        if (!Password.checkPassword(password, hashed)) {
            return null;
        }
        String authToken = UUID.randomUUID().toString();
        tokenMap.put(authToken, System.currentTimeMillis());
        return authToken;
    }

    /**
     * Validate token
     * 
     * @param authToken
     * @return
     */
    public boolean validate(String authToken) {
        cleanup();
        if (authToken == null) {
            return false;
        }
        return tokenMap.containsKey(authToken);
    }

    /**
     * Remove old tokens
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        for (Iterator<Entry<String, Long>> it = tokenMap.entrySet().iterator(); it.hasNext();) {
            Long time = ((Entry<String, Long>) it.next()).getValue();
            if (now - time > TTL) {
                it.remove();
            }
        }
    }

}
