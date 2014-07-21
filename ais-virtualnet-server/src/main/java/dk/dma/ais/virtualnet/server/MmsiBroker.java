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

import java.util.HashMap;
import java.util.Map;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.virtualnet.common.message.ReserveMmsiReplyMessage.ReserveResult;

@ThreadSafe
public class MmsiBroker {

    private static final Logger LOG = LoggerFactory.getLogger(MmsiBroker.class);

    /**
     * How long to wait between reservation and activation
     */
    private static final long ACTIVATE_TIME = 60 * 1000; // 1 min

    /**
     * Map from mmsi to booking
     */
    private final Map<Integer, Booking> mmsiBookingMap = new HashMap<>();

    /**
     * Map from authToken to mmsi
     */
    private final Map<String, Integer> authTokenMmsiMap = new HashMap<>();

    /**
     * Reserve a mmsi
     * 
     * @param mmsi
     * @param authToken
     * @return
     */
    public synchronized ReserveResult reserve(int mmsi, String authToken) {
        LOG.info("Reserve mmsi: " + mmsi + " authToken: " + authToken);
        // No reservation needed for 0 and 9xxxx MMSI
        if (nonReservableMmsi(mmsi)) {
            authTokenMmsiMap.put(authToken, mmsi);
            return ReserveResult.MMSI_RESERVED;
        }
        Booking booking = mmsiBookingMap.get(mmsi);
        if (booking == null || !booking.isReserved()) {
            // Determine if target is visible
            // if (!targetTable.exists(mmsi)) {
            // return ReserveResult.MMSI_NOT_FOUND;
            // }
            booking = new Booking();
            mmsiBookingMap.put(mmsi, booking);
            authTokenMmsiMap.put(authToken, mmsi);
            return ReserveResult.MMSI_RESERVED;
        }
        return ReserveResult.MMSI_ALREADY_RESERVED;
    }

    /**
     * Activate a reservation
     * 
     * @param authToken
     */
    public synchronized boolean activate(String authToken) {
        LOG.info("Activate mmsi authToken: " + authToken);
        Integer mmsi = authTokenMmsiMap.get(authToken);
        if (mmsi == null) {
            LOG.error("No MMSI for authToken: " + authToken);
            return false;
        }
        if (nonReservableMmsi(mmsi)) {
            return true;
        }
        Booking booking = mmsiBookingMap.get(mmsi);
        if (booking == null) {
            LOG.error("No booking for MMSI: " + mmsi);
            return false;
        }
        if (booking.isActivated()) {
            LOG.error("Booking already activate for MMSI: " + mmsi);
            return false;
        }
        booking.activate();
        return true;
    }

    public synchronized void release(String authToken) {
        LOG.info("Release mmsi authToken: " + authToken);
        Integer mmsi = authTokenMmsiMap.get(authToken);
        authTokenMmsiMap.remove(authToken);
        if (mmsi == null) {
            LOG.error("No MMSI for authToken: " + authToken);
            return;
        }
        if (nonReservableMmsi(mmsi)) {
            return;
        }
        Booking booking = mmsiBookingMap.get(mmsi);
        if (booking == null) {
            LOG.error("No booking for MMSI: " + mmsi);
            return;
        }
        mmsiBookingMap.remove(mmsi);
    }

    public static boolean nonReservableMmsi(int mmsi) {
        return mmsi == 0 || mmsi >= 900000000;
    }

    private class Booking {
        private final long created;

        private long activated;

        public Booking() {
            this.created = System.currentTimeMillis();
        }

        public void activate() {
            this.activated = System.currentTimeMillis();
        }

        public boolean isActivated() {
            return activated > 0;
        }

        public boolean isReserved() {
            if (isActivated()) {
                return true;
            }
            long reserveAge = created - System.currentTimeMillis();
            return reserveAge < ACTIVATE_TIME;
        }

    }

}
