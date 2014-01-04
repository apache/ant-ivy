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
package org.apache.ivy.osgi.repo;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class AggregatedRepoDescriptor extends RepoDescriptor {

    private List<RepoDescriptor> repos;

    public AggregatedRepoDescriptor(List<RepoDescriptor> repos) {
        this.repos = repos;
    }

    @Override
    public Iterator<ModuleDescriptorWrapper> getModules() {
        final Iterator<RepoDescriptor> itRepos = repos.iterator();
        return new Iterator<ModuleDescriptorWrapper>() {

            private Iterator<ModuleDescriptorWrapper> current = null;

            private ModuleDescriptorWrapper next = null;

            public boolean hasNext() {
                while (next == null) {
                    if (current == null) {
                        if (!itRepos.hasNext()) {
                            return false;
                        }
                        RepoDescriptor repo = itRepos.next();
                        current = repo.getModules();
                    }
                    if (current.hasNext()) {
                        next = current.next();
                    } else {
                        current = null;
                    }
                }
                return true;
            }

            public ModuleDescriptorWrapper next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                ModuleDescriptorWrapper ret = next;
                next = null;
                return ret;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Set<String> getCapabilities() {
        Set<String> ret = new HashSet<String>();
        for (RepoDescriptor repo : repos) {
            Set<String> capabilities = repo.getCapabilities();
            if (capabilities != null) {
                ret.addAll(capabilities);
            }
        }
        return ret;
    }

    @Override
    public Set<ModuleDescriptorWrapper> findModules(String requirement, String value) {
        Set<ModuleDescriptorWrapper> ret = new HashSet<ModuleDescriptorWrapper>();
        for (RepoDescriptor repo : repos) {
            Set<ModuleDescriptorWrapper> modules = repo.findModules(requirement, value);
            if (modules != null) {
                ret.addAll(modules);
            }
        }
        return ret;
    }

    @Override
    public Set<String> getCapabilityValues(String capabilityName) {
        Set<String> ret = new HashSet<String>();
        for (RepoDescriptor repo : repos) {
            Set<String> capabilityValues = repo.getCapabilityValues(capabilityName);
            if (capabilityValues != null) {
                ret.addAll(capabilityValues);
            }
        }
        return ret;
    }

}
