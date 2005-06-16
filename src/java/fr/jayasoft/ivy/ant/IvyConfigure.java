/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Property;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.url.URLHandlerDispatcher;
import fr.jayasoft.ivy.url.URLHandlerRegistry;
import fr.jayasoft.ivy.util.Message;

/**
 * @author Hanin
 *
 */
public class IvyConfigure extends IvyTask {
    private File _file = null; 
    private URL _url = null;
    private String _realm = null;
    private String _host = null;
    private String _userName = null;
    private String _passwd = null;

    public File getFile() {
        return _file;
    }
    public void setFile(File conf) {
        _file = conf;
    }
    public URL getUrl() {
        return _url;
    }    
    public void setUrl(String url) throws MalformedURLException {
        _url = new URL(url);
    }
    public String getPasswd() {
        return _passwd;
    }
    public void setPasswd(String passwd) {
        _passwd = passwd;
    }
    public String getRealm() {
        return _realm;
    }
    public void setRealm(String realm) {
        _realm = format(realm);
    }
    public String getHost() {
        return _host;
    }
    public void setHost(String host) {
        _host = format(host);
    }
    public String getUsername() {
        return _userName;
    }
    public void setUsername(String userName) {
        _userName = format(userName);
    }
    private String format(String str) {
        return str == null ? str : (str.trim().length() == 0 ? null : str.trim());
    }

    public void execute() throws BuildException {
        try {
	        loadDefaultProperties();
        } catch (Exception ex) {
            throw new BuildException("impossible to load ivy default properties file: "+ex.getMessage(), ex);
        }
        Message.init(new AntMessageImpl(getProject()));
        Ivy ivy = new Ivy();
        try {
            configureURLHandler();
            ivy.addAllVariables(getProject().getProperties());
            if (_file == null && _url == null) {
                _file = new File(getProject().getBaseDir(), getProject().getProperty("ivy.conf.file"));
                Message.verbose("searching ivyconf file: trying "+_file);
                if (!_file.exists()) {
                    _file = new File(getProject().getProperty("ivy.conf.file"));
                    Message.verbose("searching ivyconf file: trying "+_file);
                    if (!_file.exists()) {
                        Message.info("no configuration file found, using default...");
                        _file = null;
                        _url = Ivy.class.getResource("ivyconf.xml");
                    }
                }
            } 
            if (_file != null) {
                if (!_file.exists()) {
                    throw new BuildException("configuration file does not exist: "+_file);
                } else {
                    ivy.configure(_file);
                }
            } else {
                ivy.configure(_url);
            }
            setIvyInstance(ivy);
        } catch (Exception ex) {
            throw new BuildException("impossible to configure ivy with given file: "+ex.getMessage(), ex);
        }
    }
    private void loadDefaultProperties() {
        Property prop = new Property() {
            public void execute() throws BuildException {
                URL url = IvyConfigure.class.getResource("ivy.properties");
                // this is copy of loadURL code from ant Property task  (not available in 1.5.1)
                Properties props = new Properties();
                log("Loading " + url, Project.MSG_VERBOSE);
                try {
                    InputStream is = url.openStream();
                    try {
                        props.load(is);
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                    addProperties(props);
                } catch (IOException ex) {
                    throw new BuildException(ex, getLocation());
                }
            }
        };
        prop.setProject(getProject());
        prop.execute();
    }
    private void configureURLHandler() {
        URLHandlerDispatcher dispatcher = new URLHandlerDispatcher();
        dispatcher.setDownloader("http", URLHandlerRegistry.getHttp(getRealm(), getHost(), getUsername(), getPasswd()));
        URLHandlerRegistry.setDefault(dispatcher);
    }
}
