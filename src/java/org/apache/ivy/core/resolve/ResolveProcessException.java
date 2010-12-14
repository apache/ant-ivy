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
package org.apache.ivy.core.resolve;

/**
 * ResolveProcessException is an exception which is used to control the resolve process.
 * <p>
 * All {@link ResolveProcessException} subclasses have the ability to interrupt the resolve process
 * when thrown while resolving dependencies, instead of only marking the module with a problem and
 * continuing the resolve process as part of the best effort strategy during resolve process.
 * </p>
 * Some subclasses have even a stronger power over the resolve process, like
 * {@link RestartResolveProcess} which orders to restart the resolve process at the start.
 */
public class ResolveProcessException extends RuntimeException {

    public ResolveProcessException() {
    }

    public ResolveProcessException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResolveProcessException(String message) {
        super(message);
    }

    public ResolveProcessException(Throwable cause) {
        super(cause);
    }
}
