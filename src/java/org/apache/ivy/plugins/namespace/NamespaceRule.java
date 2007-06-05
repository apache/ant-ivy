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
package org.apache.ivy.plugins.namespace;

public class NamespaceRule {
    private String _name;

    private String _description;

    private MRIDTransformationRule _fromSystem;

    private MRIDTransformationRule _toSystem;

    public MRIDTransformationRule getFromSystem() {
        return _fromSystem;
    }

    public void addFromsystem(MRIDTransformationRule fromSystem) {
        if (_fromSystem != null) {
            throw new IllegalArgumentException("only one fromsystem is allowed per rule");
        }
        _fromSystem = fromSystem;
    }

    public MRIDTransformationRule getToSystem() {
        return _toSystem;
    }

    public void addTosystem(MRIDTransformationRule toSystem) {
        if (_toSystem != null) {
            throw new IllegalArgumentException("only one tosystem is allowed per rule");
        }
        _toSystem = toSystem;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }
}
