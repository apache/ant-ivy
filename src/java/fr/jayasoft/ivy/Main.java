/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.url.CredentialsStore;
import fr.jayasoft.ivy.url.URLHandler;
import fr.jayasoft.ivy.url.URLHandlerDispatcher;
import fr.jayasoft.ivy.url.URLHandlerRegistry;
import fr.jayasoft.ivy.util.DefaultMessageImpl;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorWriter;
import fr.jayasoft.ivy.xml.XmlReportParser;

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
        Option dependency = OptionBuilder.withArgName( "organisation module revision" )
            .hasArgs()
            .withDescription(  "use this instead of ivy file to do the rest of the work with this as a dependency." )
            .create( "dependency" );
        Option confs = OptionBuilder.withArgName( "configurations" )
            .hasArgs()
            .withDescription(  "resolve given configurations" )
            .create( "confs" );
        Option retrieve = OptionBuilder.withArgName( "retrievepattern" )
            .hasArg()
            .withDescription(  "use given pattern as retrieve pattern" )
            .create( "retrieve" );
        Option cachepath = OptionBuilder.withArgName( "cachepathfile" )
            .hasArg()
            .withDescription(  "outputs a classpath consisting of all dependencies in cache (including transitive ones) of the given ivy file to the given cachepathfile" )
            .create( "cachepath" );
        Option revision = OptionBuilder.withArgName( "revision" )
            .hasArg()
            .withDescription(  "use given revision to publish the module" )
            .create( "revision" );
        Option status = OptionBuilder.withArgName( "status" )
            .hasArg()
            .withDescription(  "use given status to publish the module" )
            .create( "status" );
        Option deliver = OptionBuilder.withArgName( "ivypattern" )
            .hasArg()
            .withDescription(  "use given pattern as resolved ivy file pattern" )
            .create( "deliverto" );
        Option publishResolver = OptionBuilder.withArgName( "resolvername" )
            .hasArg()
            .withDescription(  "use given resolver to publish to" )
            .create( "publish" );
        Option publishPattern = OptionBuilder.withArgName( "artpattern" )
            .hasArg()
            .withDescription(  "use given pattern to find artifacts to publish" )
            .create( "publishpattern" );
        Option realm = OptionBuilder.withArgName( "realm" )
            .hasArg()
            .withDescription(  "use given realm for HTTP AUTH" )
            .create( "realm" );
        Option host = OptionBuilder.withArgName( "host" )
            .hasArg()
            .withDescription(  "use given host for HTTP AUTH" )
            .create( "host" );
        Option username = OptionBuilder.withArgName( "username" )
            .hasArg()
            .withDescription(  "use given username for HTTP AUTH" )
            .create( "username" );
        Option passwd = OptionBuilder.withArgName( "passwd" )
            .hasArg()
            .withDescription(  "use given password for HTTP AUTH" )
            .create( "passwd" );
        Option main = OptionBuilder.withArgName("main")
	    	.hasArg()
	    	.withDescription("the main class to runtime process")
	    	.create("main");
	    Option args = OptionBuilder.withArgName("args")
	    	.hasArgs()
	    	.withDescription("the arguments to runtime process")
	    	.create("args");
        
        Options options = new Options();

        options.addOption("debug", false, "set message level to debug");
        options.addOption("verbose", false, "set message level to verbose");
        options.addOption("warn", false, "set message level to warn");
        options.addOption("error", false, "set message level to error");
        options.addOption("novalidate", false, "do not validate ivy files against xsd");
        options.addOption("useOrigin", false, "use original artifact location with local resolvers instead of copying to the cache");
        options.addOption("sync", false, "in conjonction with -retrieve, does a synced retrieve");
        options.addOption("m2compatible", false, "use maven2 compatibility");
        options.addOption("?", false, "display this help");
        options.addOption(conf);
        options.addOption(confs);
        options.addOption(cache);
        options.addOption(ivyfile);
        options.addOption(dependency);
        options.addOption(retrieve);
        options.addOption(cachepath);
        options.addOption(revision);
        options.addOption(status);
        options.addOption(deliver);
        options.addOption(publishResolver);
        options.addOption(publishPattern);
        options.addOption(realm);
        options.addOption(host);
        options.addOption(username);
        options.addOption(passwd);
        options.addOption(main);
        options.addOption(args);
        
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
            ivy.addAllVariables(System.getProperties());
            if (line.hasOption("m2compatible")) {
                ivy.setVariable("ivy.default.configuration.m2compatible", "true");
            }

            configureURLHandler(
                    line.getOptionValue("realm", null), 
                    line.getOptionValue("host", null), 
                    line.getOptionValue("username", null), 
                    line.getOptionValue("passwd", null));
            
            String confPath = line.getOptionValue("conf", "");
            if ("".equals(confPath)) {
                ivy.configureDefault();
            } else {
                File conffile = new File(confPath);
                if (!conffile.exists()) {
                    error(options, "ivy configuration file not found: "+conffile);
                } else if (conffile.isDirectory()) {
                    error(options, "ivy configuration file is not a file: "+conffile);
                }
                ivy.configure(conffile);
            }
            
            File cache = new File(ivy.substitute(line.getOptionValue("cache", ivy.getDefaultCache().getAbsolutePath())));
            if (!cache.exists()) {
                cache.mkdirs();
            } else if (!cache.isDirectory()) {
                error(options, cache+" is not a directory");
            }
            
            String[] confs;
            if (line.hasOption("confs")) {
                confs = line.getOptionValues("confs");
            } else {
                confs = new String[] {"*"};
            }

            File ivyfile;
            if (line.hasOption("dependency")) {
                String[] dep = line.getOptionValues("dependency");
                if (dep.length != 3) {
                    error(options, "dependency should be expressed with exactly 3 arguments: organisation module revision");
                }
                ivyfile = File.createTempFile("ivy", ".xml");
                ivyfile.deleteOnExit();
                DefaultModuleDescriptor md = DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(dep[0], dep[1]+"-caller", "working"));
                DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, ModuleRevisionId.newInstance(dep[0], dep[1], dep[2]), false, false, true);
                for (int i = 0; i < confs.length; i++) {
                    dd.addDependencyConfiguration("default", confs[i]);
                }
                md.addDependency(dd);
                XmlModuleDescriptorWriter.write(md, ivyfile);
                confs = new String[] {"default"};
            } else {
                ivyfile = new File(ivy.substitute(line.getOptionValue("ivy", "ivy.xml")));
                if (!ivyfile.exists()) {
                    error(options, "ivy file not found: "+ivyfile);
                } else if (ivyfile.isDirectory()) {
                    error(options, "ivy file is not a file: "+ivyfile);
                }
            }

            
            ResolveReport report = ivy.resolve(
                    ivyfile.toURL(),
                    null,
                    confs, 
                    cache, 
                    null,
                    validate,
                    false,
                    true,
                    line.hasOption("useOrigin"),
                    null
                    );
            if (report.hasError()) {
                System.exit(1);
            }
            ModuleDescriptor md = report.getModuleDescriptor();

            if (confs.length == 1 && "*".equals(confs[0])) {
                confs = md.getConfigurationsNames();
            }
            if (line.hasOption("retrieve")) {
                String retrievePattern = ivy.substitute(line.getOptionValue("retrieve"));
                if (retrievePattern.indexOf("[") == -1) {
                    retrievePattern = retrievePattern + "/lib/[conf]/[artifact].[ext]";
                }
                ivy.retrieve(md.getModuleRevisionId().getModuleId(), confs, cache, retrievePattern, null, null, line.hasOption("sync"), line.hasOption("useOrigin"));
            }
            if (line.hasOption("cachepath")) {
                outputCachePath(ivy, cache, md, confs, line.getOptionValue("cachepath", "ivycachepath.txt"));
            }

            if (line.hasOption("revision")) {
                ivy.deliver(
                    md.getResolvedModuleRevisionId(),
                    ivy.substitute(line.getOptionValue("revision")),
                    cache, 
                    ivy.substitute(line.getOptionValue("deliverto", "ivy-[revision].xml")),
                    ivy.substitute(line.getOptionValue("status", "release")),
                    null,
                    new DefaultPublishingDRResolver(),
                    validate);
                if (line.hasOption("publish")) {
                    ivy.publish(
                            md.getResolvedModuleRevisionId(), 
                            ivy.substitute(line.getOptionValue("revision")), 
                            cache, 
                            ivy.substitute(line.getOptionValue("publishpattern", "distrib/[type]s/[artifact]-[revision].[ext]")), 
                            line.getOptionValue("publish"), 
                            ivy.substitute(line.getOptionValue("deliverto", "ivy-[revision].xml")), 
                            validate);
                    
                }
            }
            if (line.hasOption("main")) {
                invoke(ivy, cache, md, confs, line.getOptionValue("main"),
                		line.getOptionValues("args"));
            }
        } catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            
            usage(options);
        }        
    }

    private static void outputCachePath(Ivy ivy, File cache, ModuleDescriptor md, String[] confs, String outFile) {
        try {
            String pathSeparator = System.getProperty("path.separator");
            StringBuffer buf = new StringBuffer(); 
            XmlReportParser parser = new XmlReportParser();
            Collection all = new LinkedHashSet();
            for (int i = 0; i < confs.length; i++) {
                Artifact[] artifacts = parser.getArtifacts(md.getModuleRevisionId().getModuleId(), confs[i], cache);
                all.addAll(Arrays.asList(artifacts));
            }
            for (Iterator iter = all.iterator(); iter.hasNext();) {
                Artifact artifact = (Artifact)iter.next();
                buf.append(ivy.getArchiveFileInCache(cache, artifact).getCanonicalPath());
                if (iter.hasNext()) {
                    buf.append(pathSeparator);
                }
            }
            PrintWriter writer = new PrintWriter(new FileOutputStream(outFile));
            writer.println(buf.toString());
            writer.close();
            System.out.println("cachepath output to "+outFile);

        } catch (Exception ex) {
            throw new RuntimeException("impossible to build ivy cache path: "+ex.getMessage(), ex);
        }
    }

    private static void invoke(Ivy ivy, File cache, ModuleDescriptor md, String[] confs, String mainclass, String[] args) {
    	List urls = new ArrayList();
    	
        try {
            XmlReportParser parser = new XmlReportParser();
            Collection all = new LinkedHashSet();
            for (int i = 0; i < confs.length; i++) {
                Artifact[] artifacts = parser.getArtifacts(md.getModuleRevisionId().getModuleId(), confs[i], cache);
                all.addAll(Arrays.asList(artifacts));
            }
            for (Iterator iter = all.iterator(); iter.hasNext();) {
                Artifact artifact = (Artifact)iter.next();
                
                urls.add(ivy.getArchiveFileInCache(cache, artifact).toURL());
            }
        } catch (Exception ex) {
            throw new RuntimeException("impossible to build ivy cache path: "+ex.getMessage(), ex);
        }
        
        URLClassLoader classLoader = new URLClassLoader(
        		(URL[]) urls.toArray(new URL[urls.size()]), 
        		Main.class.getClassLoader());
        
        try {
        	Class c = classLoader.loadClass(mainclass);
        	
        	Method mainMethod = c.getMethod("main", new Class[] { String[].class });
        	
        	// Split up arguments 
        	mainMethod.invoke(null, new Object[] { args });
        } catch (ClassNotFoundException cnfe) {
        	throw new RuntimeException("Could not find class: " + mainclass, cnfe);
        } catch (SecurityException e) {
        	throw new RuntimeException("Could not find main method: " + mainclass, e);
		} catch (NoSuchMethodException e) {
        	throw new RuntimeException("Could not find main method: " + mainclass, e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("No permissions to invoke main method: " + mainclass, e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Unexpected exception invoking main method: " + mainclass, e);
		}
    }
    private static void configureURLHandler(String realm, String host, String username, String passwd) {
    	CredentialsStore.INSTANCE.addCredentials(realm, host, username, passwd);

    	URLHandlerDispatcher dispatcher = new URLHandlerDispatcher();
        URLHandler httpHandler = URLHandlerRegistry.getHttp();
        dispatcher.setDownloader("http", httpHandler);
        dispatcher.setDownloader("https", httpHandler);
        URLHandlerRegistry.setDefault(dispatcher);
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
