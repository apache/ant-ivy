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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ivy.util.StringUtils;

public class CommandLineParser {
    private static final int MIN_DESC_WIDTH = 40;

    private static final int MAX_SPEC_WIDTH = 30;

    private Map<String, Option> options = new LinkedHashMap<String, Option>();

    private Map<String, List<Option>> categories = new LinkedHashMap<String, List<Option>>();

    public CommandLineParser() {
    }

    public CommandLineParser addCategory(String category) {
        categories.put(category, new ArrayList<Option>());
        return this;
    }

    public CommandLineParser addOption(Option option) {
        options.put(option.getName(), option);
        if (!categories.isEmpty()) {
            ((List<Option>) categories.values().toArray()[categories.values().size() - 1])
                    .add(option);
        }
        return this;
    }

    public CommandLine parse(String[] args) throws ParseException {
        CommandLine line = new CommandLine();

        int index = args.length;
        ListIterator<String> iterator = Arrays.asList(args).listIterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
            if ("--".equals(arg)) {
                // skip this argument and stop looping
                index = iterator.nextIndex();
                break;
            }

            if (!arg.startsWith("-")) {
                index = iterator.previousIndex();
                break;
            }

            Option option = options.get(arg.substring(1));
            if (option == null) {
                throw new ParseException("Unrecognized option: " + arg);
            }
            line.addOptionValues(arg.substring(1), option.parse(iterator));
        }

        // left over args
        String[] leftOverArgs = new String[args.length - index];
        System.arraycopy(args, index, leftOverArgs, 0, leftOverArgs.length);
        line.setLeftOverArgs(leftOverArgs);

        return line;
    }

    public void printHelp(PrintWriter pw, int width, String command, boolean showDeprecated) {
        pw.println("usage: " + command);
        // compute the largest option spec
        int specWidth = 0;
        for (Option option : options.values()) {
            if (option.isDeprecated() && !showDeprecated) {
                continue;
            }
            specWidth = Math.min(MAX_SPEC_WIDTH, Math.max(specWidth, option.getSpec().length()));
        }

        // print options help
        for (Entry<String, List<Option>> entry : categories.entrySet()) {
            String category = entry.getKey();
            pw.println("==== " + category);
            List<Option> options = entry.getValue();
            for (Option option : options) {
                if (option.isDeprecated() && !showDeprecated) {
                    continue;
                }
                // print option spec: option name + argument names
                String spec = option.getSpec();
                pw.print(" " + spec);
                int specLength = spec.length() + 1;
                pw.print(StringUtils.repeat(" ", specWidth - specLength));

                // print description
                StringBuffer desc = new StringBuffer((option.isDeprecated() ? "DEPRECATED: " : "")
                        + option.getDescription());
                int count = Math.min(desc.length(), width - Math.max(specLength, specWidth));
                // see if we have enough space to start on the same line as the spec
                if (count > MIN_DESC_WIDTH || desc.length() + specLength < width) {
                    pw.print(desc.substring(0, count));
                    desc.delete(0, count);
                }
                pw.println();

                // print remaining description
                while (desc.length() > 0) {
                    pw.print(StringUtils.repeat(" ", specWidth));
                    count = Math.min(desc.length(), width - specWidth);
                    pw.println(desc.substring(0, count));
                    desc.delete(0, count);
                }
            }
            pw.println();
        }
    }
}
