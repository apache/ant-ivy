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
package org.apache.ivy.plugins.resolver;

/**
 * BintrayResolver is a resolver which can be used to resolve dependencies found in the Bintray
 * artifacts repository.
 */
public class BintrayResolver extends IBiblioResolver {

    private static final String JCENTER = "https://jcenter.bintray.com/";

    private static final String DL_BINTRAY = "https://dl.bintray.com/";

    private static final String DEFAULT_NAME = "bintray/jcenter";

    private String subject;

    private String repo;

    private boolean isNameUpdatable; // Whether resolver's name was not originally specified and can
                                     // be updated.

    public BintrayResolver() {
        setRoot(JCENTER);
        updateName(DEFAULT_NAME);
        setM2compatible(true);
        setUsepoms(true);
        setUseMavenMetadata(true);
    }

    public void setSubject(String subject) {
        this.subject = subject;
        updateRoot();
    }

    public void setRepo(String repo) {
        this.repo = repo;
        updateRoot();
    }

    private void updateRoot() {
        if (isEmpty(subject) || isEmpty(repo)) {
            return;
        }

        setRoot(String.format("%s%s/%s/", DL_BINTRAY, subject, repo));
        updateName(String.format("bintray/%s/%s", subject, repo));
    }

    private void updateName(String defaultName) {
        if (isEmpty(defaultName)) {
            throw new IllegalArgumentException("Default resolver name must not be null or empty");
        }
        if (isEmpty(getName()) || isNameUpdatable) {
            isNameUpdatable = true;
            setName(defaultName);
        }
    }

    private boolean isEmpty(String s) {
        return ((s == null) || (s.trim().length() < 1));
    }
}
