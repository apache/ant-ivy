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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.LogOptions;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.check.CheckEngine;
import org.apache.ivy.core.deliver.DeliverEngine;
import org.apache.ivy.core.deliver.DeliverOptions;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.install.InstallEngine;
import org.apache.ivy.core.install.InstallOptions;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.publish.PublishEngine;
import org.apache.ivy.core.publish.PublishOptions;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.repository.RepositoryManagementEngine;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.retrieve.RetrieveEngine;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.retrieve.RetrieveReport;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.core.search.SearchEngine;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.core.sort.SortOptions;
import org.apache.ivy.plugins.circular.CircularDependencyException;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.plugins.repository.TransferListener;
import org.apache.ivy.plugins.resolver.BasicResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.ivy.util.DateUtil;
import org.apache.ivy.util.HostUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.MessageLoggerEngine;

/**
 * <a href="http://ant.apache.org/ivy/">Ivy</a> is a free java based dependency manager.
 * <p>
 * This class is the main class of Ivy, which acts as a Facade to all services offered by Ivy:
 * <ul>
 * <li>resolve dependencies</li>
 * <li>retrieve artifacts to a local location</li>
 * <li>deliver and publish modules</li>
 * <li>repository search and listing</li>
 * </ul>
 * Here is one typical usage:
 * 
 * <pre>
 * Ivy ivy = Ivy.newInstance();
 * ivy.configure(new URL(&quot;ivysettings.xml&quot;));
 * ivy.resolve(new URL(&quot;ivy.xml&quot;));
 * </pre>
 * 
 * </p>
 * <h2>Using Ivy engines directly</h2>
 * <p>
 * If the methods offered by the {@link Ivy} class are not flexible enough and you want to use Ivy
 * engines directly, you need to call the methods within a single {@link IvyContext} associated to
 * the {@link Ivy} instance you use.<br/>
 * To do so, it is recommended to use the {@link #execute(org.apache.ivy.Ivy.IvyCallback)} method
 * like this:
 * 
 * <pre>
 * Ivy ivy = Ivy.newInstance();
 * ivy.execute(new IvyCallback() {
 *     public Object doInIvyContext(Ivy ivy, IvyContext context) {
 *         // obviously we can use regular Ivy methods in the callback
 *         ivy.configure(new URL(&quot;ivysettings.xml&quot;));
 *         // and we can safely use Ivy engines too
 *         ivy.getResolveEngine().resolve(new URL(&quot;ivy.xml&quot;));
 *         return null;
 *     }
 * });
 * </pre>
 * 
 * </p>
 */
public class Ivy {
    /**
     * Callback used to execute a set of Ivy related methods within an {@link IvyContext}.
     * 
     * @see Ivy#execute(org.apache.ivy.Ivy.IvyCallback)
     */
    public static interface IvyCallback {
        /**
         * Executes Ivy related job within an {@link IvyContext}
         * 
         * @param ivy
         *            the {@link Ivy} instance to which this callback is related
         * @param context
         *            the {@link IvyContext} in which this callback is executed
         * @return the result of this job, <code>null</code> if there is no result
         */
        public Object doInIvyContext(Ivy ivy, IvyContext context);
    }

    private static final int KILO = 1024;

    /**
     * @deprecated Use the {@link DateUtil} utility class instead.
     */
    @Deprecated
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
            DateUtil.DATE_FORMAT_PATTERN);

    /**
     * the current version of Ivy, as displayed on the console when Ivy is initialized
     */
    private static final String IVY_VERSION;

    /**
     * the date at which this version of Ivy has been built. May be empty if unknown.
     */
    private static final String IVY_DATE;

    static {
        // initialize IVY_VERSION and IVY_DATE
        Properties props = new Properties();
        URL moduleURL = Message.class.getResource("/module.properties");
        if (moduleURL != null) {
            try {
                InputStream module = moduleURL.openStream();
                props.load(module);
                module.close();
            } catch (IOException e) {
                // ignore this exception, we will initialize with default values
            }
        }
        IVY_VERSION = props.getProperty("version", "non official version");
        IVY_DATE = props.getProperty("date", "");
    }

    /**
     * Returns the current version of Ivy, as displayed on the console when Ivy is initialized.
     * 
     * @return the current version of Ivy
     */
    public static String getIvyVersion() {
        return IVY_VERSION;
    }

    /**
     * Returns the date at which this version of Ivy has been built.
     * <p>
     * May be empty if unknown.
     * 
     * @return the date at which this version of Ivy has been built
     */
    public static String getIvyDate() {
        return IVY_DATE;
    }

    /**
     * Returns the URL at which Ivy web site can be found.
     * 
     * @return the URL at which Ivy web site can be found
     */
    public static String getIvyHomeURL() {
        return "http://ant.apache.org/ivy/";
    }

    public static Ivy newInstance() {
        Ivy ivy = new Ivy();
        ivy.bind();
        return ivy;
    }

    public static Ivy newInstance(IvySettings settings) {
        Ivy ivy = new Ivy();
        ivy.setSettings(settings);
        ivy.bind();
        return ivy;
    }

    /**
     * True if the current processing has been requested to be interrupted, false otherwise
     */
    private boolean interrupted;

    /**
     * True if this instance of Ivy has already been bound to its dependencies, false otherwise.
     * 
     * @see bind()
     */
    private boolean bound;

    /*
     * Following are dependencies of the Ivy instance on instances of engines and manager which
     * actually do the work The attributes can be set either manually using the corresponding
     * setters, or all at once with the default implementations using the bind method
     */
    private IvySettings settings;

    private EventManager eventManager;

    private SortEngine sortEngine;

    private SearchEngine searchEngine;

    private CheckEngine checkEngine;

    private ResolveEngine resolveEngine;

    private RetrieveEngine retrieveEngine;

    private DeliverEngine deliverEngine;

    private PublishEngine publishEngine;

    private InstallEngine installEngine;

    private RepositoryManagementEngine repositoryEngine;

    /**
     * The logger engine to use to log messages when using this Ivy instance.
     */
    private MessageLoggerEngine loggerEngine = new MessageLoggerEngine();

    /**
     * The default constructor of Ivy allows to create an instance of Ivy with none of its
     * dependencies (engines, settings, ...) created. If you use this constructor, it's your
     * responsibility to set the dependencies of Ivy using the appropriate setters
     * (setResolveEngine, ...). You can also call the bind method to set all the dependencies except
     * those that you have provided using the setters. If you want to get an instance ready to use,
     * prefer the use of Ivy.newInstance().
     */
    public Ivy() {
    }

    /**
     * This method is used to bind this Ivy instance to required dependencies, i.e. instance of
     * settings, engines, and so on.
     * <p>
     * After this call Ivy is still not configured, which means that the settings object is still
     * empty.
     * </p>
     */
    public void bind() {
        pushContext();
        try {
            if (settings == null) {
                settings = new IvySettings();
            }
            if (eventManager == null) {
                eventManager = new EventManager();
            }
            if (sortEngine == null) {
                sortEngine = new SortEngine(settings);
            }
            if (searchEngine == null) {
                searchEngine = new SearchEngine(settings);
            }
            if (resolveEngine == null) {
                resolveEngine = new ResolveEngine(settings, eventManager, sortEngine);
            }
            if (retrieveEngine == null) {
                retrieveEngine = new RetrieveEngine(settings, eventManager);
            }
            if (deliverEngine == null) {
                deliverEngine = new DeliverEngine(settings);
            }
            if (publishEngine == null) {
                publishEngine = new PublishEngine(settings, eventManager);
            }
            if (installEngine == null) {
                installEngine = new InstallEngine(settings, searchEngine, resolveEngine);
            }
            if (repositoryEngine == null) {
                repositoryEngine = new RepositoryManagementEngine(settings, searchEngine,
                        resolveEngine);
            }

            eventManager.addTransferListener(new TransferListener() {
                public void transferProgress(TransferEvent evt) {
                    ResolveData resolve;
                    switch (evt.getEventType()) {
                        case TransferEvent.TRANSFER_PROGRESS:
                            resolve = IvyContext.getContext().getResolveData();
                            if (resolve == null
                                    || !LogOptions.LOG_QUIET.equals(resolve.getOptions().getLog())) {
                                Message.progress();
                            }
                            break;
                        case TransferEvent.TRANSFER_COMPLETED:
                            resolve = IvyContext.getContext().getResolveData();
                            if (resolve == null
                                    || !LogOptions.LOG_QUIET.equals(resolve.getOptions().getLog())) {
                                Message.endProgress(" (" + (evt.getTotalLength() / KILO) + "kB)");
                            }
                            break;
                        default:
                            break;
                    }
                }
            });

            bound = true;
        } finally {
            popContext();
        }
    }

    /**
     * Executes the given callback in the context of this Ivy instance.
     * <p>
     * Alternatively you can use the {@link #pushContext()} and {@link #popContext()} methods, but
     * this is not recommended:
     * 
     * <pre>
     * Object result = null;
     * pushContext();
     * try {
     *     result = callback.doInIvyContext(this, IvyContext.getContext());
     * } finally {
     *     popContext();
     * }
     * doSomethingWithResult(result);
     * </pre>
     * 
     * </p>
     * 
     * @param callback
     * @return
     */
    public Object execute(IvyCallback callback) {
        pushContext();
        try {
            return callback.doInIvyContext(this, IvyContext.getContext());
        } finally {
            popContext();
        }
    }

    /**
     * Pushes a new IvyContext bound to this Ivy instance if the current context is not already
     * bound to this Ivy instance. If the current context is already bound to this Ivy instance, it
     * pushes the current context on the context stack, so that you can (and must) always call
     * {@link #popContext()} when you're done.
     * <p>
     * Alternatively, you can use the {@link #execute(org.apache.ivy.Ivy.IvyCallback)} method which
     * takes care of everything for you.
     * </p>
     */
    public void pushContext() {
        if (IvyContext.getContext().peekIvy() != this) {
            // the current Ivy context is associated with another Ivy instance, we push a new
            // instance
            IvyContext.pushNewContext();
            IvyContext.getContext().setIvy(this);
        } else {
            // the current Ivy context is already associated with this Ivy instance, we only push it
            // for popping consistency
            IvyContext.pushContext(IvyContext.getContext());
        }
    }

    /**
     * Pops the current Ivy context.
     * <p>
     * You must call this method once and only once for each call to {@link #pushContext()}, when
     * you're done with the your Ivy related work.
     * </p>
     * <p>
     * Alternatively, you can use the {@link #execute(org.apache.ivy.Ivy.IvyCallback)} method which
     * takes care of everything for you.
     * </p>
     */
    public void popContext() {
        IvyContext.popContext();
    }

    // ///////////////////////////////////////////////////////////////////////
    // LOAD SETTINGS
    // ///////////////////////////////////////////////////////////////////////
    public void configure(File settingsFile) throws ParseException, IOException {
        pushContext();
        try {
            assertBound();
            settings.load(settingsFile);
            postConfigure();
        } finally {
            popContext();
        }
    }

    public void configure(URL settingsURL) throws ParseException, IOException {
        pushContext();
        try {
            assertBound();
            settings.load(settingsURL);
            postConfigure();
        } finally {
            popContext();
        }
    }

    public void configureDefault() throws ParseException, IOException {
        pushContext();
        try {
            assertBound();
            settings.loadDefault();
            postConfigure();
        } finally {
            popContext();
        }
    }

    /**
     * Configures Ivy with 1.4 compatible default settings
     */
    public void configureDefault14() throws ParseException, IOException {
        pushContext();
        try {
            assertBound();
            settings.loadDefault14();
            postConfigure();
        } finally {
            popContext();
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // CHECK
    // ///////////////////////////////////////////////////////////////////////
    public boolean check(URL ivyFile, String resolvername) {
        pushContext();
        try {
            return checkEngine.check(ivyFile, resolvername);
        } finally {
            popContext();
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // RESOLVE
    // ///////////////////////////////////////////////////////////////////////

    public ResolveReport resolve(File ivySource) throws ParseException, IOException {
        pushContext();
        try {
            return resolveEngine.resolve(ivySource);
        } finally {
            popContext();
        }
    }

    public ResolveReport resolve(URL ivySource) throws ParseException, IOException {
        pushContext();
        try {
            return resolveEngine.resolve(ivySource);
        } finally {
            popContext();
        }
    }

    public ResolveReport resolve(ModuleRevisionId mrid, ResolveOptions options, boolean changing)
            throws ParseException, IOException {
        pushContext();
        try {
            return resolveEngine.resolve(mrid, options, changing);
        } finally {
            popContext();
        }
    }

    public ResolveReport resolve(URL ivySource, ResolveOptions options) throws ParseException,
            IOException {
        pushContext();
        try {
            return resolveEngine.resolve(ivySource, options);
        } finally {
            popContext();
        }
    }

    public ResolveReport resolve(File ivySource, ResolveOptions options) throws ParseException,
            IOException {
        return resolve(ivySource.toURI().toURL(), options);
    }

    public ResolveReport resolve(ModuleDescriptor md, ResolveOptions options)
            throws ParseException, IOException {
        pushContext();
        try {
            return resolveEngine.resolve(md, options);
        } finally {
            popContext();
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // INSTALL
    // ///////////////////////////////////////////////////////////////////////

    public ResolveReport install(ModuleRevisionId mrid, String from, String to,
            InstallOptions options) throws IOException {
        pushContext();
        try {
            return installEngine.install(mrid, from, to, options);
        } finally {
            popContext();
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // RETRIEVE
    // ///////////////////////////////////////////////////////////////////////

    public int retrieve(ModuleRevisionId mrid, String destFilePattern, RetrieveOptions options)
            throws IOException {
        pushContext();
        try {
            return retrieveEngine.retrieve(mrid, destFilePattern, options);
        } finally {
            popContext();
        }
    }

    public RetrieveReport retrieve(ModuleRevisionId mrid, RetrieveOptions options)
            throws IOException {
        pushContext();
        try {
            return retrieveEngine.retrieve(mrid, options);
        } finally {
            popContext();
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // DELIVER
    // ///////////////////////////////////////////////////////////////////////

    public void deliver(ModuleRevisionId mrid, String revision, String destIvyPattern)
            throws IOException, ParseException {
        pushContext();
        try {
            deliverEngine.deliver(mrid, revision, destIvyPattern,
                DeliverOptions.newInstance(settings));
        } finally {
            popContext();
        }
    }

    public void deliver(String revision, String destIvyPattern, DeliverOptions options)
            throws IOException, ParseException {
        pushContext();
        try {
            deliverEngine.deliver(revision, destIvyPattern, options);
        } finally {
            popContext();
        }
    }

    /**
     * Example of use: deliver(mrid, "1.5", "target/ivy/ivy-[revision].xml",
     * DeliverOptions.newInstance(settings).setStatus("release").setValidate(false));
     * 
     * @param mrid
     * @param revision
     * @param destIvyPattern
     * @param options
     * @throws IOException
     * @throws ParseException
     */
    public void deliver(ModuleRevisionId mrid, String revision, String destIvyPattern,
            DeliverOptions options) throws IOException, ParseException {
        pushContext();
        try {
            deliverEngine.deliver(mrid, revision, destIvyPattern, options);
        } finally {
            popContext();
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // PUBLISH
    // ///////////////////////////////////////////////////////////////////////

    public Collection<Artifact> publish(ModuleRevisionId mrid,
            Collection<String> srcArtifactPattern, String resolverName, PublishOptions options)
            throws IOException {
        pushContext();
        try {
            return publishEngine.publish(mrid, srcArtifactPattern, resolverName, options);
        } finally {
            popContext();
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // SORT
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Sorts the collection of IvyNode from the less dependent to the more dependent
     */
    public List<IvyNode> sortNodes(Collection<IvyNode> nodes, SortOptions options) {
        pushContext();
        try {
            return getSortEngine().sortNodes(nodes, options);
        } finally {
            popContext();
        }
    }

    /**
     * Sorts the given ModuleDescriptors from the less dependent to the more dependent. This sort
     * ensures that a ModuleDescriptor is always found in the list before all ModuleDescriptors
     * depending directly on it.
     * 
     * @param moduleDescriptors
     *            a Collection of ModuleDescriptor to sort
     * @param options
     *            Options to use to sort the descriptors.
     * @return a List of sorted ModuleDescriptors
     * @throws CircularDependencyException
     *             if a circular dependency exists and circular dependency strategy decide to throw
     *             an exception
     */
    public List<ModuleDescriptor> sortModuleDescriptors(
            Collection<ModuleDescriptor> moduleDescriptors, SortOptions options) {
        pushContext();
        try {
            return getSortEngine().sortModuleDescriptors(moduleDescriptors, options);
        } finally {
            popContext();
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // SEARCH
    // ///////////////////////////////////////////////////////////////////////

    public ResolvedModuleRevision findModule(ModuleRevisionId mrid) {
        pushContext();
        try {
            ResolveOptions options = new ResolveOptions();
            options.setValidate(false);
            return resolveEngine.findModule(mrid, options);
        } finally {
            popContext();
        }
    }

    public ModuleEntry[] listModuleEntries(OrganisationEntry org) {
        pushContext();
        try {
            return searchEngine.listModuleEntries(org);
        } finally {
            popContext();
        }
    }

    public ModuleId[] listModules(ModuleId criteria, PatternMatcher matcher) {
        pushContext();
        try {
            return searchEngine.listModules(criteria, matcher);
        } finally {
            popContext();
        }
    }

    public ModuleRevisionId[] listModules(ModuleRevisionId criteria, PatternMatcher matcher) {
        pushContext();
        try {
            return searchEngine.listModules(criteria, matcher);
        } finally {
            popContext();
        }
    }

    public String[] listModules(String org) {
        pushContext();
        try {
            return searchEngine.listModules(org);
        } finally {
            popContext();
        }
    }

    public OrganisationEntry[] listOrganisationEntries() {
        pushContext();
        try {
            return searchEngine.listOrganisationEntries();
        } finally {
            popContext();
        }
    }

    public String[] listOrganisations() {
        pushContext();
        try {
            return searchEngine.listOrganisations();
        } finally {
            popContext();
        }
    }

    public RevisionEntry[] listRevisionEntries(ModuleEntry module) {
        pushContext();
        try {
            return searchEngine.listRevisionEntries(module);
        } finally {
            popContext();
        }
    }

    public String[] listRevisions(String org, String module) {
        pushContext();
        try {
            return searchEngine.listRevisions(org, module);
        } finally {
            popContext();
        }
    }

    public String[] listTokenValues(String token, Map<String, Object> otherTokenValues) {
        pushContext();
        try {
            return searchEngine.listTokenValues(token, otherTokenValues);
        } finally {
            popContext();
        }
    }

    // ///////////////////////////////////////////////////////////////////////
    // INTERRUPTIONS
    // ///////////////////////////////////////////////////////////////////////

    /**
     * Interrupts the current running operation, no later than interruptTimeout milliseconds after
     * the call
     */
    public void interrupt() {
        Thread operatingThread = IvyContext.getContext().getOperatingThread();
        interrupt(operatingThread);
    }

    /**
     * Interrupts the current running operation in the given operating thread, no later than
     * interruptTimeout milliseconds after the call
     */
    public void interrupt(Thread operatingThread) {
        if (operatingThread != null && operatingThread.isAlive()) {
            if (operatingThread == Thread.currentThread()) {
                throw new IllegalStateException("cannot call interrupt from ivy operating thread");
            }
            Message.verbose("interrupting operating thread...");
            operatingThread.interrupt();
            synchronized (this) {
                interrupted = true;
            }
            try {
                Message.verbose("waiting clean interruption of operating thread");
                operatingThread.join(settings.getInterruptTimeout());
            } catch (InterruptedException e) {
                // reset thread interrupt status
                Thread.currentThread().interrupt();
            }
            if (operatingThread.isAlive()) {
                Message.warn("waited clean interruption for too long: stopping operating thread");
                operatingThread.stop();
            }
            synchronized (this) {
                interrupted = false;
            }
        }
    }

    public synchronized boolean isInterrupted() {
        return interrupted;
    }

    /**
     * Check if the current operation has been interrupted, and if it is the case, throw a runtime
     * exception
     */
    public void checkInterrupted() {
        if (isInterrupted()) {
            Message.info("operation interrupted");
            throw new RuntimeException("operation interrupted");
        }
    }

    public static String getWorkingRevision() {
        return "working@" + HostUtil.getLocalHostName();
    }

    public ResolutionCacheManager getResolutionCacheManager() {
        return settings.getResolutionCacheManager();
    }

    private void assertBound() {
        if (!bound) {
            bind();
        }
    }

    private void postConfigure() {
        List<Trigger> triggers = settings.getTriggers();
        for (Trigger trigger : triggers) {
            eventManager.addIvyListener(trigger, trigger.getEventFilter());
        }

        for (DependencyResolver resolver : settings.getResolvers()) {
            if (resolver instanceof BasicResolver) {
                ((BasicResolver) resolver).setEventManager(eventManager);
            }
        }
    }

    public String getVariable(String name) {
        pushContext();
        try {
            assertBound();
            return settings.getVariable(name);
        } finally {
            popContext();
        }
    }

    public String substitute(String str) {
        pushContext();
        try {
            assertBound();
            return settings.substitute(str);
        } finally {
            popContext();
        }
    }

    public void setVariable(String varName, String value) {
        pushContext();
        try {
            assertBound();
            settings.setVariable(varName, value);
        } finally {
            popContext();
        }
    }

    // ///////////////////////////////////////////////////////////////////
    // GETTERS / SETTERS
    // ///////////////////////////////////////////////////////////////////

    public IvySettings getSettings() {
        return settings;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public CheckEngine getCheckEngine() {
        return checkEngine;
    }

    public void setCheckEngine(CheckEngine checkEngine) {
        this.checkEngine = checkEngine;
    }

    public DeliverEngine getDeliverEngine() {
        return deliverEngine;
    }

    public void setDeliverEngine(DeliverEngine deliverEngine) {
        this.deliverEngine = deliverEngine;
    }

    public InstallEngine getInstallEngine() {
        return installEngine;
    }

    public void setInstallEngine(InstallEngine installEngine) {
        this.installEngine = installEngine;
    }

    public PublishEngine getPublishEngine() {
        return publishEngine;
    }

    public void setPublishEngine(PublishEngine publishEngine) {
        this.publishEngine = publishEngine;
    }

    public ResolveEngine getResolveEngine() {
        return resolveEngine;
    }

    public void setResolveEngine(ResolveEngine resolveEngine) {
        this.resolveEngine = resolveEngine;
    }

    public RetrieveEngine getRetrieveEngine() {
        return retrieveEngine;
    }

    public void setRetrieveEngine(RetrieveEngine retrieveEngine) {
        this.retrieveEngine = retrieveEngine;
    }

    public SearchEngine getSearchEngine() {
        return searchEngine;
    }

    public void setSearchEngine(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    public SortEngine getSortEngine() {
        return sortEngine;
    }

    public void setSortEngine(SortEngine sortEngine) {
        this.sortEngine = sortEngine;
    }

    public RepositoryManagementEngine getRepositoryEngine() {
        return repositoryEngine;
    }

    public void setRepositoryEngine(RepositoryManagementEngine repositoryEngine) {
        this.repositoryEngine = repositoryEngine;
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void setSettings(IvySettings settings) {
        this.settings = settings;
    }

    public MessageLoggerEngine getLoggerEngine() {
        return loggerEngine;
    }
}
