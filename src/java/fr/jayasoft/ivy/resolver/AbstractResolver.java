/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.Artifact;
import fr.jayasoft.ivy.DependencyResolver;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyAware;
import fr.jayasoft.ivy.ResolveData;
import fr.jayasoft.ivy.report.ArtifactDownloadReport;
import fr.jayasoft.ivy.report.DownloadReport;
import fr.jayasoft.ivy.report.DownloadStatus;
import fr.jayasoft.ivy.util.Message;

/**
 * This abstract resolver only provides handling for resolver name
 */
public abstract class AbstractResolver implements DependencyResolver, IvyAware {

    /**
     * True if parsed ivy files should be validated against xsd, false if they should not,
     * null if default behaviour should be used
     */
    private Boolean _validate = null;
    private String _name;
    private Ivy _ivy;

    public Ivy getIvy() {
        return _ivy;
    }    

    public void setIvy(Ivy ivy) {
        _ivy = ivy;
    }
    
    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    protected boolean doValidate(ResolveData data) {
        if (_validate != null) {
            return _validate.booleanValue();
        } else {
            return data.isValidate();
        }
    }

    public boolean isValidate() {
        return _validate == null ? true: _validate.booleanValue();
    }
    

    public void setValidate(boolean validate) {
        _validate = Boolean.valueOf(validate);
    }
    
    public void reportFailure() {
        Message.verbose("no failure report implemented by "+getName());
    }

    public void reportFailure(Artifact art) {
        Message.verbose("no failure report implemented by "+getName());
    }

    public OrganisationEntry[] listOrganisations() {
        return new OrganisationEntry[0];
    }
    public ModuleEntry[] listModules(OrganisationEntry org) {
        return new ModuleEntry[0];
    }
    public RevisionEntry[] listRevisions(ModuleEntry module) {
        return new RevisionEntry[0];
    }

    public String toString() {
        return getName();
    }
    public void dumpConfig() {
        Message.verbose("\t"+getName()+" ["+getTypeName()+"]");
    }

    public String getTypeName() {
        return getClass().getName();
    }
    /**
     * Default implementation actually download the artifact
     * Subclasses should overwrite this to avoid the download
     */
    public boolean exists(Artifact artifact) {
        DownloadReport dr = download(new Artifact[] {artifact}, getIvy(), getIvy().getDefaultCache());
        ArtifactDownloadReport adr = dr.getArtifactReport(artifact);
        return adr.getDownloadStatus() != DownloadStatus.FAILED;
    }
}
