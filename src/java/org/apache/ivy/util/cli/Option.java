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
import java.util.ListIterator;

public class Option {
    private String name;

    private String[] args;

    private String description;

    private boolean required;

    private boolean countArgs;

    private boolean deprecated;

    Option(String name, String[] args, String description, boolean required, boolean countArgs,
            boolean deprecated) {
        this.name = name;
        this.args = args;
        this.description = description;
        this.required = required;
        this.countArgs = countArgs;
        this.deprecated = deprecated;
        if (required) {
            throw new UnsupportedOperationException("required option not supported yet");
        }
    }

    public String getName() {
        return name;
    }

    public String[] getArgs() {
        return args;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isCountArgs() {
        return countArgs;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    String[] parse(ListIterator iterator) throws ParseException {
        if (isCountArgs()) {
            String[] values = new String[args.length];
            for (int i = 0; i < values.length; i++) {
                if (!iterator.hasNext()) {
                    missingArgument(i);
                }
                values[i] = (String) iterator.next();
                if (values[i].startsWith("-")) {
                    missingArgument(i);
                }
            }
            return values;
        } else {
            List values = new ArrayList();
            while (iterator.hasNext()) {
                String value = (String) iterator.next();
                if (value.startsWith("-")) {
                    iterator.previous();
                    break;
                }
                values.add(value);
            }
            return (String[]) values.toArray(new String[values.size()]);
        }
    }

    private void missingArgument(int i) throws ParseException {
        if (i == 0) {
            throw new ParseException("no argument for: " + name);
        } else {
            throw new ParseException("missing argument for: " + name + ". Expected: "
                    + getArgsSpec());
        }
    }

    public String getSpec() {
        return "-" + name + " " + getArgsSpec();
    }

    private String getArgsSpec() {
        if (args.length == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
            sb.append("<").append(args[i]).append("> ");
        }
        return sb.toString();
    }
}
