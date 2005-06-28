/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.File;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.jayasoft.ivy.util.DefaultMessageImpl;
import fr.jayasoft.ivy.util.Message;

/**
 * class used to launch ivy as a standalone tool
 * arguments are :
 * -conf <conffile> : indicates the path to the ivy configuration file
 *                  ivyconf.xml is assumed if not given
 * -cache <cachedir> : indicates the path to the cache directory
 *                   cache is assumed if not given
 * -ivy <ivyfile> : indicates the path to the ivy file to use
 *                  ivy.xml is assumed if not given
 * -retrieve <retrievepattern> : when used, retrieve is also done using the given retrievepattern
 * -revision <revision> : the revision with which the module should be published, required to publish
 * -status <status> :   the status with which the module should be published, 
 *                      release is assumed if not given
 * -publish <publishpattern> :  the pattern used to publish the resolved ivy file, 
 *                              ivy-[revision].xml is assumed if not given
 */
public class Main {
    private static Options getOptions() {
        Option conf = OptionBuilder.withArgName( "conffile" )
            .hasArg()
            .withDescription(  "use given file for configuration" )
            .create( "conf" );
        Option cache = OptionBuilder.withArgName( "cachedir" )
            .hasArg()
            .withDescription(  "use given directory for cache" )
            .create( "cache" );
        Option ivyfile = OptionBuilder.withArgName( "ivyfile" )
            .hasArg()
            .withDescription(  "use given file as ivy file" )
            .create( "ivy" );
        Option confs = OptionBuilder.withArgName( "configurations" )
            .hasArgs()
            .withDescription(  "resolve given configurations" )
            .create( "confs" );
        Option retrieve = OptionBuilder.withArgName( "retrievepattern" )
            .hasArg()
            .withDescription(  "use given pattern as retrieve pattern" )
            .create( "retrieve" );
        Option revision = OptionBuilder.withArgName( "revision" )
            .hasArg()
            .withDescription(  "use given revision to publish the module" )
            .create( "revision" );
        Option status = OptionBuilder.withArgName( "status" )
            .hasArg()
            .withDescription(  "use given status to publish the module" )
            .create( "status" );
        Option publish = OptionBuilder.withArgName( "publishpattern" )
            .hasArg()
            .withDescription(  "use given pattern as resolved ivy file pattern" )
            .create( "publish" );
        
        Options options = new Options();

        options.addOption("debug", false, "set message level to debug");
        options.addOption("verbose", false, "set message level to verbose");
        options.addOption("warn", false, "set message level to warn");
        options.addOption("error", false, "set message level to error");
        options.addOption("novalidate", false, "do not validate ivy files against xsd");
        options.addOption("?", false, "display this help");
        options.addOption(conf);
        options.addOption(confs);
        options.addOption(cache);
        options.addOption(ivyfile);
        options.addOption(retrieve);
        options.addOption(revision);
        options.addOption(status);
        options.addOption(publish);
        
        return options;
    }
    public static void main(String[] args) throws Exception {
        Options options = getOptions();
        
        CommandLineParser parser = new GnuParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );
            
            if (line.hasOption("?")) {
                usage(options);
                return;
            }
            
            if (line.hasOption("debug")) {
                Message.init(new DefaultMessageImpl(Message.MSG_DEBUG));
            } else if (line.hasOption("verbose")) {
                Message.init(new DefaultMessageImpl(Message.MSG_VERBOSE));
            } else if (line.hasOption("warn")) {
                Message.init(new DefaultMessageImpl(Message.MSG_WARN));
            } else if (line.hasOption("error")) {
                Message.init(new DefaultMessageImpl(Message.MSG_ERR));
            } else {
                Message.init(new DefaultMessageImpl(Message.MSG_INFO));
            }
            
            boolean validate = line.hasOption("novalidate")?false:true;
            
            Ivy ivy = new Ivy();

            String confPath = line.getOptionValue("conf", "");
            if ("".equals(confPath)) {
                ivy.configure(Ivy.class.getResource("ivyconf.xml"));
            } else {
                File conffile = new File(confPath);
                if (!conffile.exists()) {
                    error(options, "ivy configuration file not found: "+conffile);
                } else if (conffile.isDirectory()) {
                    error(options, "ivy configuration file is not a file: "+conffile);
                }
                ivy.configure(conffile);
            }
            
            File cache = new File(ivy.substitute(line.getOptionValue("cache", "cache")));
            if (!cache.exists()) {
                cache.mkdirs();
            } else if (!cache.isDirectory()) {
                error(options, cache+" is not a directory");
            }
            File ivyfile = new File(ivy.substitute(line.getOptionValue("ivy", "ivy.xml")));
            if (!ivyfile.exists()) {
                error(options, "ivy file not found: "+ivyfile);
            } else if (ivyfile.isDirectory()) {
                error(options, "ivy file is not a file: "+ivyfile);
            }
            String[] confs;
            if (line.hasOption("confs")) {
                confs = line.getOptionValues("confs");
            } else {
                confs = new String[] {"*"};
            }

            Date date = new Date();
            
            ModuleDescriptor md = ivy.resolve(
                    ivyfile.toURL(),
                    null,
                    confs, 
                    cache, 
                    date,
                    validate).getModuleDescriptor();

            if (confs.length == 1 && "*".equals(confs[0])) {
                confs = md.getConfigurationsNames();
            }
            if (line.hasOption("retrieve")) {
                String retrievePattern = ivy.substitute(line.getOptionValue("retrieve"));
                if (retrievePattern.indexOf("[") == -1) {
                    retrievePattern = retrievePattern + "/lib/[conf]/[artifact].[type]";
                }
                ivy.retrieve(md.getModuleRevisionId().getModuleId(), confs, cache, retrievePattern);
            }

            if (line.hasOption("revision")) {
                ivy.deliver(
                    md.getResolvedModuleRevisionId(),
                    ivy.substitute(line.getOptionValue("revision")),
                    cache, 
                    ivy.substitute(line.getOptionValue("publish", "ivy-[revision].xml")),
                    ivy.substitute(line.getOptionValue("status", "release")),
                    date,
                    new DefaultPublishingDRResolver(),
                    validate);
            }
        } catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            
            usage(options);
        }        
    }

    private static void error(Options options, String msg) {
        System.err.println(msg);
        usage(options);
        System.exit(1);
    }

    private static void usage(Options options) {
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "ivy", options );
    }

}
