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

import java.util.HashMap;
import java.util.Map;

public class CommandLine {
    private Map/* <String, String[]> */optionValues = new HashMap();

    private String[] leftOverArgs;

    void addOptionValues(String option, String[] values) {
        optionValues.put(option, values);
    }

    void setLeftOverArgs(String[] args) {
        leftOverArgs = args;
    }

    public boolean hasOption(String option) {
        return optionValues.containsKey(option);
    }

    public String getOptionValue(String option) {
        String[] values = getOptionValues(option);
        return values == null || values.length == 0 ? null : values[0];
    }

    public String getOptionValue(String option, String defaultValue) {
        String value = getOptionValue(option);
        return value == null ? defaultValue : value;
    }

    public String[] getOptionValues(String option) {
        return (String[]) optionValues.get(option);
    }

    public String[] getLeftOverArgs() {
        return leftOverArgs;
    }

}
