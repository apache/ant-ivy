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
package org.apache.ivy.util.extendable;

import java.util.Map;

public interface ExtendableItem {
    /**
     * Gets the value of an attribute Can be used to access the value of a standard attribute (like
     * organisation, revision) or of an extra attribute.
     * 
     * @param attName
     *            the name of the attribute to get
     * @return the value of the attribute, null if the attribute doesn't exist
     */
    String getAttribute(String attName);

    /**
     * Gets the value of an extra attribute Can be used only to access the value of an extra
     * attribute, not a standard one (like organisation, revision)
     * 
     * @param attName
     *            the name of the extra attribute to get. This name can be either qualified or
     *            unqualified.
     * @return the value of the attribute, null if the attribute doesn't exist
     */
    String getExtraAttribute(String attName);

    /**
     * Returns a Map of all attributes of this extendable item, including standard and extra ones.
     * The Map keys are attribute names as Strings, and values are corresponding attribute values
     * (as String too). Extra attributes are included in unqualified form only.
     * 
     * @return A Map instance containing all the attributes and their values.
     */
    Map<String, String> getAttributes();

    /**
     * Returns a Map of all extra attributes of this extendable item. The Map keys are
     * <b>unqualified</b> attribute names as Strings, and values are corresponding attribute values
     * (as String too)
     * 
     * @return A Map instance containing all the extra attributes and their values.
     * @see #getQualifiedExtraAttributes()
     */
    Map<String, String> getExtraAttributes();

    /**
     * Returns a Map of all extra attributes of this extendable item.
     * <p>
     * The Map keys are <b>qualified</b> attribute names as Strings, and values are corresponding
     * attribute values (as String too).
     * </p>
     * <p>
     * An attribute name is qualified with a namespace exactly the same way xml attributes are
     * qualified. Thus qualified attribute names are of the form <code>prefix:name</code>
     * </p>
     * 
     * @return A Map instance containing all the extra attributes and their values.
     * @see #getExtraAttributes()
     */
    Map<String, String> getQualifiedExtraAttributes();
}
