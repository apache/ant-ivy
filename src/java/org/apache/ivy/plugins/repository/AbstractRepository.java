/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.plugins.repository;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.event.EventListenerList;

import org.apache.ivy.core.module.descriptor.Artifact;



public abstract class AbstractRepository implements Repository {
    private EventListenerList _listeners = new EventListenerList();
    private String _name;
    private TransferEvent _evt;
    
    public void addTransferListener(TransferListener listener) {
        _listeners.add(TransferListener.class, listener);
    }

    public void removeTransferListener(TransferListener listener) {
        _listeners.remove(TransferListener.class, listener);
    }

    public boolean hasTransferListener(TransferListener listener) {
        return Arrays.asList(_listeners.getListeners(TransferListener.class)).contains(listener);
    }
    
    protected void fireTransferInitiated(Resource res, int requestType) {
        _evt = new TransferEvent(this, res, TransferEvent.TRANSFER_INITIATED, requestType);
        fireTransferEvent(_evt);
    }
    
    protected void fireTransferStarted() {
        _evt.setEventType(TransferEvent.TRANSFER_STARTED);
        fireTransferEvent(_evt);
    }
    
    protected void fireTransferStarted(long totalLength) {
        _evt.setEventType(TransferEvent.TRANSFER_STARTED);
        _evt.setTotalLength(totalLength);
        _evt.setTotalLengthSet(true);
        fireTransferEvent(_evt);
    }
    
    protected void fireTransferProgress(long length) {
        _evt.setEventType(TransferEvent.TRANSFER_PROGRESS);
        _evt.setLength(length);
        if (!_evt.isTotalLengthSet()) {
            _evt.setTotalLength(_evt.getTotalLength() + length);
        }
        fireTransferEvent(_evt);
    }
    
    protected void fireTransferCompleted() {
        _evt.setEventType(TransferEvent.TRANSFER_COMPLETED);
        if (_evt.getTotalLength() > 0 && !_evt.isTotalLengthSet()) {
        	_evt.setTotalLengthSet(true);
        }
        fireTransferEvent(_evt);
    }
    
    protected void fireTransferCompleted(long totalLength) {
        _evt.setEventType(TransferEvent.TRANSFER_COMPLETED);
        _evt.setTotalLength(totalLength);
        _evt.setTotalLengthSet(true);
        fireTransferEvent(_evt);
    }
    
    protected void fireTransferError() {
        _evt.setEventType(TransferEvent.TRANSFER_ERROR);
        fireTransferEvent(_evt);
    }
    
    protected void fireTransferError(Exception ex) {
        _evt.setEventType(TransferEvent.TRANSFER_ERROR);
        _evt.setException(ex);
        fireTransferEvent(_evt);
    }
    
    protected void fireTransferEvent(TransferEvent evt) {
        Object[] listeners = _listeners.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TransferListener.class) {
                ((TransferListener)listeners[i+1]).transferProgress(evt);
            }
        }
    }

    public String getFileSeparator() {
        return "/";
    }

    public String standardize(String source) {
        return source.replace('\\', '/');
    }

    public String getName() {
        return _name;
    }
    

    public void setName(String name) {
        _name = name;
    }
    
    public String toString() {
        return getName();
    }
    
    public void put(Artifact artifact, File source, String destination, boolean overwrite) throws IOException {
    	put(source, destination, overwrite);
    }

	protected void put(File source, String destination, boolean overwrite) throws IOException {
		throw new UnsupportedOperationException("put in not supported by "+getName());
	}
}
