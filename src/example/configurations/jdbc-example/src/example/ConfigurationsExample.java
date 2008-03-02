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

import java.io.IOException;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public final class ConfigurationsExample {
    
    public static void main(String[] args) {
        String jdbcPropToLoad = "prod.properties";
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("d", "dev", false, 
            "Dev tag to launch app in dev mode. Means that app will launch embedded mckoi db.");
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("d")) {
                System.err.println("App is in DEV mode");
                jdbcPropToLoad = "dev.properties";
            }
        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }
        Properties p = new Properties();
        try {
            p.load(ConfigurationsExample.class.getResourceAsStream("/" + jdbcPropToLoad));
        } catch (IOException e) {
            System.err.println("Properties loading failed.  Reason: " + e.getMessage());
        }
        try {
            String clazz = p.getProperty("driver.class");
            Class.forName(clazz);
            System.out.println(" Jdbc driver loaded :" + clazz);
        } catch (ClassNotFoundException e) {
            System.err.println("Jdbc Driver class loading failed.  Reason: " + e.getMessage());
            e.printStackTrace();
        }
        
    }
    
    private ConfigurationsExample() {
    }
}
