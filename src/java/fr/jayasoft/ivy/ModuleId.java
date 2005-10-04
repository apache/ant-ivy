/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;


/**
 * @author x.hanin
 *
 */
public class ModuleId {
    private String _organisation;
    private String _name;

    public ModuleId(String organisation, String name) {
        if (name == null) {
            throw new IllegalArgumentException("null name not allowed");
        }
        _organisation = organisation;
        _name = name;
    }

    public String getName() {
        return _name;
    }
    public String getOrganisation() {
        return _organisation;
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof ModuleId)) {
            return false;
        }
        ModuleId other = (ModuleId)obj;
        return other._organisation.equals(_organisation) && other._name.equals(_name);
    }
    public int hashCode() {
        int hash = 31;
        hash = hash * 13 + _organisation.hashCode();
        hash = hash * 13 + _name.hashCode();
        return hash;
    }
    public String toString() {
        return "[ "+_organisation+" | "+_name+" ]";
    }
}
