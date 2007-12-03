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

import org.apache.ivy.core.event.IvyEvent;

/**
 * TransferEvent is used to notify TransferListeners about progress in transfer of resources form/to
 * the respository This class is LARGELY inspired by org.apache.maven.wagon.events.TransferEvent
 * released under the following copyright license:
 * 
 * <pre>
 * 
 *  Copyright 2001-2005 The Apache Software Foundation.
 * 
 *  Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 * </pre>
 * 
 * Orginal class written by Michal Maczka.
 */
public class TransferEvent extends IvyEvent {
    /**
     * A transfer was attempted, but has not yet commenced.
     */
    public static final int TRANSFER_INITIATED = 0;

    /**
     * A transfer was started.
     */
    public static final int TRANSFER_STARTED = 1;

    /**
     * A transfer is completed.
     */
    public static final int TRANSFER_COMPLETED = 2;

    /**
     * A transfer is in progress.
     */
    public static final int TRANSFER_PROGRESS = 3;

    /**
     * An error occured during transfer
     */
    public static final int TRANSFER_ERROR = 4;

    /**
     * Indicates GET transfer (from the repository)
     */
    public static final int REQUEST_GET = 5;

    /**
     * Indicates PUT transfer (to the repository)
     */
    public static final int REQUEST_PUT = 6;

    public static final String TRANSFER_INITIATED_NAME = "transfer-initiated";

    public static final String TRANSFER_STARTED_NAME = "transfer-started";

    public static final String TRANSFER_PROGRESS_NAME = "transfer-progress";

    public static final String TRANSFER_COMPLETED_NAME = "transfer-completed";

    public static final String TRANSFER_ERROR_NAME = "transfer-error";

    private Resource resource;

    private int eventType;

    private int requestType;

    private Exception exception;

    private File localFile;

    private Repository repository;

    private long length;

    private long totalLength;

    private boolean isTotalLengthSet = false;

    public TransferEvent(final Repository repository, final Resource resource, final int eventType,
            final int requestType) {
        super(getName(eventType));

        this.repository = repository;
        addAttribute("repository", this.repository.getName());
        this.resource = resource;
        addAttribute("resource", this.resource.getName());

        setEventType(eventType);

        setRequestType(requestType);
        addAttribute("request-type", requestType == REQUEST_GET ? "get" : "put");
    }

    public TransferEvent(final Repository repository, final Resource resource,
            final Exception exception, final int requestType) {
        this(repository, resource, TRANSFER_ERROR, requestType);

        this.exception = exception;
    }

    public TransferEvent(final Repository repository, final Resource resource, long length,
            final int requestType) {
        this(repository, resource, TRANSFER_PROGRESS, requestType);

        this.length = length;
        this.totalLength = length;
    }

    private static String getName(int eventType) {
        switch (eventType) {
            case TRANSFER_INITIATED:
                return TRANSFER_INITIATED_NAME;
            case TRANSFER_STARTED:
                return TRANSFER_STARTED_NAME;
            case TRANSFER_PROGRESS:
                return TRANSFER_PROGRESS_NAME;
            case TRANSFER_COMPLETED:
                return TRANSFER_COMPLETED_NAME;
            case TRANSFER_ERROR:
                return TRANSFER_ERROR_NAME;
            default:
                return null;
        }
    }

    /**
     * @return Returns the resource.
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * @return Returns the exception.
     */
    public Exception getException() {
        return exception;
    }

    /**
     * Returns the request type.
     * 
     * @return Returns the request type. The Request type is one of
     *         <code>TransferEvent.REQUEST_GET<code> or <code>TransferEvent.REQUEST_PUT<code>
     */
    public int getRequestType() {
        return requestType;
    }

    /**
     * Sets the request type
     * 
     * @param requestType
     *            The requestType to set. The Request type value should be either
     *            <code>TransferEvent.REQUEST_GET<code> or <code>TransferEvent.REQUEST_PUT<code>.
     * @throws IllegalArgumentException when
     */
    protected void setRequestType(final int requestType) {
        switch (requestType) {

            case REQUEST_PUT:
                break;
            case REQUEST_GET:
                break;

            default:
                throw new IllegalArgumentException("Illegal request type: " + requestType);
        }

        this.requestType = requestType;
    }

    /**
     * @return Returns the eventType.
     */
    public int getEventType() {
        return eventType;
    }

    /**
     * @param eventType
     *            The eventType to set.
     */
    protected void setEventType(final int eventType) {
        switch (eventType) {

            case TRANSFER_INITIATED:
                break;
            case TRANSFER_STARTED:
                break;
            case TRANSFER_COMPLETED:
                break;
            case TRANSFER_PROGRESS:
                break;
            case TRANSFER_ERROR:
                break;
            default:
                throw new IllegalArgumentException("Illegal event type: " + eventType);
        }

        this.eventType = eventType;
    }

    /**
     * @param resource
     *            The resource to set.
     */
    protected void setResource(final Resource resource) {
        this.resource = resource;
    }

    /**
     * @return Returns the local file.
     */
    public File getLocalFile() {
        return localFile;
    }

    /**
     * @param localFile
     *            The local file to set.
     */
    protected void setLocalFile(File localFile) {
        this.localFile = localFile;
    }

    public long getLength() {
        return length;
    }

    protected void setLength(long length) {
        this.length = length;
    }

    public long getTotalLength() {
        return totalLength;
    }

    protected void setTotalLength(long totalLength) {
        this.totalLength = totalLength;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public boolean isTotalLengthSet() {
        return isTotalLengthSet;
    }

    public void setTotalLengthSet(boolean isTotalLengthSet) {
        this.isTotalLengthSet = isTotalLengthSet;
    }

}
