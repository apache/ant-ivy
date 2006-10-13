/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyContext;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.report.ResolveReport;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.util.StringUtils;

/**
 * Base class for all ivy ant tasks, deal particularly with ivy instance storage in ant project.
 * 
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
        Object ref = getProject().getReference("ivy.instances");
        if (ref != null && !(ref instanceof Map)) {
            throw new BuildException("ivy problem with ant: ivy.instances reference is not a Map. Please do not sett ivy.instances reference in your ant project. current reference: "+ref+" class="+ref.getClass()+" classloader="+ref.getClass().getClassLoader());
        }
        Map instances = (Map) ref;
        if (instances == null || !instances.containsKey(Ivy.class)) {
            Message.verbose("no ivy instance found: auto configuring ivy");
            IvyConfigure configure = new IvyConfigure();
            configure.setProject(getProject());
            configure.execute();
            instances = (Map) getProject().getReference("ivy.instances");
            if (instances == null || !instances.containsKey(Ivy.class)) {
                throw new BuildException("ivy internal problem: impossible to get ivy instance after configure... maybe a classloader problem");
            }
        } 
        return (Ivy)instances.get(Ivy.class);
    }

    /** 
     * Every task MUST call ensureMessageInitialised when the execution method
     * starts (at least before doing any log in order to set the correct task
     * in the log.
     */
    protected void ensureMessageInitialised() {
        if (!Message.isInitialised()) { 
            Message.init(new AntMessageImpl(this));
        }

    }
    protected void setIvyInstance(Ivy ivy) {
    	// this reference is not used anymore, what is used is the instances map below
        getProject().addReference("ivy.instance", ivy);
        
        if (ivy != null) {
        	Message.debug("setting ivy.instance on "+getProject()+": "+ivy+" class="+ivy.getClass().getName()+" classloader="+ivy.getClass().getClassLoader());
        	// we keep a map of ivy instances per Ivy class, in case of multiple classloaders
        	Map instances = (Map) getProject().getReference("ivy.instances");
        	if (instances == null) {
        		instances = new HashMap();
        		getProject().addReference("ivy.instances", instances);
        	}
        	instances.put(ivy.getClass(), ivy);
        }
    }
    
    protected void setResolved(ResolveReport report, boolean keep) {
    	ModuleDescriptor md = report.getModuleDescriptor();
    	String[] confs = report.getConfigurations();
    	if (keep) {
	        getProject().addReference("ivy.resolved.report", report);
	        getProject().addReference("ivy.resolved.configurations.ref", confs);
	        getProject().addReference("ivy.resolved.descriptor", md);
    	}
    	String suffix = md.getModuleRevisionId().getModuleId().getOrganisation()+"."+md.getModuleRevisionId().getModuleId().getName();
        getProject().addReference("ivy.resolved.report."+suffix, report);
        getProject().addReference("ivy.resolved.descriptor."+suffix, md);
        getProject().addReference("ivy.resolved.configurations.ref."+suffix, confs);
    }
    
	protected void ensureResolved(boolean haltOnFailure, boolean useOrigin, String org, String module) {
    	ensureResolved(haltOnFailure, useOrigin, true, org, module, null);
    }
    protected void ensureResolved(boolean haltOnFailure, boolean useOrigin, boolean transitive, String org, String module, String conf) {
        ensureMessageInitialised();
//        if (org != null  && module != null) {
//            return;
//        }
        String[] confs = getConfsToResolve(org, module, conf, false);
        
        if (confs.length > 0)  {
        	IvyResolve resolve = createResolve(haltOnFailure, useOrigin);
        	resolve.setTransitive(transitive);
        	resolve.setConf(StringUtils.join(confs, ", "));
        	resolve.execute();
        } 
    }
    
    protected String[] getConfsToResolve(String org, String module, String conf, boolean strict) {
        ModuleDescriptor reference = (ModuleDescriptor) getResolvedDescriptor(org, module, strict); 
		Message.debug("calculating configurations to resolve");
        
        if (reference == null)  {
    		Message.debug("module not yet resolved, all confs still need to be resolved");
        	if (conf == null) {
        		return new String[] {"*"};
        	} else {
        		return splitConfs(conf);
        	}
        } else if (conf != null) {
        	String[] rconfs = getResolvedConfigurations(org, module, strict);
        	String[] confs;
        	if ("*".equals(conf)) {
        		confs = reference.getConfigurationsNames();
        	} else {
        		confs = splitConfs(conf);
        	}
    		HashSet rconfsSet = new HashSet(Arrays.asList(rconfs));
			HashSet confsSet = new HashSet(Arrays.asList(confs));
			Message.debug("resolved configurations:   "+rconfsSet);
			Message.debug("asked configurations:      "+confsSet);
			confsSet.removeAll(rconfsSet);
			Message.debug("to resolve configurations: "+confsSet);
			return (String[]) confsSet.toArray(new String[confsSet.size()]);
        } else {
    		Message.debug("module already resolved, no configuration to resolve");
        	return new String[0];
        }
    	
    }
    
    protected String[] getResolvedConfigurations(String org, String module, boolean strict) {
		return (String[]) getReference("ivy.resolved.configurations.ref", org, module, strict);
	}
    
	protected Object getResolvedDescriptor(String org, String module) {
		return getResolvedDescriptor(org, module, false);
	}
    
	protected Object getResolvedDescriptor(String org, String module, boolean strict) {
		return getReference("ivy.resolved.descriptor", org, module, strict);
	}
	private Object getReference(String prefix, String org, String module, boolean strict) {
		Object reference = null;
		if (org != null && module != null) {
			reference = getProject().getReference(prefix+"."+org+"."+module);
		}
        if (!strict && reference == null) {
        	reference = getProject().getReference(prefix);
        }
		return reference;
	}
    
	protected ResolveReport getResolvedReport(String org, String module) {
		return getResolvedReport(org, module, false);
	}
	protected ResolveReport getResolvedReport(String org, String module, boolean strict) {
		return (ResolveReport) getReference("ivy.resolved.report", org, module, strict);
	}
    
	protected IvyResolve createResolve(boolean haltOnFailure, boolean useOrigin) {
		Message.verbose("no resolved descriptor found: launching default resolve");
		IvyResolve resolve = new IvyResolve();
		resolve.setProject(getProject());
		resolve.setHaltonfailure(haltOnFailure);
		resolve.setUseOrigin(useOrigin);
		if (_validate != null) {
		    resolve.setValidate(_validate.booleanValue());
		}
		return resolve;
	}

    protected boolean shouldResolve(String org, String module) {
        ensureMessageInitialised();
        if (org != null  && module != null) {
            return false;
        }
        Object reference = getResolvedDescriptor(org, module); 
        return (reference == null);
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
