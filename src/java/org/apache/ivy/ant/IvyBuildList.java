/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.util.Message;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;


/**
 * Creates an ant filelist of files (usually build.xml) ordered according to the dependencies declared in ivy files.
 * 
 * @author Xavier Hanin
 */
public class IvyBuildList extends IvyTask {
    private List _buildFiles = new ArrayList(); // List (FileSet)
    private String _reference;
    private boolean _haltOnError = true;
    private boolean _skipBuildWithoutIvy = false;
    private boolean _reverse = false;
    private String _ivyFilePath;
    private String _root = "*";
    private boolean _excludeRoot = false;
    private String _leaf = "*";
    private String _delimiter = ",";
    private boolean _excludeLeaf = false;


    public void addFileset(FileSet buildFiles) {
        _buildFiles.add(buildFiles);
    }

    public String getReference() {
        return _reference;
    }

    public void setReference(String reference) {
        _reference = reference;
    }

    public String getRoot() {
        return _root;
    }

    public void setRoot(String root) {
        _root = root;
    }

    public boolean isExcludeRoot() {
        return _excludeRoot;
    }

    public void setExcludeRoot(boolean root) {
        _excludeRoot = root;
    }

	public String getLeaf() {
		return _leaf;
	}

	public void setLeaf(String leaf) {
		_leaf = leaf;
	}

	public boolean isExcludeLeaf() {
		return _excludeLeaf;
	}

	public void setExcludeLeaf(boolean excludeLeaf) {
		_excludeLeaf = excludeLeaf;
	}

	public String getDelimiter() {
		return _delimiter;
	}
	
	public void setDelimiter(String delimiter) {
		_delimiter = delimiter;
	}
	
    public void execute() throws BuildException {
        if (_reference == null) {
            throw new BuildException("reference should be provided in ivy build list");
        }
        if (_buildFiles.isEmpty()) {
            throw new BuildException("at least one nested fileset should be provided in ivy build list");
        }

        Ivy ivy = getIvyInstance();
        IvySettings settings = ivy.getSettings();
        
		_ivyFilePath = getProperty(_ivyFilePath, settings, "ivy.buildlist.ivyfilepath");

        Path path = new Path(getProject());

        Map buildFiles = new HashMap(); // Map (ModuleDescriptor -> File buildFile)
        Map mdsMap = new LinkedHashMap(); // Map (String moduleName -> ModuleDescriptor)
        List independent = new ArrayList();

        Set rootModuleNames = new LinkedHashSet();
        if (!"*".equals(_root)) {
        	StringTokenizer st = new StringTokenizer(_root, _delimiter);
        	while (st.hasMoreTokens()) {
        		rootModuleNames.add(st.nextToken());
        	}
        }
        
        Set leafModuleNames = new LinkedHashSet();
        if (! "*".equals(_leaf)) {
        	StringTokenizer st = new StringTokenizer(_leaf, _delimiter);
        	while (st.hasMoreTokens()) {
        		leafModuleNames.add(st.nextToken());
        	}
        }
        
        for (ListIterator iter = _buildFiles.listIterator(); iter.hasNext();) {
            FileSet fs = (FileSet)iter.next();
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            String[] builds = ds.getIncludedFiles();
            for (int i = 0; i < builds.length; i++) {
                File buildFile = new File(ds.getBasedir(), builds[i]);
                File ivyFile = getIvyFileFor(buildFile);
                if (!ivyFile.exists()) {
                    if (_skipBuildWithoutIvy) {
                        Message.debug("skipping "+buildFile+": ivy file "+ivyFile+" doesn't exist");
                    } else {
                        Message.verbose("no ivy file for "+buildFile+": ivyfile="+ivyFile+": adding it at the beginning of the path");
                        Message.verbose("\t(set skipbuildwithoutivy to true if you don't want this file to be added to the path)");
                        independent.add(buildFile);
                    }
                } else {
                    try {
                        ModuleDescriptor md = ModuleDescriptorParserRegistry.getInstance().parseDescriptor(settings, ivyFile.toURL(), doValidate(settings));
                        buildFiles.put(md, buildFile);
                        mdsMap.put(md.getModuleRevisionId().getModuleId().getName(), md);
                    } catch (Exception ex) {
                        if (_haltOnError) {
                            throw new BuildException("impossible to parse ivy file for "+buildFile+": ivyfile="+ivyFile+" exception="+ex, ex);
                        } else {
                            Message.warn("impossible to parse ivy file for "+buildFile+": ivyfile="+ivyFile+" exception="+ex.getMessage());
                            Message.info("\t=> adding it at the beginning of the path");
                            independent.add(buildFile);
                        }
                    }
                }
            }
        }
        
        List leafModuleDescriptors = convertModuleNamesToModuleDescriptors(mdsMap, leafModuleNames, "leaf");
        List rootModuleDescriptors = convertModuleNamesToModuleDescriptors(mdsMap, rootModuleNames, "root");
        
        Collection mds = new ArrayList(mdsMap.values());
        if (! rootModuleDescriptors.isEmpty()) {
            Message.info("Filtering modules based on roots " + rootModuleNames);
            mds = filterModulesFromRoot(mds, rootModuleDescriptors);
        }
        if (! leafModuleDescriptors.isEmpty()) {
            Message.info("Filtering modules based on leafs " + leafModuleNames);
            mds = filterModulesFromLeaf(mds, leafModuleDescriptors);
        }

		List sortedModules = ivy.sortModuleDescriptors(mds);

        for (ListIterator iter = independent.listIterator(); iter.hasNext();) {
            File buildFile = (File)iter.next();
            addBuildFile(path, buildFile);
        }
        if (isReverse()) {
            Collections.reverse(sortedModules);
        }
        StringBuffer order = new StringBuffer();
        for (ListIterator iter = sortedModules.listIterator(); iter.hasNext();) {
            ModuleDescriptor md = (ModuleDescriptor)iter.next();
            order.append(md.getModuleRevisionId().getModuleId());
            if (iter.hasNext()) {
            	order.append(", ");
            }
            File buildFile = (File)buildFiles.get(md);
            addBuildFile(path, buildFile);
        }

        getProject().addReference(getReference(), path);
        getProject().setProperty("ivy.sorted.modules", order.toString());
    }

	private List convertModuleNamesToModuleDescriptors(Map mdsMap, Set moduleNames, String kind) {
		List mds = new ArrayList();
		for (Iterator iter = moduleNames.iterator(); iter.hasNext();) {
			String name = (String) iter.next();
            ModuleDescriptor md = (ModuleDescriptor) mdsMap.get(name);
            if (md == null) {
            	throw new BuildException("unable to find "+kind+" module " +  name + " in build fileset");
            }
			mds.add(md);
		}
		return mds;
	}

    /**
     * Returns a collection of ModuleDescriptors that are conatined in the input
     * collection of ModuleDescriptors and upon which the root module depends
     *
     * @param mds input collection of ModuleDescriptors
     * @param rootmd root module
     * @return filtered list of modules
     */
    private Collection filterModulesFromRoot(Collection mds, List rootmds) {
        // Make a map of ModuleId objects -> ModuleDescriptors
        Map moduleIdMap = new HashMap();
        for (Iterator iter = mds.iterator(); iter.hasNext();) {
            ModuleDescriptor md = ((ModuleDescriptor) iter.next());
            moduleIdMap.put(md.getModuleRevisionId().getModuleId(), md);
        }

        // recursively process the nodes
        Set toKeep = new LinkedHashSet();
        
        Iterator it = rootmds.iterator();
        while (it.hasNext()) {
        	ModuleDescriptor rootmd = (ModuleDescriptor) it.next();
            processFilterNodeFromRoot(rootmd, toKeep, moduleIdMap);
            // With the excluderoot attribute set to true, take the rootmd out of the toKeep set.
            if (_excludeRoot) {
                Message.verbose("Excluded module " + rootmd.getModuleRevisionId().getModuleId().getName());
                toKeep.remove(rootmd);
            }
        }
        

        // just for logging
        for (Iterator iter = toKeep.iterator(); iter.hasNext();) {
            ModuleDescriptor md = ((ModuleDescriptor) iter.next());
            Message.verbose("Kept module " + md.getModuleRevisionId().getModuleId().getName());
        }

        return toKeep;
    }

    /**
     * Adds the current node to the toKeep collection and then processes the each of the direct dependencies
     * of this node that appear in the moduleIdMap (indicating that the dependency is part of this BuildList)
     *
     * @param node the node to be processed
     * @param toKeep the set of ModuleDescriptors that should be kept
     * @param moduleIdMap reference mapping of moduleId to ModuleDescriptor that are part of the BuildList
     */
    private void processFilterNodeFromRoot(ModuleDescriptor node, Set toKeep, Map moduleIdMap) {
        toKeep.add(node);

        DependencyDescriptor[] deps = node.getDependencies();
        for (int i=0; i<deps.length; i++) {
            ModuleId id = deps[i].getDependencyId();
            if (moduleIdMap.get(id) != null) {
                processFilterNodeFromRoot((ModuleDescriptor) moduleIdMap.get(id), toKeep, moduleIdMap);
            }
        }
    }

    /**
     * Returns a collection of ModuleDescriptors that are conatined in the input
     * collection of ModuleDescriptors which depends on the leaf module
     *
     * @param mds input collection of ModuleDescriptors
     * @param leafmd leaf module
     * @return filtered list of modules
     */
    private Collection filterModulesFromLeaf(Collection mds, List leafmds) {
        // Make a map of ModuleId objects -> ModuleDescriptors
        Map moduleIdMap = new HashMap();
        for (Iterator iter = mds.iterator(); iter.hasNext();) {
            ModuleDescriptor md = ((ModuleDescriptor) iter.next());
            moduleIdMap.put(md.getModuleRevisionId().getModuleId(), md);
        }

        // recursively process the nodes
        Set toKeep = new LinkedHashSet();
        Iterator it = leafmds.iterator();
        while (it.hasNext()) {
        	ModuleDescriptor leafmd = (ModuleDescriptor) it.next();
            // With the excludeleaf attribute set to true, take the rootmd out of the toKeep set.
            if (_excludeLeaf) {
            	Message.verbose("Excluded module " + leafmd.getModuleRevisionId().getModuleId().getName());
            } else {
            	toKeep.add(leafmd);
            }
            processFilterNodeFromLeaf(leafmd, toKeep, moduleIdMap);
        }


        // just for logging
        for (Iterator iter = toKeep.iterator(); iter.hasNext();) {
            ModuleDescriptor md = ((ModuleDescriptor) iter.next());
            Message.verbose("Kept module " + md.getModuleRevisionId().getModuleId().getName());
        }

        return toKeep;
    }

    /**
     * Search in the moduleIdMap modules depending on node, add them to the toKeep set and process them 
     * recursively.
     *
     * @param node the node to be processed
     * @param toKeep the set of ModuleDescriptors that should be kept
     * @param moduleIdMap reference mapping of moduleId to ModuleDescriptor that are part of the BuildList
     */
    private void processFilterNodeFromLeaf(ModuleDescriptor node, Set toKeep, Map moduleIdMap) {
    	for (Iterator iter = moduleIdMap.values().iterator(); iter.hasNext();) {
			ModuleDescriptor md = (ModuleDescriptor) iter.next();
			DependencyDescriptor[] deps = md.getDependencies();
	        for (int i=0; i<deps.length; i++) {
	            ModuleId id = deps[i].getDependencyId();
	            if (node.getModuleRevisionId().getModuleId().equals(id) && !toKeep.contains(md)) {
	            	toKeep.add(md);
	            	processFilterNodeFromLeaf(md, toKeep, moduleIdMap);
	            }
	        }
		}
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

    public boolean isSkipbuildwithoutivy() {
        return _skipBuildWithoutIvy;
    }

    public void setSkipbuildwithoutivy(boolean skipBuildFilesWithoutIvy) {
        _skipBuildWithoutIvy = skipBuildFilesWithoutIvy;
    }

    public boolean isReverse() {
        return _reverse;
    }


    public void setReverse(boolean reverse) {
        _reverse = reverse;
    }

}
