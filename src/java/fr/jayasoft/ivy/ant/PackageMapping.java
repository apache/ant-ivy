/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import fr.jayasoft.ivy.ModuleRevisionId;


/**
 * Describes a mapping between a package name and an org name revision uple
 * 
 * @author Xavier Hanin
 *
 */
public class PackageMapping {
    private String _package;
    private String _organisation;
    private String _module;
    private String _revision;
    
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
    public String getRevision() {
        return _revision;
    }
    public void setRevision(String revision) {
        _revision = revision;
    }
    public String getPackage() {
        return _package;
    }
    public void setPackage(String package1) {
        _package = package1;
    }
    public ModuleRevisionId getModuleRevisionId() {
        return ModuleRevisionId.newInstance(_organisation, _module, _revision);
    }
}
