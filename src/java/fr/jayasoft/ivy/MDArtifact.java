/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author x.hanin
 *
 */
public class MDArtifact extends AbstractArtifact {

    public static Artifact newIvyArtifact(ModuleDescriptor md) {
        return new MDArtifact(md, "ivy", "ivy", "xml");
    }
    
    private ModuleDescriptor _md;
    private String _name;
    private String _type;
    private String _ext;
    private List  _confs = new ArrayList();
    private ArtifactRevisionId _arid;
    private Map _extraAttributes = null;
	private URL _url;

    public MDArtifact(ModuleDescriptor md, String name, String type, String ext) {
        this(md, name, type, ext, null, null);
    }
    public MDArtifact(ModuleDescriptor md, String name, String type, String ext, URL url, Map extraAttributes) {
        if (md == null) {
            throw new NullPointerException("null module descriptor not allowed");
        }
        if (name == null) {
            throw new NullPointerException("null name not allowed");
        }
        if (type == null) {
            throw new NullPointerException("null type not allowed");
        }
        if (ext == null) {
            throw new NullPointerException("null ext not allowed");
        }
        _md = md;
        _name = name;
        _type = type;
        _ext = ext;
        _url = url;
        _extraAttributes = extraAttributes;
    }
    
    public ModuleRevisionId getModuleRevisionId() {
        return _md.getResolvedModuleRevisionId();
    }
    
    public Date getPublicationDate() {
        return _md.getResolvedPublicationDate();
    }
    public ArtifactRevisionId getId() {
        if (_arid == null) {
            _arid = ArtifactRevisionId.newInstance(_md.getResolvedModuleRevisionId(), _name, _type, _ext, _extraAttributes);
        }
        return _arid;
    }    

    public String getName() {
        return _name;
    }

    public String getType() {
        return _type;
    }

    public String getExt() {
        return _ext;
    }

    public String[] getConfigurations() {
        return (String[])_confs.toArray(new String[_confs.size()]);
    }

    public void addConfiguration(String conf) {
        _confs.add(conf);
    }
    
	public URL getUrl() {
		return _url;
	}
    
}
