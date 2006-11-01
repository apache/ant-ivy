/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.ArtifactRevisionId;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;

/**
 * This task allow to publish a module revision to an Ivy repository.
 * 
 * @author Xavier Hanin
 *
 */
public class IvyPublish extends IvyTask {
    private String  _organisation;
    private String  _module;
    private String  _revision;
    private String  _pubRevision;
    private File 	_cache; 
    private String 	_srcivypattern;
    private String 	_status;
    private String 	_conf=null;
    private String 	_pubdate;
    private String  _deliverTarget;
    private String  _publishResolverName = null;
    private List _artifactspattern = new ArrayList();
    private File    _deliveryList;
    private boolean _publishivy = true;
    private boolean _warnonmissing = true;
    private boolean _haltonmissing = true;
    private boolean _overwrite = false;
    private boolean _update = false;
    private boolean _replacedynamicrev = true;
	private boolean _forcedeliver;
	private Collection _artifacts = new ArrayList();
    
    public File getCache() {
        return _cache;
    }
    public void setCache(File cache) {
        _cache = cache;
    }
    public String getSrcivypattern() {
        return _srcivypattern;
    }
    public void setSrcivypattern(String destivypattern) {
        _srcivypattern = destivypattern;
    }
    /**
     * @deprecated use getSrcivypattern instead
     * @return
     */
    public String getDeliverivypattern() {
        return _srcivypattern;
    }
    /**
     * @deprecated use setSrcivypattern instead
     * @return
     */
    public void setDeliverivypattern(String destivypattern) {
        _srcivypattern = destivypattern;
    }
    public String getModule() {
        return _module;
    }
    public void setModule(String module) {
        _module = module;
    }
    public String getOrganisation() {
        return _organisation;
    }
    public void setOrganisation(String organisation) {
        _organisation = organisation;
    }
    public String getPubdate() {
        return _pubdate;
    }
    public void setPubdate(String pubdate) {
        _pubdate = pubdate;
    }
    public String getPubrevision() {
        return _pubRevision;
    }
    public void setPubrevision(String pubRevision) {
        _pubRevision = pubRevision;
    }
    public String getRevision() {
        return _revision;
    }
    public void setRevision(String revision) {
        _revision = revision;
    }
    public String getStatus() {
        return _status;
    }
    public void setStatus(String status) {
        _status = status;
    }
    public void setConf(String conf) {
        _conf = conf;
    }
    public void setDelivertarget(String deliverTarget) {
        _deliverTarget = deliverTarget;
    }
    public void setDeliveryList(File deliveryList) {
        _deliveryList = deliveryList;
    }
    public String getResolver() {
        return _publishResolverName;
    }    
    public void setResolver(String publishResolverName) {
        _publishResolverName = publishResolverName;
    }
    public String getArtifactspattern() {
        return (String) (_artifactspattern.isEmpty()?null:_artifactspattern.get(0));
    }    
    public void setArtifactspattern(String artifactsPattern) {
        _artifactspattern.clear();
        _artifactspattern.add(artifactsPattern);
    }
	public void addArtifactspattern(String artifactsPattern) {
		_artifactspattern.add(artifactsPattern);
	}
	public void addConfiguredArtifacts(ArtifactsPattern p) {
		_artifactspattern.add(p.getPattern());
	}
    public boolean isReplacedynamicrev() {
        return _replacedynamicrev;
    }
    public void setReplacedynamicrev(boolean replacedynamicrev) {
        _replacedynamicrev = replacedynamicrev;
    }
    
    public void execute() throws BuildException {
        Ivy ivy = getIvyInstance();
        _organisation = getProperty(_organisation, ivy, "ivy.organisation");
        _module = getProperty(_module, ivy, "ivy.module");
        _revision = getProperty(_revision, ivy, "ivy.revision");
        _pubRevision = getProperty(_pubRevision, ivy, "ivy.deliver.revision");
        if (_cache == null) {
            _cache = ivy.getDefaultCache();
        }
        if (_artifactspattern.isEmpty()) {
        	String p = getProperty(null, ivy, "ivy.publish.src.artifacts.pattern");
        	if (p != null) {
        		_artifactspattern.add(p);
        	}
        }
        if (_srcivypattern == null) {
            _srcivypattern = getArtifactspattern();
        }
        _status = getProperty(_status, ivy, "ivy.status");
        if (_organisation == null) {
            throw new BuildException("no organisation provided for ivy publish task: It can either be set explicitely via the attribute 'organisation' or via 'ivy.organisation' property or a prior call to <resolve/>");
        }
        if (_module == null) {
            throw new BuildException("no module name provided for ivy publish task: It can either be set explicitely via the attribute 'module' or via 'ivy.module' property or a prior call to <resolve/>");
        }
        if (_revision == null) {
            throw new BuildException("no module revision provided for ivy publish task: It can either be set explicitely via the attribute 'revision' or via 'ivy.revision' property or a prior call to <resolve/>");
        }
        if (_artifactspattern.isEmpty()) {
            throw new BuildException("no artifacts pattern: either provide it through parameter or through ivy.publish.src.artifacts.pattern property");
        }
        if (_publishResolverName == null) {
            throw new BuildException("no publish deliver name: please provide it through parameter 'resolver'");
        }
        if ("working".equals(_revision)) {
        	_revision = "working@"+Ivy.getLocalHostName();
        }
        Date pubdate = getPubDate(_pubdate, new Date());
        if (_pubRevision == null) {
            if (_revision.startsWith("working@")) {
                _pubRevision = Ivy.DATE_FORMAT.format(pubdate);
            } else {
                _pubRevision = _revision;
            }
        }
        if (_status == null) {
            throw new BuildException("no status provided: either provide it as parameter or through the ivy.status.default property");
        }
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(_organisation, _module, _revision);
        try {
            File ivyFile = new File(IvyPatternHelper.substitute(_srcivypattern, _organisation, _module, _pubRevision, "ivy", "ivy", "xml"));
            if (null!=_conf && isPublishivy()) {
            	if (isPublishivy()) {
            		Message.warn("Impossible to publish ivy file when conf is specified");
            		Message.warn("Please, set publishivy to false");
            		setPublishivy(false);
            	}
            }
            if (_publishivy && (!ivyFile.exists() || _forcedeliver)) {
                IvyDeliver deliver = new IvyDeliver();
                deliver.setProject(getProject());
                deliver.setCache(getCache());
                deliver.setDeliverpattern(getSrcivypattern());
                deliver.setDelivertarget(_deliverTarget);
                deliver.setDeliveryList(_deliveryList);
                deliver.setModule(getModule());
                deliver.setOrganisation(getOrganisation());
                deliver.setPubdate(Ivy.DATE_FORMAT.format(pubdate));
                deliver.setPubrevision(getPubrevision());
                deliver.setRevision(getRevision());
                deliver.setStatus(getStatus());
                deliver.setValidate(doValidate(ivy));
                deliver.setReplacedynamicrev(isReplacedynamicrev());
                
                deliver.execute();
            }
            
            Collection missing = ivy.publish(
            		mrid, 
            		_pubRevision, 
            		_cache, 
            		_artifactspattern, 
            		_publishResolverName, 
            		_publishivy?_srcivypattern:null, 
            		getStatus(), 
            		pubdate, 
            		(Artifact[]) _artifacts.toArray(new Artifact[_artifacts.size()]), doValidate(ivy), 
            		_overwrite, 
            		_update,
            		_conf);
            if (_warnonmissing) {
                for (Iterator iter = missing.iterator(); iter.hasNext();) {
                    Artifact artifact = (Artifact)iter.next();
                    Message.warn("missing artifact: "+artifact);
                }
            }
            if (_haltonmissing && !missing.isEmpty()) {
                throw new BuildException("missing published artifacts for "+mrid+": "+missing);
            }
            
        } catch (Exception e) {
            throw new BuildException("impossible to publish artifacts for "+mrid+": "+e, e);
        }
    }
    public PublishArtifact createArtifact() {
    	PublishArtifact art = new PublishArtifact();
    	_artifacts .add(art);
		return art;
    }
    public boolean isPublishivy() {
        return _publishivy;
    }
    
    public void setPublishivy(boolean publishivy) {
        _publishivy = publishivy;
    }
    public boolean isWarnonmissing() {
        return _warnonmissing;
    }
    
    public void setWarnonmissing(boolean warnonmissing) {
        _warnonmissing = warnonmissing;
    }
    public boolean isHaltonmissing() {
        return _haltonmissing;
    }
    
    public void setHaltonmissing(boolean haltonmissing) {
        _haltonmissing = haltonmissing;
    }
    public boolean isOverwrite() {
        return _overwrite;
    }
    public void setOverwrite(boolean overwrite) {
        _overwrite = overwrite;
    }
	public void setForcedeliver(boolean b) {
		_forcedeliver = b;
	}
	public boolean isForcedeliver() {
		return _forcedeliver;
	}
	public boolean isUpdate() {
		return _update;
	}
	public void setUpdate(boolean update) {
		_update = update;
	}
	public class PublishArtifact implements Artifact {
		private String _ext;
		private String _name;
		private String _type;

		public String[] getConfigurations() {
			return null;
		}

		public String getExt() {
			return _ext==null?_type:_ext;
		}

		public ArtifactRevisionId getId() {
			return null;
		}

		public ModuleRevisionId getModuleRevisionId() {
			return null;
		}

		public String getName() {
			return _name;
		}

		public Date getPublicationDate() {
			return null;
		}

		public String getType() {
			return _type;
		}

		public URL getUrl() {
			return null;
		}

		public void setExt(String ext) {
			_ext = ext;
		}

		public void setName(String name) {
			_name = name;
		}

		public void setType(String type) {
			_type = type;
		}

		public String getAttribute(String attName) {
			return null;
		}

		public Map getAttributes() {
			return new HashMap();
		}

		public String getExtraAttribute(String attName) {
			return null;
		}

		public Map getExtraAttributes() {
			return new HashMap();
		}

		public String getStandardAttribute(String attName) {
			return null;
		}

		public Map getStandardAttributes() {
			return new HashMap();
		}
	}
	public static class ArtifactsPattern {
		private String _pattern;

		public String getPattern() {
			return _pattern;
		}

		public void setPattern(String pattern) {
			_pattern = pattern;
		}
	}
}
