package sizewhere;

import java.io.File;

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
            .withDescription(  "give total size of files in given dir" )
            .create( "dir" );
        Option name = OptionBuilder.withArgName( "name" )
            .hasArg()
            .withDescription(  "give total size of files with given name" )
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
        System.out.println("total size of files in "+dir+" containing "+name+": "+SizeWhere.totalSize(dir, name));
      } catch( ParseException exp ) {
          // oops, something went wrong
          System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
          
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "sizewhere", options );
      }        
    }
            
}
