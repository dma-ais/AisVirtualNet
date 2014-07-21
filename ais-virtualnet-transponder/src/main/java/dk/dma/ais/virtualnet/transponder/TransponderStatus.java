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

import java.util.concurrent.CopyOnWriteArraySet;

import dk.dma.enav.model.geometry.Position;
import net.jcip.annotations.ThreadSafe;

/**
 * Class to represent the current state of the transponder
 */
@ThreadSafe
public class TransponderStatus {

    private Position ownPos;
    private boolean clientConnected;
    private boolean serverConnected;
    private String serverError;
    private String shipName = "";

    private final CopyOnWriteArraySet<ITransponderStatusListener> listeners = new CopyOnWriteArraySet<>();

    public TransponderStatus() {

    }

    public synchronized boolean isClientConnected() {
        return clientConnected;
    }

    public void setClientConnected(boolean clientConnected) {
        synchronized (this) {
            this.clientConnected = clientConnected;
        }
        notifyListeners();
    }

    public synchronized Position getOwnPos() {
        return ownPos;
    }

    public void setOwnPos(Position ownPos) {
        synchronized (this) {
            this.ownPos = ownPos;
        }
        notifyListeners();
    }

    public synchronized boolean isServerConnected() {
        return serverConnected;
    }

    public void setServerConnected(boolean serverConnected) {
        synchronized (this) {
            this.serverConnected = serverConnected;
        }
        notifyListeners();
    }

    private void notifyListeners() {
        for (ITransponderStatusListener listener : listeners) {
            listener.stateChanged(this);
        }
    }

    public synchronized String getServerError() {
        return serverError;
    }

    public void setServerError(String serverError) {
        synchronized (this) {
            this.serverError = serverError;
        }
        notifyListeners();
    }

    public synchronized String getShipName() {
        return shipName;
    }

    public void setShipName(String shipName) {
        synchronized (this) {
            this.shipName = shipName;
        }
        notifyListeners();
    }

    public void addListener(ITransponderStatusListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ITransponderStatusListener listener) {
        listeners.remove(listener);
    }
}
