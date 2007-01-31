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
 * 
 * @author Xavier Hanin
 */
public class IvyVar extends IvyTask {
    private String _name;
    private String _value;
    
    private File _file;
    private String _url;
    
    private String _prefix;

    public File getFile() {
        return _file;
    }
    

    public void setFile(File file) {
        _file = file;
    }
    

    public String getName() {
        return _name;
    }
    

    public void setName(String name) {
        _name = name;
    }
    

    public String getPrefix() {
        return _prefix;
    }
    

    public void setPrefix(String prefix) {
        _prefix = prefix;
    }
    

    public String getUrl() {
        return _url;
    }
    

    public void setUrl(String url) {
        _url = url;
    }
    

    public String getValue() {
        return _value;
    }
    

    public void setValue(String value) {
        _value = value;
    }
    
    public void execute() throws BuildException {
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
                throw new BuildException("impossible to load variables from file: "+ex, ex);
            } finally {
                if (is != null) {
                    try {is.close();} catch (Exception e) {}
                }
            }
            for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
                String name = (String)iter.next();
                String value = (String)props.get(name);
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
