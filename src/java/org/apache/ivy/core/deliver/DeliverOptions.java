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
package org.apache.ivy.core.deliver;

import java.util.Date;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.settings.IvySettings;

/**
 * A set of options used to do a deliver.
 */
public class DeliverOptions {
	private String _status;
	private Date _pubdate;
	private CacheManager _cache;
	private PublishingDependencyRevisionResolver _pdrResolver = new DefaultPublishingDRResolver(); 
	private boolean _validate = true;
	private boolean _resolveDynamicRevisions = true;
	private String _resolveId;
	
	/**
	 * Returns an instance of DeliverOptions with options corresponding to default values
	 * taken from the given settings.
	 * 
	 * @param settings The settings to use to get default option values
	 * @return a DeliverOptions instance ready to be used or customized
	 */
	public static DeliverOptions newInstance(IvySettings settings) {
		return new DeliverOptions(null, new Date(), CacheManager.getInstance(settings), 
				new DefaultPublishingDRResolver(),
				settings.doValidate(),
				true);
	}

	/**
	 * Creates an instance of DeliverOptions which require to be configured
	 * using the appropriate setters.
	 */
	public DeliverOptions() {
	}
	
	/**
	 * Creates an instance of DeliverOptions with all options explicitly set.
	 */
	public DeliverOptions(String status, Date pubDate, CacheManager cache, PublishingDependencyRevisionResolver pdrResolver, boolean validate, boolean resolveDynamicRevisions) {
		_status = status;
		_pubdate = pubDate;
		_cache = cache;
		_pdrResolver = pdrResolver;
		_validate = validate;
		_resolveDynamicRevisions = resolveDynamicRevisions;
	}

	public CacheManager getCache() {
		return _cache;
	}

	public DeliverOptions setCache(CacheManager cache) {
		_cache = cache;
		return this;
	}
	
	/**
	 * Return the pdrResolver that will be used during deliver for each 
	 * dependency to get its published information. 
	 * This can particularly useful
	 * when the deliver is made for a release, and when we wish to deliver each
	 * dependency which is still in integration. The PublishingDependencyRevisionResolver
	 * can then do the delivering work for the dependency and return the new (delivered)
	 * dependency info (with the delivered revision). Note that 
	 * PublishingDependencyRevisionResolver is only called for each <b>direct</b> dependency.
	 * @return the pdrResolver that will be used during deliver
	 */
	public PublishingDependencyRevisionResolver getPdrResolver() {
		return _pdrResolver;
	}

	/**
	 * Sets the pdrResolver that will be used during deliver for each 
	 * dependency to get its published information. 
	 * This can particularly useful
	 * when the deliver is made for a release, and when we wish to deliver each
	 * dependency which is still in integration. The PublishingDependencyRevisionResolver
	 * can then do the delivering work for the dependency and return the new (delivered)
	 * dependency info (with the delivered revision). Note that 
	 * PublishingDependencyRevisionResolver is only called for each <b>direct</b> dependency.
	 * @return the instance of DeliverOptions on which the method has been called, 
	 * for easy method chaining 
	 */
	public DeliverOptions setPdrResolver(PublishingDependencyRevisionResolver pdrResolver) {
		_pdrResolver = pdrResolver;
		return this;
	}

	public boolean isResolveDynamicRevisions() {
		return _resolveDynamicRevisions;
	}

	public DeliverOptions setResolveDynamicRevisions(boolean resolveDynamicRevisions) {
		_resolveDynamicRevisions = resolveDynamicRevisions;
		return this;
	}

	public boolean isValidate() {
		return _validate;
	}

	public DeliverOptions setValidate(boolean validate) {
		_validate = validate;
		return this;
	}

	public Date getPubdate() {
		return _pubdate;
	}

	public DeliverOptions setPubdate(Date pubdate) {
		_pubdate = pubdate;
		return this;
	}

	/**
	 * Returns the status to which the module should be delivered,
	 * or null if the current status should be kept.
	 * @return the status to which the module should be delivered
	 */
	public String getStatus() {
		return _status;
	}

	/**
	 * Sets the status to which the module should be delivered,
	 * use null if the current status should be kept.
	 * @return the instance of DeliverOptions on which the method has been called, 
	 * for easy method chaining 
	 */
	public DeliverOptions setStatus(String status) {
		_status = status;
		return this;
	}
	
	/**
	 * Returns the id of a previous resolve to use for delivering.
	 * @return the id of a previous resolve
	 */
	public String getResolveId() {
		return _resolveId;
	}
	
	/**
	 * Sets the id of a previous resolve to use for delivering.
	 * @param resolveId the id of a previous resolve
	 * @return the instance of DeliverOptions on which the method has been called, 
	 * for easy method chaining 
	 */
	public DeliverOptions setResolveId(String resolveId) {
		_resolveId = resolveId;
		return this;
	}
	
	public String toString() {
		return "status="+_status+" pubdate="+_pubdate+" validate="+_validate+" resolveDynamicRevisions="+_resolveDynamicRevisions+" cache="+_cache+" resolveId="+_resolveId;
	}
	
}
