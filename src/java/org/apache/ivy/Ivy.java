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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.check.CheckEngine;
import org.apache.ivy.core.deliver.DeliverEngine;
import org.apache.ivy.core.deliver.DeliverOptions;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.install.InstallEngine;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.publish.PublishEngine;
import org.apache.ivy.core.publish.PublishOptions;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.retrieve.RetrieveEngine;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.core.search.SearchEngine;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.sort.SortEngine;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.plugins.repository.TransferListener;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.ivy.util.HostUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;


/**
 * <a href="http://incubator.apache.org/ivy/">Ivy</a> is a free java based dependency manager.
 * 
 * This class is the main class of Ivy, which acts as a Facade to all services offered by Ivy:
 * <ul>
 * <li>resolve dependencies</li>
 * <li>retrieve artifacts to a local location</li>
 * <li>deliver and publish modules</li>
 * <li>repository search and listing</li>
 * </li>
 * 
 * Here is one typical usage:
 * Ivy ivy = Ivy.newInstance();
 * ivy.configure(new URL("ivysettings.xml"));
 * ivy.resolve(new URL("ivy.xml"));
 *  
 * @author Xavier Hanin
 */
public class Ivy {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

	public static Ivy newInstance() {
		Ivy ivy = new Ivy();
		ivy.bind();
		return ivy;
	}

	/**
	 * True if the current processing has been requested to be interrupted,
	 * false otherwise
	 */
    private boolean _interrupted;
    /**
     * True if this instance of Ivy hass already been bound to its dependencies,
     * false otherwise.
     * @see bind()
     */
    private boolean _bound;

    /* 
     * Following are dependencies of the Ivy instance on
     * instances of engines and manager which actually do the work
     * 
     * The attributes can be set either manually using the corresponding setters,
     * or all at once with the default implementations using the bind method
     */
	private IvySettings _settings;

	private EventManager _eventManager;
	private SortEngine _sortEngine;
	private SearchEngine _searchEngine;
	private CheckEngine _checkEngine;
	private ResolveEngine _resolveEngine;
	private RetrieveEngine _retrieveEngine;
	private DeliverEngine _deliverEngine;
	private PublishEngine _publishEngine;
	private InstallEngine _installEngine;
    
	/**
	 * The default constructor of Ivy allows to create an instance of Ivy with none of 
	 * its dependencies (engines, settings, ...) created.
	 * If you use this constructor, it's your responsibility to set the dependencies
	 * of Ivy using the appropriate setters (setResolveEngine, ...).
	 * You can also call the bind method to set all the dependencies except those that you 
	 * have provided using the setters.
	 * 
	 * If you want to get an instance ready to use, prefer the use of Ivy.newInstance().
	 */
    public Ivy() {
    }
    
	/**
	 * This method is used to bind this Ivy instance to 
	 * required dependencies, i.e. instance of settings, engines, and so on.
	 * After thes call Ivy is still not configured, which means that the settings
	 * object is still empty.
	 */
	public void bind() {
        IvyContext.getContext().setIvy(this);
        if (_settings == null) {
        	_settings = new IvySettings();
        }
        if (_eventManager == null) {
        	_eventManager = new EventManager();
        }
        if (_sortEngine == null) {
        	_sortEngine = new SortEngine(_settings);
        }
        if (_searchEngine == null) {
        	_searchEngine = new SearchEngine(_settings);
        }
        if (_resolveEngine == null) {
        	_resolveEngine = new ResolveEngine(_settings, _eventManager, _sortEngine);
        }
        if (_retrieveEngine == null) {
        	_retrieveEngine = new RetrieveEngine(_settings);
        }
        if (_deliverEngine == null) {
        	_deliverEngine = new DeliverEngine(_settings);
        }
        if (_publishEngine == null) {
        	_publishEngine = new PublishEngine(_settings);
        }
        if (_installEngine == null) {
        	_installEngine = new InstallEngine(_settings, _searchEngine, _resolveEngine, _publishEngine);
        }
		
		_eventManager.addTransferListener(new TransferListener() {
            public void transferProgress(TransferEvent evt) {
                switch (evt.getEventType()) {
                case TransferEvent.TRANSFER_PROGRESS:
                    Message.progress();
                    break;
                case TransferEvent.TRANSFER_COMPLETED:
                    Message.endProgress(" ("+(evt.getTotalLength() / 1024)+"kB)");
                    break;
                default:
                    break;
                }
            }
        });

		_bound = true;
	}

	/////////////////////////////////////////////////////////////////////////
    //                         LOAD SETTINGS
    /////////////////////////////////////////////////////////////////////////
	public void configure(File settingsFile) throws ParseException, IOException {
		assertBound();
		_settings.load(settingsFile);
		postConfigure();
	}

	public void configure(URL settingsURL) throws ParseException, IOException {
		assertBound();
		_settings.load(settingsURL);
		postConfigure();
	}

	public void configureDefault() throws ParseException, IOException {
		assertBound();
		_settings.loadDefault();
		postConfigure();
	}
	
	/**
	 * Configures Ivy with 1.4 compatible default settings
	 */
	public void configureDefault14() throws ParseException, IOException {
		assertBound();
		_settings.loadDefault14();
		postConfigure();
	}
	
	/////////////////////////////////////////////////////////////////////////
    //                         CHECK
    /////////////////////////////////////////////////////////////////////////
	public boolean check(URL ivyFile, String resolvername) {
		return _checkEngine.check(ivyFile, resolvername);
	}

    

	/////////////////////////////////////////////////////////////////////////
    //                         RESOLVE
    /////////////////////////////////////////////////////////////////////////

	public ResolveReport resolve(File ivySource) throws ParseException, IOException {
		return _resolveEngine.resolve(ivySource);
	}

	public ResolveReport resolve(URL ivySource) throws ParseException, IOException {
		return _resolveEngine.resolve(ivySource);
	}

	public ResolveReport resolve(ModuleRevisionId mrid, ResolveOptions options, boolean changing) throws ParseException, IOException {
		return _resolveEngine.resolve(mrid, options, changing);
	}

	public ResolveReport resolve(URL ivySource, ResolveOptions options) throws ParseException, IOException {
		return _resolveEngine.resolve(ivySource, options);
	}

	public ResolveReport resolve(ModuleDescriptor md, ResolveOptions options) throws ParseException, IOException, FileNotFoundException {
		return _resolveEngine.resolve(md, options);
	}

    /////////////////////////////////////////////////////////////////////////
    //                         INSTALL
    /////////////////////////////////////////////////////////////////////////
    
    
    public ResolveReport install(ModuleRevisionId mrid, String from, String to, boolean transitive, boolean validate, boolean overwrite, Filter artifactFilter, File cache, String matcherName) throws IOException {
		return _installEngine.install(mrid, from, to, transitive, validate, overwrite, artifactFilter, cache, matcherName);
	}

    /////////////////////////////////////////////////////////////////////////
    //                         RETRIEVE
    /////////////////////////////////////////////////////////////////////////

	public int retrieve(ModuleRevisionId mrid, String destFilePattern, RetrieveOptions options) throws IOException {
		return _retrieveEngine.retrieve(mrid, destFilePattern, options);
	}

    /////////////////////////////////////////////////////////////////////////
    //                         DELIVER
    /////////////////////////////////////////////////////////////////////////

	public void deliver(ModuleRevisionId mrid, String revision,	String destIvyPattern) throws IOException, ParseException {
		_deliverEngine.deliver(mrid, revision, destIvyPattern, DeliverOptions.newInstance(_settings));
	}

	public void deliver(String revision, String destIvyPattern, DeliverOptions options) throws IOException, ParseException {
		_deliverEngine.deliver(revision, destIvyPattern, options);
	}

	/**
	 * Example of use:
	 * deliver(mrid, "1.5", "target/ivy/ivy-[revision].xml", 
	 *         DeliverOptions.newInstance(settings).setStatus("release").setValidate(false));
	 *         
	 * @param mrid
	 * @param revision
	 * @param destIvyPattern
	 * @param options
	 * @throws IOException
	 * @throws ParseException
	 */
	public void deliver(ModuleRevisionId mrid, String revision,	String destIvyPattern, DeliverOptions options) throws IOException, ParseException {
		_deliverEngine.deliver(mrid, revision, destIvyPattern, options);
	}


    /////////////////////////////////////////////////////////////////////////
    //                         PUBLISH
    /////////////////////////////////////////////////////////////////////////

	public Collection publish(ModuleRevisionId mrid, Collection srcArtifactPattern, String resolverName, PublishOptions options) throws IOException {
		return _publishEngine.publish(mrid, srcArtifactPattern, resolverName, options);
	}

    /////////////////////////////////////////////////////////////////////////
    //                         SORT 
    /////////////////////////////////////////////////////////////////////////

	/**
     * Sorts the collection of IvyNode from the less dependent to the more dependent
     */
    public List sortNodes(Collection nodes) {
        return _sortEngine.sortNodes(nodes);
    }


    /**
     * Sorts the given ModuleDescriptors from the less dependent to the more dependent.
     * This sort ensures that a ModuleDescriptor is always found in the list before all 
     * ModuleDescriptors depending directly on it.
     * @param moduleDescriptors a Collection of ModuleDescriptor to sort
     * @return a List of sorted ModuleDescriptors
     */
    public List sortModuleDescriptors(Collection moduleDescriptors) {
        return _sortEngine.sortModuleDescriptors(moduleDescriptors);   
    }
    
    /////////////////////////////////////////////////////////////////////////
    //                         SEARCH
    /////////////////////////////////////////////////////////////////////////
    
    public ResolvedModuleRevision findModule(ModuleRevisionId mrid) {
    	ResolveOptions options = new ResolveOptions();
    	options.setValidate(false);
    	options.setCache(CacheManager.getInstance(_settings));
    	return _resolveEngine.findModule(mrid, options);
    }
    
	public ModuleEntry[] listModuleEntries(OrganisationEntry org) {
		return _searchEngine.listModuleEntries(org);
	}

	public ModuleId[] listModules(ModuleId criteria, PatternMatcher matcher) {
		return _searchEngine.listModules(criteria, matcher);
	}

	public ModuleRevisionId[] listModules(ModuleRevisionId criteria, PatternMatcher matcher) {
		return _searchEngine.listModules(criteria, matcher);
	}

	public String[] listModules(String org) {
		return _searchEngine.listModules(org);
	}

	public OrganisationEntry[] listOrganisationEntries() {
		return _searchEngine.listOrganisationEntries();
	}

	public String[] listOrganisations() {
		return _searchEngine.listOrganisations();
	}

	public RevisionEntry[] listRevisionEntries(ModuleEntry module) {
		return _searchEngine.listRevisionEntries(module);
	}

	public String[] listRevisions(String org, String module) {
		return _searchEngine.listRevisions(org, module);
	}

	public String[] listTokenValues(String token, Map otherTokenValues) {
		return _searchEngine.listTokenValues(token, otherTokenValues);
	}

	/////////////////////////////////////////////////////////////////////////
    //                         INTERRUPTIONS
    /////////////////////////////////////////////////////////////////////////
    

    /**
     * Interrupts the current running operation, no later than
     * interruptTimeout milliseconds after the call
     */
    public void interrupt() {
    	Thread operatingThread = IvyContext.getContext().getOperatingThread();
    	interrupt(operatingThread);
    }

    /**
     * Interrupts the current running operation in the given operating thread, 
     * no later than interruptTimeout milliseconds after the call
     */
	public void interrupt(Thread operatingThread) {
		if (operatingThread != null && operatingThread.isAlive()) {
    		if (operatingThread == Thread.currentThread()) {
    			throw new IllegalStateException("cannot call interrupt from ivy operating thread");
    		}
			Message.verbose("interrupting operating thread...");
    		operatingThread.interrupt();
    		synchronized (this) {
    			_interrupted = true;
    		}
    		try {
				Message.verbose("waiting clean interruption of operating thread");
    			operatingThread.join(_settings.getInterruptTimeout());
			} catch (InterruptedException e) {
			}
			if (operatingThread.isAlive()) {
				Message.warn("waited clean interruption for too long: stopping operating thread");
				operatingThread.stop();
			}
    		synchronized (this) {
    			_interrupted = false;
    		}
    	}
	}
	
	public synchronized boolean isInterrupted() {
		return _interrupted;
	}
        

    /**
     * Check if the current operation has been interrupted, and if it is the case, throw a runtime exception
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

	
	public CacheManager getCacheManager(File cache) {
		//TODO : reuse instance
		CacheManager cacheManager = new CacheManager(_settings, cache);
		return cacheManager;
	}


	private void assertBound() {
		if (!_bound) {
			bind();
		}
	}

	private void postConfigure() {
		Collection triggers = _settings.getTriggers();
		for (Iterator iter = triggers.iterator(); iter.hasNext();) {
			Trigger trigger = (Trigger) iter.next();
			_eventManager.addIvyListener(trigger, trigger.getEventFilter());
		}
	}

	public String getVariable(String name) {
		assertBound();
		return _settings.getVariable(name);
	}

	public String substitute(String str) {
		assertBound();
		return _settings.substitute(str);
	}

	public void setVariable(String varName, String value) {
		assertBound();
		_settings.setVariable(varName, value);
	}
	
	/////////////////////////////////////////////////////////////////////
	// GETTERS / SETTERS
	/////////////////////////////////////////////////////////////////////


	public IvySettings getSettings() {
		return _settings;
	}
	
	public EventManager getEventManager() {
		return _eventManager;
	}

	public CheckEngine getCheckEngine() {
		return _checkEngine;
	}

	public void setCheckEngine(CheckEngine checkEngine) {
		_checkEngine = checkEngine;
	}

	public DeliverEngine getDeliverEngine() {
		return _deliverEngine;
	}

	public void setDeliverEngine(DeliverEngine deliverEngine) {
		_deliverEngine = deliverEngine;
	}

	public InstallEngine getInstallEngine() {
		return _installEngine;
	}

	public void setInstallEngine(InstallEngine installEngine) {
		_installEngine = installEngine;
	}

	public PublishEngine getPublishEngine() {
		return _publishEngine;
	}

	public void setPublishEngine(PublishEngine publishEngine) {
		_publishEngine = publishEngine;
	}

	public ResolveEngine getResolveEngine() {
		return _resolveEngine;
	}

	public void setResolveEngine(ResolveEngine resolveEngine) {
		_resolveEngine = resolveEngine;
	}

	public RetrieveEngine getRetrieveEngine() {
		return _retrieveEngine;
	}

	public void setRetrieveEngine(RetrieveEngine retrieveEngine) {
		_retrieveEngine = retrieveEngine;
	}

	public SearchEngine getSearchEngine() {
		return _searchEngine;
	}

	public void setSearchEngine(SearchEngine searchEngine) {
		_searchEngine = searchEngine;
	}

	public SortEngine getSortEngine() {
		return _sortEngine;
	}

	public void setSortEngine(SortEngine sortEngine) {
		_sortEngine = sortEngine;
	}

	public void setEventManager(EventManager eventManager) {
		_eventManager = eventManager;
	}

	public void setSettings(IvySettings settings) {
		_settings = settings;
	}

}
