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
package org.apache.ivy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.deliver.DeliverOptions;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.publish.PublishOptions;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.url.CredentialsStore;
import org.apache.ivy.util.url.URLHandler;
import org.apache.ivy.util.url.URLHandlerDispatcher;
import org.apache.ivy.util.url.URLHandlerRegistry;

/**
 * Class used to launch ivy as a standalone tool.
 * <p>
 * Valid arguments can be obtained with the -? argument. 
 */
public final class Main {
    private static final int DEPENDENCY_ARG_COUNT = 3;

    private static Options getOptions() {
        Option settings = OptionBuilder.withArgName("settingsfile").hasArg().withDescription(
            "use given file for settings").create("settings");
        Option conf = OptionBuilder.withArgName("settingsfile").hasArg().withDescription(
            "DEPRECATED - use given file for settings").create("conf");
        Option cache = OptionBuilder.withArgName("cachedir").hasArg().withDescription(
            "use given directory for cache").create("cache");
        Option ivyfile = OptionBuilder.withArgName("ivyfile").hasArg().withDescription(
            "use given file as ivy file").create("ivy");
        Option dependency = OptionBuilder
                .withArgName("organisation module revision")
                .hasArgs()
                .withDescription(
                    "use this instead of ivy file to do the rest "
                    + "of the work with this as a dependency.")
                .create("dependency");
        Option confs = OptionBuilder.withArgName("configurations").hasArgs().withDescription(
            "resolve given configurations").create("confs");
        Option retrieve = OptionBuilder.withArgName("retrievepattern").hasArg().withDescription(
            "use given pattern as retrieve pattern").create("retrieve");
        Option cachepath = OptionBuilder
                .withArgName("cachepathfile")
                .hasArg()
                .withDescription(
                    "outputs a classpath consisting of all dependencies in cache "
                    + "(including transitive ones) "
                    + "of the given ivy file to the given cachepathfile")
                .create("cachepath");
        Option revision = OptionBuilder.withArgName("revision").hasArg().withDescription(
            "use given revision to publish the module").create("revision");
        Option status = OptionBuilder.withArgName("status").hasArg().withDescription(
            "use given status to publish the module").create("status");
        Option deliver = OptionBuilder.withArgName("ivypattern").hasArg().withDescription(
            "use given pattern as resolved ivy file pattern").create("deliverto");
        Option publishResolver = OptionBuilder.withArgName("resolvername").hasArg()
                .withDescription("use given resolver to publish to").create("publish");
        Option publishPattern = OptionBuilder.withArgName("artpattern").hasArg().withDescription(
            "use given pattern to find artifacts to publish").create("publishpattern");
        Option realm = OptionBuilder.withArgName("realm").hasArg().withDescription(
            "use given realm for HTTP AUTH").create("realm");
        Option host = OptionBuilder.withArgName("host").hasArg().withDescription(
            "use given host for HTTP AUTH").create("host");
        Option username = OptionBuilder.withArgName("username").hasArg().withDescription(
            "use given username for HTTP AUTH").create("username");
        Option passwd = OptionBuilder.withArgName("passwd").hasArg().withDescription(
            "use given password for HTTP AUTH").create("passwd");
        Option main = OptionBuilder.withArgName("main").hasArg().withDescription(
            "the main class to runtime process").create("main");
        Option args = OptionBuilder.withArgName("args").hasArgs().withDescription(
            "the arguments to runtime process").create("args");
        Option cp = OptionBuilder.withArgName("cp").hasArg().withDescription(
            "extra classpath, used only in combination with option main").create("cp");

        Options options = new Options();

        options.addOption("debug", false, "set message level to debug");
        options.addOption("verbose", false, "set message level to verbose");
        options.addOption("warn", false, "set message level to warn");
        options.addOption("error", false, "set message level to error");
        options.addOption("novalidate", false, "do not validate ivy files against xsd");
        options.addOption("useOrigin", false,
            "DEPRECATED: use original artifact location "
            + "with local resolvers instead of copying to the cache");
        options.addOption("sync", false, "in conjonction with -retrieve, does a synced retrieve");
        options.addOption("m2compatible", false, "use maven2 compatibility");
        options.addOption("?", false, "display this help");
        options.addOption(conf);
        options.addOption(settings);
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
        options.addOption(cp);

        return options;
    }

    public static void main(String[] args) throws Exception {
        Options options = getOptions();

        CommandLineParser parser = new GnuParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("?")) {
                usage(options);
                return;
            }


            boolean validate = line.hasOption("novalidate") ? false : true;

            Ivy ivy = Ivy.newInstance();
            initMessage(line, ivy);
            IvySettings settings = initSettings(line, options, ivy);

            File cache = new File(settings.substitute(line.getOptionValue("cache", settings
                    .getDefaultCache().getAbsolutePath())));
            if (!cache.exists()) {
                cache.mkdirs();
            } else if (!cache.isDirectory()) {
                error(options, cache + " is not a directory");
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
                if (dep.length != DEPENDENCY_ARG_COUNT) {
                    error(options,
                        "dependency should be expressed with exactly 3 arguments: "
                        + "organisation module revision");
                }
                ivyfile = File.createTempFile("ivy", ".xml");
                ivyfile.deleteOnExit();
                DefaultModuleDescriptor md = DefaultModuleDescriptor
                        .newDefaultInstance(ModuleRevisionId.newInstance(dep[0],
                            dep[1] + "-caller", "working"));
                DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md,
                        ModuleRevisionId.newInstance(dep[0], dep[1], dep[2]), false, false, true);
                for (int i = 0; i < confs.length; i++) {
                    dd.addDependencyConfiguration("default", confs[i]);
                }
                md.addDependency(dd);
                XmlModuleDescriptorWriter.write(md, ivyfile);
                confs = new String[] {"default"};
            } else {
                ivyfile = new File(settings.substitute(line.getOptionValue("ivy", "ivy.xml")));
                if (!ivyfile.exists()) {
                    error(options, "ivy file not found: " + ivyfile);
                } else if (ivyfile.isDirectory()) {
                    error(options, "ivy file is not a file: " + ivyfile);
                }
            }

            if (line.hasOption("useOrigin")) {
                ivy.getSettings().useDeprecatedUseOrigin();
            }
            ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs)
                .setValidate(validate);
            ResolveReport report = ivy.resolve(ivyfile.toURL(), resolveOptions);
            if (report.hasError()) {
                System.exit(1);
            }
            ModuleDescriptor md = report.getModuleDescriptor();

            if (confs.length == 1 && "*".equals(confs[0])) {
                confs = md.getConfigurationsNames();
            }
            if (line.hasOption("retrieve")) {
                String retrievePattern = settings.substitute(line.getOptionValue("retrieve"));
                if (retrievePattern.indexOf("[") == -1) {
                    retrievePattern = retrievePattern + "/lib/[conf]/[artifact].[ext]";
                }
                ivy.retrieve(md.getModuleRevisionId(), retrievePattern, new RetrieveOptions()
                        .setConfs(confs).setSync(line.hasOption("sync"))
                        .setUseOrigin(line.hasOption("useOrigin")));
            }
            if (line.hasOption("cachepath")) {
                outputCachePath(ivy, cache, md, confs, line.getOptionValue("cachepath",
                    "ivycachepath.txt"));
            }

            if (line.hasOption("revision")) {
                ivy.deliver(md.getResolvedModuleRevisionId(), settings.substitute(line
                        .getOptionValue("revision")), settings.substitute(line.getOptionValue(
                    "deliverto", "ivy-[revision].xml")), DeliverOptions.newInstance(settings)
                        .setStatus(settings.substitute(line.getOptionValue("status", "release")))
                        .setValidate(validate));
                if (line.hasOption("publish")) {
                    ivy.publish(md.getResolvedModuleRevisionId(), Collections.singleton(settings
                            .substitute(line.getOptionValue("publishpattern",
                                "distrib/[type]s/[artifact]-[revision].[ext]"))), line
                            .getOptionValue("publish"), new PublishOptions()
                            .setPubrevision(settings.substitute(line.getOptionValue("revision")))
                            .setValidate(validate).setSrcIvyPattern(
                                settings.substitute(line.getOptionValue("deliverto",
                                    "ivy-[revision].xml"))));
                }
            }
            if (line.hasOption("main")) {
                // check if the option cp has been set
                List fileList = getExtraClasspathFileList(line);

                // merge -args and left over args
                String[] fargs = line.getOptionValues("args");
                if (fargs == null) {
                    fargs = new String[0];
                }
                String[] extra = line.getArgs();
                if (extra == null) {
                    extra = new String[0];
                }
                String[] params = new String[fargs.length + extra.length];
                System.arraycopy(fargs, 0, params, 0, fargs.length);
                System.arraycopy(extra, 0, params, fargs.length, extra.length);
                // invoke with given main class and merged params
                invoke(ivy, cache, md, confs, fileList, line.getOptionValue("main"), params);
            }
            ivy.getLoggerEngine().popLogger();
        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());

            usage(options);
        }
    }

    /**
     * Parses the <code>cp</code> option from the command line, and returns a list of {@link File}.
     * <p>
     * All the files contained in the returned List exist, non existing files are simply skipped
     * with a warning.
     * </p>
     * 
     * @param line
     *            the command line in which the cp option shold be parsed
     * @return a List of files to include as extra classpath entries, or <code>null</code> if no
     *         cp option was provided.
     */
    private static List/*<File>*/ getExtraClasspathFileList(CommandLine line) {
        List fileList = null;
        if (line.hasOption("cp")) {
            fileList = new ArrayList/*<File>*/();
            String[] cpArray = line.getOptionValues("cp");
            for (int index = 0; index < cpArray.length; index++) {
                StringTokenizer tokenizer = new StringTokenizer(cpArray[index], 
                    System.getProperty("path.separator"));
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    File file = new File(token);
                    if (file.exists()) {
                        fileList.add(file);
                    } else {
                        Message.warn(
                            "Skipping extra classpath '" + file + "' as it does not exist.");
                    }
                }
            }
        }
        return fileList;
    }

    private static IvySettings initSettings(CommandLine line, Options options, Ivy ivy) 
            throws java.text.ParseException, IOException {
        IvySettings settings = ivy.getSettings();
        settings.addAllVariables(System.getProperties());
        if (line.hasOption("m2compatible")) {
            settings.setVariable("ivy.default.configuration.m2compatible", "true");
        }

        configureURLHandler(line.getOptionValue("realm", null), line.getOptionValue("host",
            null), line.getOptionValue("username", null), line.getOptionValue("passwd", null));

        String settingsPath = line.getOptionValue("settings", "");
        if ("".equals(settingsPath)) {
            settingsPath = line.getOptionValue("conf", "");
            if (!"".equals(settingsPath)) {
                Message.deprecated("-conf is deprecated, use -settings instead");
            }
        }
        if ("".equals(settingsPath)) {
            ivy.configureDefault();
        } else {
            File conffile = new File(settingsPath);
            if (!conffile.exists()) {
                error(options, "ivy configuration file not found: " + conffile);
            } else if (conffile.isDirectory()) {
                error(options, "ivy configuration file is not a file: " + conffile);
            }
            ivy.configure(conffile);
        }
        return settings;
    }

    private static void initMessage(CommandLine line, Ivy ivy) {
        if (line.hasOption("debug")) {
            ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_DEBUG));
        } else if (line.hasOption("verbose")) {
            ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_VERBOSE));
        } else if (line.hasOption("warn")) {
            ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_WARN));
        } else if (line.hasOption("error")) {
            ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_ERR));
        } else {
            ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_INFO));
        }
    }

    private static void outputCachePath(Ivy ivy, File cache, ModuleDescriptor md, String[] confs,
            String outFile) {
        try {
            String pathSeparator = System.getProperty("path.separator");
            StringBuffer buf = new StringBuffer();
            Collection all = new LinkedHashSet();
            ResolutionCacheManager cacheMgr = ivy.getResolutionCacheManager();
            XmlReportParser parser = new XmlReportParser();
            for (int i = 0; i < confs.length; i++) {
                String resolveId = ResolveOptions.getDefaultResolveId(md);
                File report = cacheMgr.getConfigurationResolveReportInCache(resolveId, confs[i]);
                parser.parse(report);

                all.addAll(Arrays.asList(parser.getArtifactReports()));
            }
            for (Iterator iter = all.iterator(); iter.hasNext();) {
                ArtifactDownloadReport artifact = (ArtifactDownloadReport) iter.next();
                if (artifact.getLocalFile() != null) {
                    buf.append(artifact.getLocalFile().getCanonicalPath());
                    buf.append(pathSeparator);
                }
            }
            
            PrintWriter writer = new PrintWriter(new FileOutputStream(outFile));
            if (buf.length() > 0) {
                writer.println(buf.substring(0, buf.length() - pathSeparator.length()));
            }
            writer.close();
            System.out.println("cachepath output to " + outFile);

        } catch (Exception ex) {
            throw new RuntimeException(
                "impossible to build ivy cache path: " + ex.getMessage(), ex);
        }
    }

    private static void invoke(Ivy ivy, File cache, ModuleDescriptor md, String[] confs,
            List fileList, String mainclass, String[] args) {
        List urls = new ArrayList();

        // Add option cp (extra classpath) urls
        if (fileList != null && fileList.size() > 0) {
            for (Iterator iter = fileList.iterator(); iter.hasNext();) {
                File file = (File) iter.next();
                try {
                    urls.add(file.toURL());
                } catch (MalformedURLException e) {
                    // Should not happen, just ignore.
                } 
            }
        }
        
        try {
            Collection all = new LinkedHashSet();
            ResolutionCacheManager cacheMgr = ivy.getResolutionCacheManager();
            XmlReportParser parser = new XmlReportParser();
            for (int i = 0; i < confs.length; i++) {
                String resolveId = ResolveOptions.getDefaultResolveId(md);
                File report = cacheMgr.getConfigurationResolveReportInCache(resolveId, confs[i]);
                parser.parse(report);

                all.addAll(Arrays.asList(parser.getArtifactReports()));
            }
            for (Iterator iter = all.iterator(); iter.hasNext();) {
                ArtifactDownloadReport artifact = (ArtifactDownloadReport) iter.next();

                if (artifact.getLocalFile() != null) {
                    urls.add(artifact.getLocalFile().toURL());
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(
                "impossible to build ivy cache path: " + ex.getMessage(), ex);
        }

        URLClassLoader classLoader = new URLClassLoader((URL[]) urls.toArray(new URL[urls.size()]),
                Main.class.getClassLoader());

        try {
            Class c = classLoader.loadClass(mainclass);

            Method mainMethod = c.getMethod("main", new Class[] {String[].class});

            Thread.currentThread().setContextClassLoader(classLoader);
            mainMethod.invoke(null, new Object[] {(args == null ? new String[0] : args)});
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("Could not find class: " + mainclass, cnfe);
        } catch (SecurityException e) {
            throw new RuntimeException("Could not find main method: " + mainclass, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find main method: " + mainclass, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("No permissions to invoke main method: " + mainclass, e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(
                "Unexpected exception invoking main method: " + mainclass, e);
        }
    }

    private static void configureURLHandler(String realm, String host, String username,
            String passwd) {
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
        formatter.printHelp("ivy", options);
    }

    private Main() {
    }
}
