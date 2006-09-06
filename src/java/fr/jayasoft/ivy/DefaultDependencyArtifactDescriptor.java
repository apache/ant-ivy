/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import fr.jayasoft.ivy.matcher.PatternMatcher;

public class DefaultDependencyArtifactDescriptor implements DependencyArtifactDescriptor {

    private DefaultDependencyDescriptor _dd;
    private ArtifactId _id;
    private Collection _confs = new ArrayList();
    private boolean _includes;
    private PatternMatcher _patternMatcher;
	private URL _url;
    

    
    public DefaultDependencyArtifactDescriptor(DefaultDependencyDescriptor dd,
            String name, String type, String ext, boolean includes, PatternMatcher matcher) {
		this(dd, name, type, ext, null, includes, matcher);
	}
    /**
     * @param dd
     * @param name
     * @param type
     * @param url 
     */
    public DefaultDependencyArtifactDescriptor(DefaultDependencyDescriptor dd,
            String name, String type, String ext, URL url, boolean includes, PatternMatcher matcher) {
        if (dd == null) {
            throw new NullPointerException("dependency descriptor must not be null");
        }
        if (name == null) {
            throw new NullPointerException("name must not be null");
        }
        if (type == null) {
            throw new NullPointerException("type must not be null");
        }
        _dd = dd;
        _id = new ArtifactId(dd.getDependencyId(), name, type, ext);
        _includes = includes;
        _url = url;
        _patternMatcher = matcher;
    }
    
    public DefaultDependencyArtifactDescriptor(DefaultDependencyDescriptor dd, ArtifactId aid, boolean includes, PatternMatcher matcher) {
        if (dd == null) {
            throw new NullPointerException("dependency descriptor must not be null");
        }
        _dd = dd;
        _id = aid;
        _includes = includes;
        _patternMatcher = matcher;
    }

	public boolean equals(Object obj) {
        if (!(obj instanceof DependencyArtifactDescriptor)) {
            return false;
        }
        DependencyArtifactDescriptor dad = (DependencyArtifactDescriptor)obj;
        return getId().equals(dad.getId());
    }
    
    public int hashCode() {
        return getId().hashCode();
    }
    
    /**
     * Add a configuration for this artifact (includes or excludes depending on this type dependency artifact descriptor).
     * This method also updates the corresponding dependency descriptor
     * @param conf
     */
    public void addConfiguration(String conf) {
        _confs.add(conf);
        if (_includes) {
            _dd.addDependencyArtifactIncludes(conf, this);
        } else {
            _dd.addDependencyArtifactExcludes(conf, this);
        }
    }
        
    public DependencyDescriptor getDependency() {
        return _dd;
    }
    
    public ArtifactId getId() {
        return _id;
    }

    public String getName() {
        return _id.getName();
    }

    public String getType() {
        return _id.getType();
    }
    public String getExt() {
        return _id.getExt();
    }

    public String[] getConfigurations() {
        return (String[])_confs.toArray(new String[_confs.size()]);
    }

    public PatternMatcher getMatcher() {
        return _patternMatcher;
    }

	public URL getUrl() {
		return _url;
	}

	public String toString() {
		return (_includes?"I":"E")+":"+_id+"("+_confs+")"+(_url==null?"":_url.toString());
	}
}
