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
package org.apache.ivy.core;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Resolve relative url relatively to the current execution directory instead of relatively to the
 * context. This is was actually done prior 2.0. This class allow thus to work in a backward
 * compatible mode.
 */
public class ExecutionRelativeUrlResolver extends RelativeUrlResolver {

    public URL getURL(URL context, String url) throws MalformedURLException {
        return new URL(url);
    }

}
