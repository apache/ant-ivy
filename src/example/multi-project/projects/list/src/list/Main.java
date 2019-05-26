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
package list;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public final class Main {
    private static Options getOptions() {
        Option dir = Option.builder("d")
            .longOpt("dir")
            .hasArg()
            .desc("list files in given dir")
            .build();
        Options options = new Options();

        options.addOption(dir);

        return options;
    }

    public static void main(String[] args) throws Exception {
      Options options = getOptions();
      try {

        CommandLineParser parser = new DefaultParser();

        CommandLine line = parser.parse(options, args);
        File dir = new File(line.getOptionValue("d", "."));
          System.out.println("listing files in " + dir);
          for (File file : ListFile.list(dir)) {
              System.out.println("\t" + file + "\n");
          }
      } catch (ParseException exp) {
          // oops, something went wrong
          System.err.println("Parsing failed.  Reason: " + exp.getMessage());

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("list", options);
      }
    }

    private Main() {
    }

}
