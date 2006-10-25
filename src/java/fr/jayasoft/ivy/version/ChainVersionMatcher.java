/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.version;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyAware;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;

public class ChainVersionMatcher extends AbstractVersionMatcher {
    private List _matchers = new LinkedList();
    
    public ChainVersionMatcher() {
    	super("chain");
    }
    
    public void add(VersionMatcher matcher) {
        _matchers.add(0, matcher);
		if (getIvy() != null && matcher instanceof IvyAware) {
			((IvyAware) matcher).setIvy(getIvy());
		}
    }
    
    public void setIvy(Ivy ivy) {
    	super.setIvy(ivy);
    	for (Iterator iter = _matchers.iterator(); iter.hasNext();) {
			VersionMatcher matcher = (VersionMatcher) iter.next();
			if (matcher instanceof IvyAware) {
				((IvyAware) matcher).setIvy(ivy);
			}
		}
    }
    
    public List getMatchers() {
    	return Collections.unmodifiableList(_matchers);
    }
    

    public boolean isDynamic(ModuleRevisionId askedMrid) {
        for (Iterator iter = _matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher)iter.next();
            if (matcher.isDynamic(askedMrid)) {
                return true;
            }
        }
        return false;
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        for (Iterator iter = _matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher)iter.next();
            if (!iter.hasNext() || matcher.isDynamic(askedMrid)) {
                return matcher.accept(askedMrid, foundMrid);
            }
        }
        return false;
    }

    public boolean needModuleDescriptor(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
        for (Iterator iter = _matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher)iter.next();
            if (!iter.hasNext() || matcher.isDynamic(askedMrid)) {
                return matcher.needModuleDescriptor(askedMrid, foundMrid);
            }
        }
        return false;
    }

    public boolean accept(ModuleRevisionId askedMrid, ModuleDescriptor foundMD) {
        for (Iterator iter = _matchers.iterator(); iter.hasNext();) {
            VersionMatcher matcher = (VersionMatcher)iter.next();
            if (!iter.hasNext() || matcher.isDynamic(askedMrid)) {
                return matcher.accept(askedMrid, foundMD);
            }
        }
        return false;
    }

}
