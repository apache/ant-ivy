/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.xml.XmlModuleDescriptorParser;

public class IvyBuildList extends IvyTask {
    private List _buildFiles = new ArrayList(); // List (FileSet)
    private String _reference;
    private boolean _haltOnError = true;
    private String _ivyFilePath;
    

    public void addFileset(FileSet buildFiles) {
        _buildFiles.add(buildFiles);
    }
    
    public String getReference() {
        return _reference;
    }

    public void setReference(String reference) {
        _reference = reference;
    }

    public void execute() throws BuildException {   
        if (_reference == null) {
            throw new BuildException("reference should be provided in ivy build list");
        }
        if (_buildFiles.isEmpty()) {
            throw new BuildException("at least one nested fileset should be provided in ivy build list");
        }

        _ivyFilePath = getProperty(_ivyFilePath, getIvyInstance(), "ivy.buildlist.ivyfilepath");

        Path path = new Path(getProject());
        
        Map buildFiles = new HashMap(); // Map (ModuleDescriptor -> File buildFile)
        Collection mds = new ArrayList();
        List independent = new ArrayList();
        
        for (ListIterator iter = _buildFiles.listIterator(); iter.hasNext();) {
            FileSet fs = (FileSet)iter.next();
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            String[] builds = ds.getIncludedFiles();
            for (int i = 0; i < builds.length; i++) {
                File buildFile = new File(ds.getBasedir(), builds[i]);
                File ivyFile = getIvyFileFor(buildFile);
                try {
                    ModuleDescriptor md = XmlModuleDescriptorParser.parseDescriptor(getIvyInstance(), ivyFile.toURL(), isValidate());
                    buildFiles.put(md, buildFile);
                    mds.add(md);
                } catch (Exception ex) {
                    if (_haltOnError) {
                        throw new BuildException("impossible to parse ivy file for "+buildFile+": ivyfile="+ivyFile+" exception="+ex.getMessage(), ex);
                    } else {
                        Message.warn("impossible to parse ivy file for "+buildFile+": ivyfile="+ivyFile+" exception="+ex.getMessage());
                        independent.add(buildFile);
                    }
                }
            }
        }
        
        List sortedModules = Ivy.sortModuleDescriptors(mds);
        
        for (ListIterator iter = independent.listIterator(); iter.hasNext();) {
            File buildFile = (File)iter.next();
            addBuildFile(path, buildFile);
        }
        
        for (ListIterator iter = sortedModules.listIterator(); iter.hasNext();) {
            ModuleDescriptor md = (ModuleDescriptor)iter.next();
            File buildFile = (File)buildFiles.get(md);
            addBuildFile(path, buildFile);
        }
        
        getProject().addReference(getReference(), path);
    }

    private void addBuildFile(Path path, File buildFile) {
        FileList fl = new FileList();
        fl.setDir(buildFile.getParentFile());
        FileList.FileName fileName = new FileList.FileName();
        fileName.setName(buildFile.getName());
        fl.addConfiguredFile(fileName);
        path.addFilelist(fl);
    }

    private File getIvyFileFor(File buildFile) {
        return new File(buildFile.getParentFile(), _ivyFilePath);
    }

    public boolean isHaltonerror() {
        return _haltOnError;
    }

    public void setHaltonerror(boolean haltOnError) {
        _haltOnError = haltOnError;
    }

    public String getIvyfilepath() {
        return _ivyFilePath;
    }

    public void setIvyfilepath(String ivyFilePath) {
        _ivyFilePath = ivyFilePath;
    }

}
