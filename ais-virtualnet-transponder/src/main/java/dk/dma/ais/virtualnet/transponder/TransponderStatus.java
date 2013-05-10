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
