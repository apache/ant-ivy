/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.asciidoc;

import java.util.Map;
import java.util.HashMap;

import org.asciidoctor.ast.AbstractBlock;
import org.asciidoctor.extension.InlineMacroProcessor;

public class JiraMacro extends InlineMacroProcessor {

    public JiraMacro(String macroName, Map<String, Object> config) {
        super(macroName, config);
    }

    @Override
    public String process(AbstractBlock parent, String target, Map<String, Object> attributes) {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("type", ":link");
        options.put("target", "https://issues.apache.org/jira/browse/" + target);
        return createInline(parent, "anchor", target, attributes, options).convert();
    }

}
