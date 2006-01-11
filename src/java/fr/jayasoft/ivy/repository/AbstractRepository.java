/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository;

import java.util.Arrays;

import javax.swing.event.EventListenerList;


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
    
    protected void fireTransferProgress(byte[] buffer, long length) {
        _evt.setEventType(TransferEvent.TRANSFER_PROGRESS);
        _evt.setBuffer(buffer);
        _evt.setLength(length);
        if (!_evt.isTotalLengthSet()) {
            _evt.setTotalLength(_evt.getTotalLength() + length);
        }
        fireTransferEvent(_evt);
    }
    
    protected void fireTransferCompleted(byte[] buffer, long length) {
        _evt.setEventType(TransferEvent.TRANSFER_COMPLETED);
        _evt.setBuffer(buffer);
        _evt.setLength(length);
        if (!_evt.isTotalLengthSet()) {
            _evt.setTotalLength(_evt.getTotalLength() + length);
            _evt.setTotalLengthSet(true);
        }
        fireTransferEvent(_evt);
        _evt = null;
    }
    
    protected void fireTransferCompleted(int totalLength) {
        _evt.setEventType(TransferEvent.TRANSFER_COMPLETED);
        _evt.setBuffer(null);
        _evt.setTotalLength(totalLength);
        _evt.setTotalLengthSet(true);
        fireTransferEvent(_evt);
        _evt = null;
    }
    
    protected void fireTransferError() {
        _evt.setEventType(TransferEvent.TRANSFER_ERROR);
        fireTransferEvent(_evt);
        _evt = null;
    }
    
    protected void fireTransferError(Exception ex) {
        _evt.setEventType(TransferEvent.TRANSFER_ERROR);
        _evt.setException(ex);
        fireTransferEvent(_evt);
        _evt = null;
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
}
