package find;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {
    private static Options getOptions() {
        Option dir = OptionBuilder.withArgName( "dir" )
            .hasArg()
            .withDescription(  "list files in given dir" )
            .create( "dir" );
        Option name = OptionBuilder.withArgName( "name" )
            .hasArg()
            .withDescription(  "list files with given name" )
            .create( "name" );
        Options options = new Options();

        options.addOption(dir);
        options.addOption(name);
        
        return options;
    }
    
    public static void main(String[] args) throws Exception {
      Options options = getOptions();
      try {
        
        CommandLineParser parser = new GnuParser();

        CommandLine line = parser.parse( options, args );
        File dir = new File(line.getOptionValue("dir", "."));
        String name = line.getOptionValue("name", "jar");
        Collection files = FindFile.find(dir, name);
        System.out.println("listing files in "+dir+" containing "+name);
        for (Iterator it = files.iterator(); it.hasNext(); ) {
          System.out.println("\t"+it.next()+"\n");
        }
      } catch( ParseException exp ) {
          // oops, something went wrong
          System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
          
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "find", options );
      }        
    }
            
}
