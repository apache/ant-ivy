/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

/**
 * Event reporting a stream copy progression
 */
public class CopyProgressEvent {
    private long _totalReadBytes;
    private byte[] _buffer;
    private int _readBytes;
    
    public CopyProgressEvent() {
    }
    public CopyProgressEvent(byte[] buffer, int read, long total) {
        update(buffer, read, total);
    }
    public CopyProgressEvent(byte[] buffer, long total) {
        update(buffer, 0, total);
    }
    protected CopyProgressEvent update(byte[] buffer, int read, long total) {
        _buffer = buffer;
        _readBytes = read;
        _totalReadBytes = total;
        return this;
    }
    public long getTotalReadBytes() {
        return _totalReadBytes;
    }
    public byte[] getBuffer() {
        return _buffer;
    }
    
    public int getReadBytes() {
        return _readBytes;
    }
    
}
