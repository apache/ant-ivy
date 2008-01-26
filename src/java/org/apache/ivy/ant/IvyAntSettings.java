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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.core.settings.IvyVariableContainer;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.url.CredentialsStore;
import org.apache.ivy.util.url.URLHandler;
import org.apache.ivy.util.url.URLHandlerDispatcher;
import org.apache.ivy.util.url.URLHandlerRegistry;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;

public class IvyAntSettings extends Task {

    public static class Credentials {
        private String realm;

        private String host;

        private String username;

        private String passwd;

        public String getPasswd() {
            return this.passwd;
        }

        public void setPasswd(String passwd) {
            this.passwd = passwd;
        }

        public String getRealm() {
            return this.realm;
        }

        public void setRealm(String realm) {
            this.realm = format(realm);
        }

        public String getHost() {
            return this.host;
        }

        public void setHost(String host) {
            this.host = format(host);
        }

        public String getUsername() {
            return this.username;
        }

        public void setUsername(String userName) {
            this.username = format(userName);
        }
    }

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

    private static final Collection OVERRIDE_VALUES = Arrays.asList(new String[] {
            OVERRIDE_TRUE, OVERRIDE_FALSE, OVERRIDE_NOT_ALLOWED
    });

    private Ivy ivyEngine = null;

    private File file = null;

    private URL url = null;

    private String realm = null;

    private String host = null;

    private String userName = null;

    private String passwd = null;
    
    private String id = "ivy.instance";

    private String override = OVERRIDE_NOT_ALLOWED;

    /**
     * Returns the default ivy settings of this classloader. If it doesn't exist yet, a new one is
     * created using the given project to back the VariableContainer.
     * 
     * @param  project  TODO add text.
     * @return  An IvySetting instance.
     */
    public static IvyAntSettings getDefaultInstance(Project project) {
        Object defaultInstanceObj = project.getReference("ivy.instance");
        if (defaultInstanceObj != null
                && defaultInstanceObj.getClass().getClassLoader() != IvyAntSettings.class
                        .getClassLoader()) {
            project.log("ivy.instance reference an ivy:settings defined in an other classloader.  "
                     + "An new default one will be used in this project.", Project.MSG_WARN);
            defaultInstanceObj = null;
        }
        if (defaultInstanceObj != null && !(defaultInstanceObj instanceof IvyAntSettings)) {
            throw new BuildException("ivy.instance reference a "
                    + defaultInstanceObj.getClass().getName()
                    + " an not an IvyAntSettings.  Please don't use this reference id ()");
        }
        if (defaultInstanceObj == null) {
            project.log("No ivy:settings found for the default reference 'ivy.instance'.  " 
                    + "A default instance will be used", Project.MSG_INFO);
            IvyAntSettings defaultInstance = new IvyAntSettings();
            defaultInstance.setProject(project);
            defaultInstance.perform();
            return defaultInstance;
        } else {
            return (IvyAntSettings) defaultInstanceObj;
        }
    }

    public File getFile() {
        return file;
    }

    public URL getUrl() {
        return url;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String aPasswd) {
        passwd = aPasswd;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String aRealm) {
        realm = format(aRealm);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String aHost) {
        host = format(aHost);
    }

    public String getUsername() {
        return userName;
    }

    public void setUsername(String aUserName) {
        userName = format(aUserName);
    }


    private static String format(String str) {
        return str == null ? str : (str.trim().length() == 0 ? null : str.trim());
    }

    public void addConfiguredCredentials(Credentials c) {
        CredentialsStore.INSTANCE.addCredentials(c.getRealm(), c.getHost(), c.getUsername(), c
                .getPasswd());
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setUrl(String confUrl) throws MalformedURLException {
        this.url = new URL(confUrl);
    }

    public void setOverride(String override) {
        if (!OVERRIDE_VALUES.contains(override)) {
            throw new IllegalArgumentException("invalid override value '" + override + "'. "
                + "Valid values are " + OVERRIDE_VALUES);
        }
        this.override = override;
    }
    
    /*
     * This is usually not necessary to define a reference in Ant, but it's the only
     * way to know the id of the settings, which we use to set ant properties.
     */
    public void setId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }

    /*
     * public void execute() throws BuildException { 
     * ensureMessageInitialised(); 
     * if (getId()==null) {
     * log("No id specified for the ivy:settings, set the instance as the default one",
     * Project.MSG_DEBUG); getProject().addReference("ivy.instance", this); } else {
     * getProject().addReference(id, this); } }
     */

    /**
     * Return the configured Ivy instance.
     * @return Returns the configured Ivy instance.
     */
    public Ivy getConfiguredIvyInstance() {
        if (ivyEngine == null) {
            perform();
        }
        return ivyEngine;
    }

    public void execute() throws BuildException {
        if (!OVERRIDE_TRUE.equals(override)) {
            if (getProject().getReference(id) != null) {
                if (OVERRIDE_FALSE.equals(override)) {
                    verbose("a settings definition is already available for " + id + ": skipping");
                    return;
                } else {
                    // OVERRIDE_NOT_ALLOWED
                    throw new BuildException(
                        "overriding a previous definition of ivy:settings with the id '" + id + "'" 
                        + " is not allowed when using override='" + OVERRIDE_NOT_ALLOWED + "'.");
                }
            }
        }
        getProject().addReference(id, this);
        Property prop = new Property() {
            public void execute() throws BuildException {
                addProperties(getDefaultProperties());
            }
        };
        prop.setProject(getProject());
        prop.execute();
        createIvyEngine();
    }

    private void createIvyEngine() {
        IvyAntVariableContainer ivyAntVariableContainer = new IvyAntVariableContainer(getProject());

        IvySettings settings = new IvySettings(ivyAntVariableContainer);
        
        if (file == null && url == null) {
            defineDefaultSettingFile(ivyAntVariableContainer);
        }

        Ivy ivy = Ivy.newInstance(settings);
        ivy.getLoggerEngine().pushLogger(new AntMessageLogger(this));
        Message.showInfo();
        try {
            configureURLHandler();
            if (file != null) {
                if (!file.exists()) {
                    throw new BuildException("settings file does not exist: " + file);
                }
                ivy.configure(file);
            } else {
                if (url == null) {
                    throw new AssertionError(
                            "ivy setting should have either a file, either an url,"
                                    + " and if not defineDefaultSettingFile must set it.");
                }
                ivy.configure(url);
            }
            ivyAntVariableContainer.updateProject(id);
            ivyEngine = ivy;
        } catch (ParseException e) {
            throw new BuildException("impossible to configure ivy:settings with given "
                    + (file != null ? "file: " + file : "url :" + url) + " :" + e, e);
        } catch (IOException e) {
            throw new BuildException("impossible to configure ivy:settings with given "
                    + (file != null ? "file: " + file : "url :" + url) + " :" + e, e);
        } finally {
            ivy.getLoggerEngine().popLogger();
        }
    }

    protected Properties getDefaultProperties() {
        URL url = IvySettings.getDefaultPropertiesURL();
        // this is copy of loadURL code from ant Property task (not available in 1.5.1)
        Properties props = new Properties();
        verbose("Loading " + url);
        try {
            InputStream is = url.openStream();
            try {
                props.load(is);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
        return props;
    }

    /**
     * Set file or url to its default value
     * 
     * @param variableContainer
     */
    private void defineDefaultSettingFile(IvyVariableContainer variableContainer) {
        String settingsFileName = variableContainer.getVariable("ivy.conf.file");
        if (settingsFileName != null 
                && !settingsFileName.equals(variableContainer.getVariable("ivy.settings.file"))) {
            info("DEPRECATED: 'ivy.conf.file' is deprecated, use 'ivy.settings.file' instead");
        } else {
            settingsFileName = variableContainer.getVariable("ivy.settings.file");
        }
        File[] settingsLocations = new File[] {
                new File(getProject().getBaseDir(), settingsFileName),
                new File(getProject().getBaseDir(), "ivyconf.xml"), 
                new File(settingsFileName),
                new File("ivyconf.xml") 
        };
        for (int i = 0; i < settingsLocations.length; i++) {
            file = settingsLocations[i];
            verbose("searching settings file: trying " + file);
            if (file.exists()) {
                break;
            }
        }
        if (!file.exists()) {
            if (Boolean.valueOf(getProject().getProperty("ivy.14.compatible")).booleanValue()) {
                info("no settings file found, using Ivy 1.4 default...");
                file = null;
                url = IvySettings.getDefault14SettingsURL();
            } else {
                info("no settings file found, using default...");
                file = null;
                url = IvySettings.getDefaultSettingsURL();
            }
        }
    }

    private void verbose(String msg) {
        log(msg, Project.MSG_VERBOSE);
    }

    private void info(String msg) {
        log(msg, Project.MSG_INFO);
    }

    private void warn(String msg) {
        log(msg, Project.MSG_WARN);
    }

    private void configureURLHandler() {
        // TODO : the credentialStore should also be scoped
        CredentialsStore.INSTANCE.addCredentials(getRealm(), getHost(), getUsername(), getPasswd());

        URLHandlerDispatcher dispatcher = new URLHandlerDispatcher();
        URLHandler httpHandler = URLHandlerRegistry.getHttp();
        dispatcher.setDownloader("http", httpHandler);
        dispatcher.setDownloader("https", httpHandler);
        URLHandlerRegistry.setDefault(dispatcher);
    }

}
