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
package org.apache.ivy.plugins.repository.url;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.ivy.plugins.repository.AbstractRepository;
import org.apache.ivy.plugins.repository.BasicResource;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.util.Message;

public class ChainedRepository extends AbstractRepository {

    private List/* Repository */repositories;

    public void setRepositories(List/* Repository */repositories) {
        this.repositories = repositories;
    }

    public Resource getResource(String source) throws IOException {
        Iterator it = repositories.iterator();
        while (it.hasNext()) {
            Repository repository = (Repository) it.next();
            logTry(repository);
            try {
                Resource r = repository.getResource(source);
                if (r != null && r.exists()) {
                    logSuccess(repository);
                    return r;
                }
            } catch (Exception e) {
                logFailed(repository, e);
            }
        }
        // resource that basically doesn't exists
        return new BasicResource(source, false, 0, 0, true);
    }

    public void get(String source, File destination) throws IOException {
        Iterator it = repositories.iterator();
        while (it.hasNext()) {
            Repository repository = (Repository) it.next();
            logTry(repository);
            boolean ok = false;
            try {
                repository.get(source, destination);
                ok = true;
            } catch (Exception e) {
                logFailed(repository, e);
            }
            if (ok) {
                logSuccess(repository);
                return;
            }
        }
        throw newIOEFail("copy " + source + " into " + destination);
    }

    public List list(String parent) throws IOException {
        Iterator it = repositories.iterator();
        while (it.hasNext()) {
            Repository repository = (Repository) it.next();
            logTry(repository);
            try {
                List list = repository.list(parent);
                if (list != null) {
                    logSuccess(repository);
                    return list;
                }
            } catch (Exception e) {
                logFailed(repository, e);
            }
        }
        throw newIOEFail("list contents in " + parent);
    }

    private void logTry(Repository repository) {
        Message.debug("Mirrored repository " + getName() + ": trying " + repository.getName());
    }

    private void logFailed(Repository repository, Exception e) {
        Message.warn("Mirrored repository " + getName() + ": " + repository.getName()
                + " is not available", e);
        Message.warn("Trying the next one in the mirror list...");
    }

    private void logSuccess(Repository repository) {
        Message.debug("Mirrored repository " + getName() + ": success with " + repository.getName());
    }

    private IOException newIOEFail(String action) {
        return new IOException("Mirrored repository " + getName() + ": fail to " + action
                + " with every listed mirror");
    }

}
