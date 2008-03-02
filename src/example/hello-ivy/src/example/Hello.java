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
package example;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.WordUtils;

/**
 * Simple example to show how easy it is to retrieve transitive libs with ivy !!! 
 */
public final class Hello {
    public static void main(String[] args) throws Exception {
        Option msg = OptionBuilder.withArgName("msg")
            .hasArg()
            .withDescription("the message to capitalize")
            .create("message");
        Options options = new Options();
        options.addOption(msg);
        
        CommandLineParser parser = new GnuParser();
        CommandLine line = parser.parse(options, args);
        
        String  message = line.getOptionValue("message", "hello ivy !");
        System.out.println("standard message : " + message);
        System.out.println("capitalized by " + WordUtils.class.getName() 
            + " : " + WordUtils.capitalizeFully(message));
    }
    
    private Hello() {
    }
}
