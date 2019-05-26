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

package org.apache.ivy.plugins.resolver;

import org.apache.ivy.core.settings.TimeoutConstraint;

/**
 * A {@link TimeoutConstraint} which determines the timeouts by invoking the {@link AbstractResolver
 * underlying resolver}'s {@link AbstractResolver#getTimeoutConstraint()}, whenever the timeouts are
 * requested for. This class can be used when the {@link TimeoutConstraint} is to be created but the
 * underlying resolver, which decides the timeouts, hasn't yet been fully initialized
 */
final class LazyTimeoutConstraint implements TimeoutConstraint {

    private final AbstractResolver resolver;

    public LazyTimeoutConstraint(final AbstractResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public int getConnectionTimeout() {
        final TimeoutConstraint resolverTimeoutConstraint = resolver.getTimeoutConstraint();
        return resolverTimeoutConstraint == null ? -1 : resolverTimeoutConstraint.getConnectionTimeout();
    }

    @Override
    public int getReadTimeout() {
        final TimeoutConstraint resolverTimeoutConstraint = resolver.getTimeoutConstraint();
        return resolverTimeoutConstraint == null ? -1 : resolverTimeoutConstraint.getReadTimeout();
    }
}
