/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
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
public class Hello {
    public static void main(String[] args) throws Exception {
        Option msg = OptionBuilder.withArgName( "msg" )
        .hasArg()
        .withDescription(  "the message to capitalize" )
        .create( "message" );
        Options options = new Options();
        options.addOption(msg);
        
        CommandLineParser parser = new GnuParser();
        CommandLine line = parser.parse( options, args );
        
        String  message = line.getOptionValue("message", "hello ivy !");
        System.out.println("standard message : " + message);
        System.out.println("capitalized by " + WordUtils.class.getName() + " : " + WordUtils.capitalizeFully(message));
    }
}
