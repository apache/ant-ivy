/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.core.event;

import java.util.Arrays;

import javax.swing.event.EventListenerList;

import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.plugins.repository.TransferListener;
import org.apache.ivy.util.filter.Filter;

public class EventManager implements TransferListener {

    private EventListenerList listeners = new EventListenerList();

    public void addIvyListener(IvyListener listener) {
        listeners.add(IvyListener.class, listener);
    }

    public void addIvyListener(IvyListener listener, String eventName) {
        addIvyListener(listener, new IvyEventFilter(eventName, null, null));
    }

    public void addIvyListener(IvyListener listener, Filter<IvyEvent> filter) {
        listeners.add(IvyListener.class, new FilteredIvyListener(listener, filter));
    }

    public void removeIvyListener(IvyListener listener) {
        listeners.remove(IvyListener.class, listener);
        IvyListener[] listeners = this.listeners.getListeners(IvyListener.class);
        for (IvyListener listen : listeners) {
            if (listen instanceof FilteredIvyListener) {
                if (listener.equals(((FilteredIvyListener) listen).getIvyListener())) {
                    this.listeners.remove(IvyListener.class, listen);
                }
            }
        }
    }

    public boolean hasIvyListener(IvyListener listener) {
        IvyListener[] listeners = this.listeners.getListeners(IvyListener.class);
        for (IvyListener listen : listeners) {
            if (listen instanceof FilteredIvyListener) {
                if (listener.equals(((FilteredIvyListener) listen).getIvyListener())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void fireIvyEvent(IvyEvent evt) {
        Object[] listeners = this.listeners.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == IvyListener.class) {
                ((IvyListener) listeners[i + 1]).progress(evt);
            }
        }
    }

    public void addTransferListener(TransferListener listener) {
        listeners.add(TransferListener.class, listener);
    }

    public void removeTransferListener(TransferListener listener) {
        listeners.remove(TransferListener.class, listener);
    }

    public boolean hasTransferListener(TransferListener listener) {
        return Arrays.asList(listeners.getListeners(TransferListener.class)).contains(listener);
    }

    protected void fireTransferEvent(TransferEvent evt) {
        Object[] listeners = this.listeners.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TransferListener.class) {
                ((TransferListener) listeners[i + 1]).transferProgress(evt);
            }
        }
    }

    public void transferProgress(TransferEvent evt) {
        fireTransferEvent(evt);
        fireIvyEvent(evt);
    }

}
