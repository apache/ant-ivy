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

import org.apache.ivy.core.settings.IvySettings;

/**
 * A set of options used to do a deliver.
 */
public class DeliverOptions {
    private String status;

    private Date pubdate;

    private PublishingDependencyRevisionResolver pdrResolver = new DefaultPublishingDRResolver();

    private boolean validate = true;

    private boolean resolveDynamicRevisions = true;

    private boolean replaceForcedRevisions = false;

    private String resolveId;

    private String[] confs;

    private String pubBranch;

    /**
     * True to indicate that the revConstraint attribute should be generated if applicable, false to
     * never generate the revConstraint attribute.
     */
    private boolean generateRevConstraint = true;

    /** true to merge parent descriptor elements into delivered child descriptor */
    private boolean merge = true;

    /**
     * Returns an instance of DeliverOptions with options corresponding to default values taken from
     * the given settings.
     * 
     * @param settings
     *            The settings to use to get default option values
     * @return a DeliverOptions instance ready to be used or customized
     */
    public static DeliverOptions newInstance(IvySettings settings) {
        return new DeliverOptions(null, new Date(), new DefaultPublishingDRResolver(),
                settings.doValidate(), true, null);
    }

    /**
     * Creates an instance of DeliverOptions which require to be configured using the appropriate
     * setters.
     */
    public DeliverOptions() {
    }

    /**
     * Creates an instance of DeliverOptions with all options explicitly set.
     */
    public DeliverOptions(String status, Date pubDate,
            PublishingDependencyRevisionResolver pdrResolver, boolean validate,
            boolean resolveDynamicRevisions, String[] confs) {
        this.status = status;
        this.pubdate = pubDate;
        this.pdrResolver = pdrResolver;
        this.validate = validate;
        this.resolveDynamicRevisions = resolveDynamicRevisions;
        this.confs = confs;
    }

    /**
     * Return the pdrResolver that will be used during deliver for each dependency to get its
     * published information. This can particularly useful when the deliver is made for a release,
     * and when we wish to deliver each dependency which is still in integration. The
     * PublishingDependencyRevisionResolver can then do the delivering work for the dependency and
     * return the new (delivered) dependency info (with the delivered revision). Note that
     * PublishingDependencyRevisionResolver is only called for each <b>direct</b> dependency.
     * 
     * @return the pdrResolver that will be used during deliver
     */
    public PublishingDependencyRevisionResolver getPdrResolver() {
        return pdrResolver;
    }

    /**
     * Sets the pdrResolver that will be used during deliver for each dependency to get its
     * published information. This can particularly useful when the deliver is made for a release,
     * and when we wish to deliver each dependency which is still in integration. The
     * PublishingDependencyRevisionResolver can then do the delivering work for the dependency and
     * return the new (delivered) dependency info (with the delivered revision). Note that
     * PublishingDependencyRevisionResolver is only called for each <b>direct</b> dependency.
     * 
     * @return the instance of DeliverOptions on which the method has been called, for easy method
     *         chaining
     */
    public DeliverOptions setPdrResolver(PublishingDependencyRevisionResolver pdrResolver) {
        this.pdrResolver = pdrResolver;
        return this;
    }

    public boolean isResolveDynamicRevisions() {
        return resolveDynamicRevisions;
    }

    public DeliverOptions setResolveDynamicRevisions(boolean resolveDynamicRevisions) {
        this.resolveDynamicRevisions = resolveDynamicRevisions;
        return this;
    }

    public boolean isReplaceForcedRevisions() {
        return replaceForcedRevisions;
    }

    public DeliverOptions setReplaceForcedRevisions(boolean replaceForcedRevisions) {
        this.replaceForcedRevisions = replaceForcedRevisions;
        return this;
    }

    public boolean isValidate() {
        return validate;
    }

    public DeliverOptions setValidate(boolean validate) {
        this.validate = validate;
        return this;
    }

    public Date getPubdate() {
        return pubdate;
    }

    public DeliverOptions setPubdate(Date pubdate) {
        this.pubdate = pubdate;
        return this;
    }

    /**
     * Returns the status to which the module should be delivered, or null if the current status
     * should be kept.
     * 
     * @return the status to which the module should be delivered
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status to which the module should be delivered, use null if the current status
     * should be kept.
     * 
     * @return the instance of DeliverOptions on which the method has been called, for easy method
     *         chaining
     */
    public DeliverOptions setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Returns the id of a previous resolve to use for delivering.
     * 
     * @return the id of a previous resolve
     */
    public String getResolveId() {
        return resolveId;
    }

    /**
     * Sets the id of a previous resolve to use for delivering.
     * 
     * @param resolveId
     *            the id of a previous resolve
     * @return the instance of DeliverOptions on which the method has been called, for easy method
     *         chaining
     */
    public DeliverOptions setResolveId(String resolveId) {
        this.resolveId = resolveId;
        return this;
    }

    /**
     * Return the configurations which must be deliverd. Returns <tt>null</tt> if all configurations
     * has to be deliverd. Attention: the returned array can contain wildcards!
     * 
     * @return the configurations to deliver
     */
    public String[] getConfs() {
        return confs;
    }

    /**
     * Sets the configurations to deliver.
     * 
     * @param confs
     *            the configurations to deliver
     * @return the instance of DeliverOptions on which the method has been called, for easy method
     *         chaining
     */
    public DeliverOptions setConfs(String[] confs) {
        this.confs = confs;
        return this;
    }

    /**
     * Returns the branch with which the Ivy file should be delivered, or <code>null</code> if
     * branch info shouldn't be changed.
     * 
     * @return the branch with which the Ivy file should be delivered
     */
    public String getPubBranch() {
        return pubBranch;
    }

    /**
     * Sets the branch with which the Ivy file should be delivered.
     * 
     * @param pubBranch
     *            the branch with which the Ivy file should be delivered
     * @return the instance of DeliverOptions on which the method has been called, for easy method
     *         chaining
     */
    public DeliverOptions setPubBranch(String pubBranch) {
        this.pubBranch = pubBranch;
        return this;
    }

    public boolean isGenerateRevConstraint() {
        return generateRevConstraint;
    }

    public DeliverOptions setGenerateRevConstraint(boolean generateRevConstraint) {
        this.generateRevConstraint = generateRevConstraint;
        return this;
    }

    public boolean isMerge() {
        return merge;
    }

    public DeliverOptions setMerge(boolean merge) {
        this.merge = merge;
        return this;
    }

    public String toString() {
        return "status=" + status + " pubdate=" + pubdate + " validate=" + validate
                + " resolveDynamicRevisions=" + resolveDynamicRevisions + " merge=" + merge
                + " resolveId=" + resolveId + " pubBranch=" + pubBranch;

    }

}
