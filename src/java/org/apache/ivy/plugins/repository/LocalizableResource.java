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

/**
 * Resource which can be located locally
 * <p>
 * If local (checked via {@link #isLocal()}), {@link #getFile()} will return its local location,
 * without any download or copy involved
 * </p>
 */
public interface LocalizableResource extends Resource {

    /**
     * @return the local file of this resource.
     * @throws IllegalStateException
     *             when {@link #isLocal()} returns <code>false</code>
     */
    public File getFile();

}
