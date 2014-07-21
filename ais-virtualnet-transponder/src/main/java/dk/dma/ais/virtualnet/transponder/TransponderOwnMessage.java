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

import dk.dma.ais.packet.AisPacket;
import net.jcip.annotations.ThreadSafe;

/**
 * Utility class to hold latest OWN message and resend with certain interval
 */
@ThreadSafe
public class TransponderOwnMessage extends Thread {

    private static final long MESSAGE_MAX_AGE = 20 * 60 * 1000; // 20 minutes

    private Long lastReceived;
    private AisPacket ownPacket;
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

        if (ownPacket == null) {
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
            transponder.send(ownPacket.getStringMessage());
        } else {
            // Has become too old
            transponder.getStatus().setOwnPos(null);
        }
    }

    public synchronized void setOwnMessage(AisPacket ownPacket) {
        this.ownPacket = ownPacket;
        this.lastReceived = System.currentTimeMillis();
    }

}
