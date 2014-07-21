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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(usersFile)))) {
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
        }
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
            Long time = it.next().getValue();
            if (now - time > TTL) {
                it.remove();
            }
        }
    }

}
