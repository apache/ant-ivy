/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyContext;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.util.StringUtils;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * @author Xavier Hanin
 *
 */
public class IvyTask extends Task {
    public static final String ANT_PROJECT_CONTEXT_KEY = "ant-project";
	private Boolean _validate = null; 

    protected boolean doValidate(Ivy ivy) {
        if (_validate != null) {
            return _validate.booleanValue();
        }
        return ivy.doValidate();
    }
    public boolean isValidate() {
        return _validate == null ? true : _validate.booleanValue();
    }
    public void setValidate(boolean validate) {
        _validate = Boolean.valueOf(validate);
    }
    
    protected Ivy getIvyInstance() {
        ensureMessageInitialised();
        Object reference = getProject().getReference("ivy.instance");
        if (reference == null) {
            Message.verbose("no ivy instance found: auto configuring ivy");
            IvyConfigure configure = new IvyConfigure();
            configure.setProject(getProject());
            configure.execute();
            reference = getProject().getReference("ivy.instance");
            if (reference == null) {
                throw new BuildException("ivy internal problem: impossible to get ivy instance !");
            }
        } 
        if (! (reference instanceof Ivy)) {
            throw new BuildException("ivy internal problem: bad ivy instance class: "+reference.getClass());
        }
        return (Ivy)reference;
    }

    protected void ensureMessageInitialised() {
        if (!Message.isInitialised()) { 
            Message.init(new AntMessageImpl(getProject()));
            getProject().addBuildListener(new BuildListener() {
                public void buildFinished(BuildEvent event) {
                    Message.uninit();
                }
    
                public void buildStarted(BuildEvent event) {
                }
                public void targetStarted(BuildEvent event) {
                }
                public void targetFinished(BuildEvent event) {
                }
                public void taskStarted(BuildEvent event) {
                }
                public void taskFinished(BuildEvent event) {
                }
                public void messageLogged(BuildEvent event) {
                }
            }); 
        }
    }
    protected void setIvyInstance(Ivy ivy) {
        getProject().addReference("ivy.instance", ivy);
    }
    
    protected void setResolved(ModuleDescriptor md) {
        getProject().addReference("ivy.resolved.descriptor", md);
    }
    
    protected void ensureResolved(boolean haltOnFailure, String org, String module) {
        ensureMessageInitialised();
        if (org != null  && module != null) {
            return;
        }
        Object reference = getProject().getReference("ivy.resolved.descriptor");
        if (reference == null) {
            Message.verbose("no resolved descriptor found: launching default resolve");
            IvyResolve resolve = new IvyResolve();
            resolve.setProject(getProject());
            resolve.setHaltonfailure(haltOnFailure);
            if (_validate != null) {
                resolve.setValidate(_validate.booleanValue());
            }
            resolve.execute();
        } 
    }

    protected String[] splitConfs(String conf) {
        String[] confs = conf.split(",");
        for (int i = 0; i < confs.length; i++) {
            confs[i] = confs[i].trim();
        }
        return confs;
    }

    protected String mergeConfs(String[] conf) {
        return StringUtils.join(conf, ", ");
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    protected Date getPubDate(String date, Date def) {
        if (date != null) {
            if ("now".equalsIgnoreCase(date)) {
                return new Date();
            }
            try {
                return DATE_FORMAT.parse(date);
            } catch (Exception ex) {
                throw new BuildException("publication date provided in bad format. should be yyyyMMddHHmmss and not "+date);
            }
        } else {
            return def;
        }
    }

    protected String getProperty(String value, Ivy ivy, String name) {
        if (value == null) {
            return getProperty(ivy, name);
        } else {
            value = ivy.substitute(value);
            Message.debug("parameter found as attribute value: "+name+"="+value);
            return value;
        }
    }
    
    protected String getProperty(Ivy ivy, String name) {        
        String val =  ivy.getVariable(name);        
        if (val == null) {
            val = ivy.substitute(getProject().getProperty(name));
            if (val != null) {
                Message.debug("parameter found as ant project property: "+name+"="+val);
            } else {
                Message.debug("parameter not found: "+name);
            }
        } else {
            val = ivy.substitute(val);
            Message.debug("parameter found as ivy variable: "+name+"="+val);
        }
        return val;
    }
    
    public void setProject(Project project) {
    	super.setProject(project);
    	IvyContext.getContext().set(ANT_PROJECT_CONTEXT_KEY, project);
    }
    
}
