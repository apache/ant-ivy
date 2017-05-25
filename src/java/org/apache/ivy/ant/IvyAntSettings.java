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
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.DataType;

public class IvyAntSettings extends DataType {

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

    private Ivy ivyEngine = null;

    private File file = null;

    private URL url = null;

    private String realm = null;

    private String host = null;

    private String userName = null;

    private String passwd = null;

    private String id = "ivy.instance";

    private boolean autoRegistered = false;

    private AntWorkspaceResolver antWorkspaceResolver;

    /**
     * Returns the default ivy settings of this classloader. If it doesn't exist yet, a new one is
     * created using the given project to back the VariableContainer.
     * 
     * @param project
     *            TODO add text.
     * @return An IvySetting instance.
     */
    public static IvyAntSettings getDefaultInstance(ProjectComponent task) {
        Project project = task.getProject();
        Object defaultInstanceObj = project.getReference("ivy.instance");
        if (defaultInstanceObj != null
                && defaultInstanceObj.getClass().getClassLoader() != IvyAntSettings.class
                        .getClassLoader()) {
            task.log("ivy.instance reference an ivy:settings defined in an other classloader.  "
                    + "An new default one will be used in this project.", Project.MSG_WARN);
            defaultInstanceObj = null;
        }
        if (defaultInstanceObj != null && !(defaultInstanceObj instanceof IvyAntSettings)) {
            throw new BuildException("ivy.instance reference a "
                    + defaultInstanceObj.getClass().getName()
                    + " an not an IvyAntSettings.  Please don't use this reference id ()");
        }
        if (defaultInstanceObj == null) {
            task.log("No ivy:settings found for the default reference 'ivy.instance'.  "
                    + "A default instance will be used", Project.MSG_VERBOSE);

            IvyAntSettings settings = new IvyAntSettings();
            settings.setProject(project);
            project.addReference("ivy.instance", settings);
            settings.createIvyEngine(task);
            return settings;
        } else {
            return (IvyAntSettings) defaultInstanceObj;
        }
    }

    /*
     * Keep this for backwards compatibility!
     */
    public static IvyAntSettings getDefaultInstance(Task task) {
        return getDefaultInstance((ProjectComponent) task);
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

    public void setProject(Project p) {
        super.setProject(p);

        if ("ivy.instance".equals(id) && !getProject().getReferences().containsKey(id)) {
            // register ourselfs as default settings, just in case the id attribute is not set
            getProject().addReference("ivy.instance", this);
            autoRegistered = true;
        }
    }

    private static String format(String str) {
        return str == null ? str : (str.trim().length() == 0 ? null : str.trim());
    }

    public void addConfiguredCredentials(Credentials c) {
        CredentialsStore.INSTANCE.addCredentials(c.getRealm(), c.getHost(), c.getUsername(),
            c.getPasswd());
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setUrl(String confUrl) throws MalformedURLException {
        this.url = new URL(confUrl);
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    /*
     * This is usually not necessary to define a reference in Ant, but it's the only way to know the
     * id of the settings, which we use to set ant properties.
     */
    public void setId(String id) {
        if (autoRegistered && getProject().getReference(this.id) == this) {
            getProject().getReferences().remove(this.id);
            autoRegistered = false;
        }
        this.id = id;

        if (getProject() != null) {
            getProject().addReference(this.id, this);
        }
    }

    public String getId() {
        return id;
    }

    /**
     * Return the configured Ivy instance.
     * 
     * @return Returns the configured Ivy instance.
     */
    public Ivy getConfiguredIvyInstance(ProjectComponent task) {
        if (ivyEngine == null) {
            createIvyEngine(task);
        }
        return ivyEngine;
    }

    /*
     * Keep this for backwards compatibility!
     */
    public Ivy getConfiguredIvyInstance(Task task) {
        return getConfiguredIvyInstance((ProjectComponent) task);
    }

    void createIvyEngine(final ProjectComponent task) {
        Project project = task.getProject();
        Property prop = new Property() {
            public void execute() throws BuildException {
                addProperties(getDefaultProperties(task));
            }
        };
        prop.setProject(project);
        prop.init();
        prop.execute();

        IvyAntVariableContainer ivyAntVariableContainer = new IvyAntVariableContainer(project);
        IvySettings settings = new IvySettings(ivyAntVariableContainer);
        settings.setBaseDir(project.getBaseDir());

        if (file == null && url == null) {
            defineDefaultSettingFile(ivyAntVariableContainer, task);
        }

        if (antWorkspaceResolver != null) {
            settings.addConfigured(antWorkspaceResolver.getResolver());
        }

        Ivy ivy = Ivy.newInstance(settings);
        try {
            ivy.pushContext();
            AntMessageLogger.register(task, ivy);

            Message.showInfo();
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
                    + (file != null ? "file: " + file : "url: " + url) + " : " + e, e);
        } catch (IOException e) {
            throw new BuildException("impossible to configure ivy:settings with given "
                    + (file != null ? "file: " + file : "url: " + url) + " : " + e, e);
        } finally {
            ivy.popContext();
        }
    }

    protected Properties getDefaultProperties(ProjectComponent task) {
        URL url = IvySettings.getDefaultPropertiesURL();
        // this is copy of loadURL code from ant Property task (not available in 1.5.1)
        Properties props = new Properties();
        task.log("Loading " + url, Project.MSG_VERBOSE);
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
    private void defineDefaultSettingFile(IvyVariableContainer variableContainer,
            ProjectComponent task) {
        String settingsFileName = variableContainer.getVariable("ivy.conf.file");
        if (settingsFileName != null
                && !settingsFileName.equals(variableContainer.getVariable("ivy.settings.file"))) {
            task.log("DEPRECATED: 'ivy.conf.file' is deprecated, use 'ivy.settings.file' instead",
                Project.MSG_INFO);
        } else {
            settingsFileName = variableContainer.getVariable("ivy.settings.file");
        }
        File[] settingsLocations = new File[] {
                new File(getProject().getBaseDir(), settingsFileName),
                new File(getProject().getBaseDir(), "ivyconf.xml"), new File(settingsFileName),
                new File("ivyconf.xml")};
        for (int i = 0; i < settingsLocations.length; i++) {
            file = settingsLocations[i];
            task.log("searching settings file: trying " + file, Project.MSG_VERBOSE);
            if (file.exists()) {
                break;
            }
        }
        if (!file.exists()) {
            file = null;
            if (Boolean.valueOf(getProject().getProperty("ivy.14.compatible")).booleanValue()) {
                task.log("no settings file found, using Ivy 1.4 default...", Project.MSG_VERBOSE);
                url = IvySettings.getDefault14SettingsURL();
            } else {
                String settingsFileUrl = variableContainer.getVariable("ivy.settings.url");
                if (settingsFileUrl != null) {
                    try {
                        url = new URL(settingsFileUrl);
                    } catch (MalformedURLException e) {
                        throw new BuildException(
                                "Impossible to configure ivy:settings with given url: "
                                        + settingsFileUrl + ": " + e.getMessage(), e);
                    }
                } else {
                    task.log("no settings file found, using default...", Project.MSG_VERBOSE);
                    url = IvySettings.getDefaultSettingsURL();
                }
            }
        }
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

    public void addConfiguredWorkspaceResolver(AntWorkspaceResolver antWorkspaceResolver) {
        this.antWorkspaceResolver = antWorkspaceResolver;
    }
}
