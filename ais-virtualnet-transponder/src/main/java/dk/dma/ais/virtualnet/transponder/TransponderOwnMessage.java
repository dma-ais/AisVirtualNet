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

import net.jcip.annotations.ThreadSafe;

/**
 * Utility class to hold latest OWN message and resend with certain interval
 */
@ThreadSafe
public class TransponderOwnMessage extends Thread {

    private static final long MESSAGE_MAX_AGE = 20 * 60 * 1000; // 20 minutes

    private Long lastReceived;
    private String ownMessage;
    private final int forceInterval;
    private final Transponder transponder;

    public TransponderOwnMessage(Transponder transponder, int forceInterval) {
        this.transponder = transponder;
        this.forceInterval = forceInterval;
    }

    @Override
    public void run() {
        // Should own message re-sending be forced
        if (forceInterval == 0) {
            return;
        }

        // Enter re-send loop
        while (true) {
            try {
                Thread.sleep(forceInterval * 1000);
            } catch (InterruptedException e) {
                return;
            }
            reSend();
        }
    }

    private synchronized void reSend() {
        long elapsed = 0L;

        if (ownMessage == null) {
            return;
        }
        // Determine last send elapsed
        elapsed = System.currentTimeMillis() - lastReceived;

        // Do not send if already sent with interval
        if (elapsed < forceInterval * 1000) {
            return;
        }
        // Send if not too old
        if (elapsed < MESSAGE_MAX_AGE) {
            transponder.send(ownMessage);
        }
    }

    public synchronized void setOwnMessage(String ownMessage) {
        this.ownMessage = ownMessage;
        this.lastReceived = System.currentTimeMillis();
    }

}
