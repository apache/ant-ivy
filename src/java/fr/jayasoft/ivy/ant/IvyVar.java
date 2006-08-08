/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import org.apache.tools.ant.BuildException;

import fr.jayasoft.ivy.Ivy;

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
        if (getName() != null) {
            ivy.setVariable(getVarName(getName()), getValue());
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
                ivy.setVariable(getVarName(name), value);
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
