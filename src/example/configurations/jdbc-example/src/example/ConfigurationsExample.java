package example;

import java.io.IOException;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class ConfigurationsExample {
    
    public static void main(String[] args) {
        String jdbcPropToLoad = "prod.properties";
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("d", "dev", false, "Dev tag to launch app in dev mode. Means that app will launch embedded mckoi db.");
        try {
            CommandLine line = parser.parse( options, args );
            if(line.hasOption("d")) {
                System.err.println("App is in DEV mode");
                jdbcPropToLoad = "dev.properties";
            }
        }
        catch( ParseException exp ) {
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
        }
        Properties p = new Properties();
        try {
            p.load(ConfigurationsExample.class.getResourceAsStream("/"+jdbcPropToLoad));
        } catch (IOException e) {
            System.err.println( "Properties loading failed.  Reason: " + e.getMessage());
        }
        try {
            String clazz = p.getProperty("driver.class");
            Class.forName(clazz);
            System.out.println(" Jdbc driver loaded :"+clazz);
        } catch (ClassNotFoundException e) {
            System.err.println( "Jdbc Driver class loading failed.  Reason: " + e.getMessage());
            e.printStackTrace();
        }
        
    }
}
