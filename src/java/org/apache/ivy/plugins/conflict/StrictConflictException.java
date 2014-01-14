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
package org.apache.ivy.plugins.conflict;

import java.util.Arrays;

import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveProcessException;

public class StrictConflictException extends ResolveProcessException {

    public StrictConflictException() {
        super();
    }

    public StrictConflictException(IvyNode node1, IvyNode node2) {
        super(node1 + " (needed by " + Arrays.asList(node1.getAllRealCallers())
                + ") conflicts with " + node2 + " (needed by "
                + Arrays.asList(node2.getAllRealCallers()) + ")");
    }

    public StrictConflictException(String msg) {
        super(msg);
    }

    public StrictConflictException(Throwable t) {
        super(t);
    }

    public StrictConflictException(String msg, Throwable t) {
        super(msg, t);
    }

}
