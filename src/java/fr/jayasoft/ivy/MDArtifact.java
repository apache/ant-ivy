/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author x.hanin
 *
 */
public class MDArtifact extends AbstractArtifact {
    private ModuleDescriptor _md;
    private String _name;
    private String _type;
    private String _ext;
    private List  _confs = new ArrayList();
    private ArtifactRevisionId _arid;

    public MDArtifact(ModuleDescriptor md, String name, String type, String ext) {
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
    }
    
    public ModuleRevisionId getModuleRevisionId() {
        return _md.getResolvedModuleRevisionId();
    }
    
    public Date getPublicationDate() {
        return _md.getResolvedPublicationDate();
    }
    public ArtifactRevisionId getId() {
        if (_arid == null) {
            _arid = ArtifactRevisionId.newInstance(_md.getResolvedModuleRevisionId(), _name, _type, _ext);
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
    
}
