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
package org.apache.ivy.plugins.parser.m2;

import org.apache.ivy.core.module.id.ModuleId;

public class ClassifiedModuleId extends ModuleId {

    static final String ENCODE_SEPARATOR = ":#@#:";

    public static final String CLASSIFIER_KEY = "classifier";

    public static final String DEFAULT_CLASSIFIER = "defaultclassifier";

    private String classifier;

    private int hash;

    public static ClassifiedModuleId newInstance(String org, String name, String classifier) {
        return (ClassifiedModuleId) intern(new ClassifiedModuleId(org, name, classifier));
    }

    public ClassifiedModuleId(String organisation, String name, String classifier) {
        super(organisation, name);

        if (classifier == null) {
            this.classifier = DEFAULT_CLASSIFIER;
        } else {
            this.classifier = classifier;
        }

        getAttributes().put(CLASSIFIER_KEY, classifier);
    }

    public String getClassifier() {
        return classifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClassifiedModuleId)) {
            return false;
        }
        ClassifiedModuleId other = (ClassifiedModuleId) obj;
        String organisation = getOrganisation();
        String name = getName();
        return (organisation == null) ? organisation == null && other.getName().equals(name)
                : other.getOrganisation().equals(organisation) && other.getName().equals(name)
                        && other.getClassifier().equals(classifier);
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            // CheckStyle:MagicNumber| OFF
            hash = super.hashCode();
            hash = hash * 13 + classifier.hashCode();
            // CheckStyle:MagicNumber| ON
        }
        return hash;
    }

    @Override
    public String toString() {
        return super.encodeToString() + "#" + classifier;
    }

    public int compareTo(ClassifiedModuleId that) {
        int result = super.compareTo(that);
        if (result == 0) {
            result = classifier.compareTo(that.classifier);
        }
        return result;
    }

    /**
     * Returns the encoded String representing this ModuleId.
     *
     * @return The ModuleId encoded as String.
     */
    public String encodeToString() {
        return getOrganisation() + ENCODE_SEPARATOR + getName() + ENCODE_SEPARATOR
                + getClassifier();
    }

    /**
     * Returns a ModuleId
     *
     * @param encoded
     *            String
     * @return The new ClassifiedModuleId.
     * @throws IllegalArgumentException
     *             If the given String could not be decoded.
     */
    public static ClassifiedModuleId decode(String encoded) {
        String[] parts = encoded.split(ENCODE_SEPARATOR);
        if (parts.length != 3) {
            throw new IllegalArgumentException("badly encoded module id: '" + encoded + "'");
        }
        return new ClassifiedModuleId(parts[0], parts[1], parts[2]);
    }

}
