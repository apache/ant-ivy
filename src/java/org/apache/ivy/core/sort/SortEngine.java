package org.apache.ivy.core.sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.circular.CircularDependencyException;

public class SortEngine {
	private IvySettings _settings;
	
    public SortEngine(IvySettings settings) {
		_settings = settings;
	}


	public List sortNodes(Collection nodes) {
        /* here we want to use the sort algorithm which work on module descriptors :
         * so we first put dependencies on a map from descriptors to dependency, then we 
         * sort the keySet (i.e. a collection of descriptors), then we replace
         * in the sorted list each descriptor by the corresponding dependency
         */
        
        Map dependenciesMap = new LinkedHashMap();
        List nulls = new ArrayList();
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            IvyNode node = (IvyNode)iter.next();
            if (node.getDescriptor() == null) {
                nulls.add(node);
            } else {
                List n = (List)dependenciesMap.get(node.getDescriptor());
                if (n == null) {
                    n = new ArrayList();
                    dependenciesMap.put(node.getDescriptor(), n);
                }
                n.add(node);
            }
        }
        List list = sortModuleDescriptors(dependenciesMap.keySet());
        List ret = new ArrayList((int)(list.size()*1.3+nulls.size())); //attempt to adjust the size to avoid too much list resizing
        for (int i=0; i<list.size(); i++) {
            ModuleDescriptor md = (ModuleDescriptor)list.get(i);
            List n = (List)dependenciesMap.get(md);
            ret.addAll(n);            
        }
        ret.addAll(0, nulls);
        return ret;
    }


    /**
     * Sorts the given ModuleDescriptors from the less dependent to the more dependent.
     * This sort ensures that a ModuleDescriptor is always found in the list before all 
     * ModuleDescriptors depending directly on it.
     * @param moduleDescriptors a Collection of ModuleDescriptor to sort
     * @return a List of sorted ModuleDescriptors
     * @throws CircularDependencyException if a circular dependency exists
     */
    public List sortModuleDescriptors(Collection moduleDescriptors) throws CircularDependencyException {
        return new ModuleDescriptorSorter(moduleDescriptors).sortModuleDescriptors(_settings.getVersionMatcher());   
    }

}
