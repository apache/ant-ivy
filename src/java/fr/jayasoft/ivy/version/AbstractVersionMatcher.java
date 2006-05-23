package fr.jayasoft.ivy.version;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyAware;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;

public abstract class AbstractVersionMatcher implements VersionMatcher, IvyAware {
	private String _name;
	private Ivy _ivy;
	
	public AbstractVersionMatcher() {
	}

	public AbstractVersionMatcher(String name) {
		_name = name;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}


    public boolean needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        return false;
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleDescriptor foundMD) {
        return accept(askedMrid, foundMD.getResolvedModuleRevisionId());
    }
    
    public String toString() {
    	return getName();
    }

	public Ivy getIvy() {
		return _ivy;
	}

	public void setIvy(Ivy ivy) {
		_ivy = ivy;
	}

}
