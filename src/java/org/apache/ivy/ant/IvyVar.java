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
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.tools.ant.BuildException;

/**
 * This task let user set ivy variables from ant.
 */
public class IvyVar extends IvyTask {
    private String name;

    private String value;

    private File file;

    private String url;

    private String prefix;

    public File getFile() {
        return file;
    }

    public void setFile(File aFile) {
        file = aFile;
    }

    public String getName() {
        return name;
    }

    public void setName(String aName) {
        name = aName;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String aPrefix) {
        prefix = aPrefix;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String aUrl) {
        url = aUrl;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String aValue) {
        value = aValue;
    }

    public void doExecute() throws BuildException {
        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        if (getName() != null) {
            settings.setVariable(getVarName(getName()), getValue());
        } else {
            Properties props = new Properties();
            InputStream is = null;
            try {
                if (getFile() != null) {
                    is = new FileInputStream(getFile());
                } else if (getUrl() != null) {
                    is = new URL(getUrl()).openStream();
                } else {
                    throw new BuildException("specify either name or file or url to ivy var task");
                }
                props.load(is);
            } catch (Exception ex) {
                throw new BuildException("impossible to load variables from file: " + ex, ex);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                String value = (String) props.get(name);
                settings.setVariable(getVarName(name), value);
            }
        }
    }

    private String getVarName(String name) {
        String prefix = getPrefix();
        if (prefix != null) {
            if (prefix.endsWith(".")) {
                return prefix + name;
            } else {
                return prefix + "." + name;
            }
        }
        return name;
    }
}
