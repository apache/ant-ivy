package org.apache.ivy.core.module.descriptor;

import org.apache.ivy.core.module.id.ModuleRevisionId;

import java.util.ArrayList;
import java.util.List;

public class DefaultExtendsDescriptor implements ExtendsDescriptor {

    private ModuleRevisionId parentRevisionId;
    private ModuleRevisionId resolvedParentRevisionId;
    private String location;
    private List extendsTypes;

    public DefaultExtendsDescriptor(ModuleRevisionId parentRevisionId,
                                    ModuleRevisionId resolvedParentRevisionId,
                                    String location, String[] types) {
        this.parentRevisionId = parentRevisionId;
        this.resolvedParentRevisionId = resolvedParentRevisionId;
        this.location = location;
        this.extendsTypes = new ArrayList(types.length);
        for (int i = 0; i < types.length; ++i) {
            extendsTypes.add(types[i]);
        }
    }

    public ModuleRevisionId getParentRevisionId() {
        return parentRevisionId;
    }

    public ModuleRevisionId getResolvedParentRevisionId() {
        return resolvedParentRevisionId;
    }

    public String getLocation() {
        return location;
    }

    public String[] getExtendsTypes() {
        return (String[])extendsTypes.toArray(new String[extendsTypes.size()]);
    }

    public boolean isAllInherited() {
        return extendsTypes.contains("all");
    }

    public boolean isInfoInherited() {
        return isAllInherited() || extendsTypes.contains("info");
    }

    public boolean isDescriptionInherited() {
        return isAllInherited() || extendsTypes.contains("description");
    }

    public boolean areConfigurationsInherited() {
        return isAllInherited() || extendsTypes.contains("configurations");
    }

    public boolean areDependenciesInherited() {
        return isAllInherited() || extendsTypes.contains("dependencies");
    }
}
