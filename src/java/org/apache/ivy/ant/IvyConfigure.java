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
package org.apache.ivy.ant;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Configure Ivy with an ivysettings.xml file
 */
public class IvyConfigure extends Task {

    /**
     * Use to override a previous definition of settings with the same id
     */
    public static final String OVERRIDE_TRUE = "true";

    /**
     * Use to avoid overriding a previous definition of settings with the same id
     */
    public static final String OVERRIDE_FALSE = "false";

    /**
     * Use to raise an error if attempting to override a previous definition of settings with the
     * same id
     */
    public static final String OVERRIDE_NOT_ALLOWED = "notallowed";

    private static final Collection OVERRIDE_VALUES = Arrays.asList(new String[] {OVERRIDE_TRUE,
            OVERRIDE_FALSE, OVERRIDE_NOT_ALLOWED});

    private String override = OVERRIDE_NOT_ALLOWED;

    private IvyAntSettings settings = new IvyAntSettings();

    public void setSettingsId(String settingsId) {
        settings.setId(settingsId);
    }

    public String getSettingsId() {
        return settings.getId();
    }

    public void setOverride(String override) {
        if (!OVERRIDE_VALUES.contains(override)) {
            throw new IllegalArgumentException("invalid override value '" + override + "'. "
                    + "Valid values are " + OVERRIDE_VALUES);
        }
        this.override = override;
    }

    public String getOverride() {
        return override;
    }

    public File getFile() {
        return settings.getFile();
    }

    public void setFile(File file) {
        settings.setFile(file);
    }

    public URL getUrl() {
        return settings.getUrl();
    }

    public void setUrl(String url) throws MalformedURLException {
        settings.setUrl(url);
    }

    public void setUrl(URL url) {
        if (url == null) {
            throw new NullPointerException("Cannot set a null URL");
        }
        settings.setUrl(url);
    }

    public String getRealm() {
        return settings.getRealm();
    }

    public void setRealm(String realm) {
        settings.setRealm(realm);
    }

    public String getHost() {
        return settings.getHost();
    }

    public void setHost(String host) {
        settings.setHost(host);
    }

    public String getUserName() {
        return settings.getUsername();
    }

    public void setUserName(String userName) {
        settings.setUsername(userName);
    }

    public String getPasswd() {
        return settings.getPasswd();
    }

    public void setPasswd(String passwd) {
        settings.setPasswd(passwd);
    }

    public void addConfiguredWorkspaceResolver(AntWorkspaceResolver resolver) {
        settings.addConfiguredWorkspaceResolver(resolver);
    }

    @Override
    public void execute() throws BuildException {
        String settingsId = settings.getId();
        Object otherRef = getProject().getReference(settingsId);

        if ((otherRef != null) && OVERRIDE_NOT_ALLOWED.equals(override)) {
            throw new BuildException(
                    "Overriding a previous definition of ivy:settings with the id '" + settingsId
                            + "' is not allowed when using override='" + OVERRIDE_NOT_ALLOWED
                            + "'.");
        }

        if ((otherRef != null) && OVERRIDE_FALSE.equals(override)) {
            verbose("A settings definition is already available for " + settingsId + ": skipping");
            return;
        }

        settings.setProject(getProject());
        getProject().addReference(settingsId, settings);
        settings.createIvyEngine(this);
    }

    private void verbose(String msg) {
        log(msg, Project.MSG_VERBOSE);
    }
}
