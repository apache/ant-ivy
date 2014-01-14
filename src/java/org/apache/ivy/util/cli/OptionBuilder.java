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
package org.apache.ivy.util.cli;

import java.util.ArrayList;
import java.util.List;

public class OptionBuilder {
    private String name;

    private List/* <String> */args = new ArrayList();

    private String description = "";

    private boolean required = false;

    private boolean countArgs = true;

    private boolean deprecated = false;

    public OptionBuilder(String name) {
        this.name = name;
    }

    public OptionBuilder required(boolean required) {
        this.required = required;
        return this;
    }

    public OptionBuilder description(String description) {
        this.description = description;
        return this;
    }

    public OptionBuilder arg(String argName) {
        this.args.add(argName);
        return this;
    }

    public OptionBuilder countArgs(boolean countArgs) {
        this.countArgs = countArgs;
        return this;
    }

    public OptionBuilder deprecated() {
        this.deprecated = true;
        return this;
    }

    public Option create() {
        return new Option(name, (String[]) args.toArray(new String[args.size()]), description,
                required, countArgs, deprecated);
    }
}
