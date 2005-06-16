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
import fr.jayasoft.ivy.ModuleDescriptor;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * @author Hanin
 *
 */
public class IvyTask extends Task {
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
        Object reference = getProject().getReference("ivy.instance");
        if (reference == null) {
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

    protected void setIvyInstance(Ivy ivy) {
        getProject().addReference("ivy.instance", ivy);
    }
    
    protected void setResolved(ModuleDescriptor md) {
        getProject().addReference("ivy.resolved.descriptor", md);
    }
    
    protected void ensureResolved(boolean haltOnFailure) {
        Object reference = getProject().getReference("ivy.resolved.descriptor");
        if (reference == null) {
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
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < conf.length; i++) {
            buf.append(conf[i]).append(", ");
        }
        if (conf.length > 0) { 
            buf.setLength(buf.length() - 2); // delete last comma
        }
        return buf.toString();
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
            return ivy.substitute(value);
        }
    }
    
    protected String getProperty(Ivy ivy, String name) {        
        String val =  ivy.getVariable(name);
        val = val == null ? getProject().getProperty(name) : val;
        return val == null ? null : ivy.substitute(val);
    }
    
}
