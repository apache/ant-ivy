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
package org.apache.ivy.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.ivy.core.IvyPatternHelper;

/**
 * Ant 1.6.1 like Configurator
 * <p>
 * This configurator is used to configure elements (initialised with setRoot) using the behaviour
 * defined by ant for its tasks.
 * <p>
 * Example (based on <a href="http://ant.apache.org/manual/develop.html#writingowntask">Ant
 * Example</a>):
 *
 * <pre>
 * Configurator conf = new Configurator();
 * conf.typeDef(&quot;buildpath&quot;, &quot;Sample$BuildPath&quot;);
 * conf.typeDef(&quot;xinterface&quot;, &quot;Sample$XInterface&quot;);
 * Sample.MyFileSelector mfs = new Sample.MyFileSelector();
 * conf.setRoot(mfs);
 * conf.startCreateChild(&quot;buildpath&quot;);
 * conf.setAttribute(&quot;path&quot;, &quot;.&quot;);
 * conf.setAttribute(&quot;url&quot;, &quot;abc&quot;);
 * conf.startCreateChild(&quot;xinterface&quot;);
 * conf.setAttribute(&quot;count&quot;, &quot;4&quot;);
 * conf.endCreateChild(); // xinterface
 * conf.endCreateChild(); // buildpath
 * </pre>
 */
public class Configurator {
    public static class Macro {
        private MacroDef macrodef;

        private Map<String, String> attValues = new HashMap<>();

        private Map<String, List<MacroRecord>> macroRecords = new HashMap<>();

        public Macro(MacroDef def) {
            macrodef = def;
        }

        public void defineAttribute(String attributeName, String value) {
            if (macrodef.getAttribute(attributeName) == null) {
                throw new IllegalArgumentException("undeclared attribute " + attributeName
                        + " on macro " + macrodef.getName());
            }
            attValues.put(attributeName, value);
        }

        public MacroRecord recordCreateChild(String name) {
            MacroRecord macroRecord = new MacroRecord(name);
            List<MacroRecord> records = macroRecords.get(name);
            if (records == null) {
                records = new ArrayList<>();
                macroRecords.put(name, records);
            }
            records.add(macroRecord);
            return macroRecord;
        }

        public Object play(Configurator conf) {
            return macrodef.play(conf, attValues, macroRecords);
        }

    }

    public static class Attribute {
        private String name;

        private String defaultValue;

        public String getDefault() {
            return defaultValue;
        }

        public void setDefault(String default1) {
            defaultValue = default1;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Element {
        private String name;

        private boolean optional = false;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isOptional() {
            return optional;
        }

        public void setOptional(boolean optional) {
            this.optional = optional;
        }
    }

    public static class MacroRecord {
        private String name;

        private Map<String, String> attributes = new LinkedHashMap<>();

        private List<MacroRecord> children = new ArrayList<>();

        private Object object;

        public MacroRecord(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void recordAttribute(String name, String value) {
            attributes.put(name, value);
        }

        public MacroRecord recordChild(String name) {
            MacroRecord child = new MacroRecord(name);
            children.add(child);
            return child;
        }

        public MacroRecord recordChild(String name, Object object) {
            MacroRecord child = recordChild(name);
            child.object = object;
            return child;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public List<MacroRecord> getChildren() {
            return children;
        }

        public Object getObject() {
            return object;
        }
    }

    public static class MacroDef {
        private String name;

        private Map<String, Attribute> attributes = new HashMap<>();

        private Map<String, Element> elements = new HashMap<>();

        private MacroRecord macroRecord;

        public MacroDef(String macroName) {
            name = macroName;
        }

        public Attribute getAttribute(String attributeName) {
            return attributes.get(attributeName);
        }

        public Object play(Configurator conf, Map<String, String> attValues,
                           Map<String, List<MacroRecord>> macroRecords) {
            for (Attribute att : attributes.values()) {
               String val = attValues.get(att.getName());
                if (val == null) {
                    if (att.getDefault() == null) {
                        throw new IllegalArgumentException("attribute " + att.getName()
                                + " is required in " + getName());
                    } else {
                        attValues.put(att.getName(), att.getDefault());
                    }
                }
            }
            return play(conf, macroRecord, attValues, macroRecords);
        }

        private Object play(Configurator conf, MacroRecord macroRecord, Map<String, String> attValues,
                Map<String, List<MacroRecord>> childrenRecords) {
            if (macroRecord.getObject() != null) {
                // this is a recorded reference, we can add the referenced object directly
                conf.addChild(macroRecord.getName(), macroRecord.getObject());
                conf.endCreateChild();
                return macroRecord.getObject();
            }
            conf.startCreateChild(macroRecord.getName());
            Map<String, String> attributes = macroRecord.getAttributes();
            for (String attName : attributes.keySet()) {
                String attValue = replaceParam(attributes.get(attName), attValues);
                conf.setAttribute(attName, attValue);
            }
            for (MacroRecord child : macroRecord.getChildren()) {
                Element elt = elements.get(child.getName());
                if (elt != null) {
                    List<MacroRecord> elements = childrenRecords.get(child.getName());
                    if (elements != null) {
                        for (MacroRecord element : elements) {
                            for (MacroRecord r : element.getChildren()) {
                                play(conf, r, attValues, Collections.<String, List<MacroRecord>>emptyMap());
                            }
                        }
                    } else if (!elt.isOptional()) {
                        throw new IllegalArgumentException(
                                "non optional element is not specified: " + elt.getName()
                                        + " in macro " + getName());
                    }
                    continue;
                }
                play(conf, child, attValues, childrenRecords);
            }
            return conf.endCreateChild();
        }

        private String replaceParam(String string, Map<String, String> attValues) {
            return IvyPatternHelper.substituteParams(string, attValues);
        }

        public String getName() {
            return name;
        }

        public void addConfiguredAttribute(Attribute att) {
            attributes.put(att.getName(), att);
        }

        public void addConfiguredElement(Element elt) {
            elements.put(elt.getName(), elt);
        }

        public Macro createMacro() {
            return new Macro(this);
        }

        public void addAttribute(String attName, String attDefaultValue) {
            Attribute att = new Attribute();
            att.setName(attName);
            att.setDefault(attDefaultValue);
            addConfiguredAttribute(att);
        }

        public void addElement(String elementName, boolean optional) {
            Element elt = new Element();
            elt.setName(elementName);
            elt.setOptional(optional);
            addConfiguredElement(elt);
        }

        public MacroRecord recordCreateChild(String name) {
            macroRecord = new MacroRecord(name);
            return macroRecord;
        }
    }

    private static class ObjectDescriptor {
        private Object obj;

        private String objName;

        private Map<String, Method> createMethods = new HashMap<>();

        private Map<String, Method> addMethods = new HashMap<>();

        private Map<String, Method> addConfiguredMethods = new HashMap<>();

        private Map<String, Method> setMethods = new HashMap<>();

        private Map<Class<?>, Method> typeAddMethods = new HashMap<>();

        private Map<Class<?>, Method> typeAddConfiguredMethods = new HashMap<>();

        public ObjectDescriptor(Object object, String objName) {
            obj = object;
            this.objName = objName;
            Method[] methods = object.getClass().getMethods();
            for (Method m : methods) {
                if (m.getName().startsWith("create") && m.getParameterTypes().length == 0
                        && !Void.TYPE.equals(m.getReturnType())) {
                    String name = StringUtils
                            .uncapitalize(m.getName().substring("create".length()));
                    if (name.length() == 0) {
                        continue;
                    }
                    addCreateMethod(name, m);
                } else if (m.getName().startsWith("addConfigured")
                        && m.getParameterTypes().length == 1 && Void.TYPE.equals(m.getReturnType())) {
                    String name = StringUtils.uncapitalize(m.getName().substring(
                            "addConfigured".length()));
                    if (name.length() == 0) {
                        addAddConfiguredMethod(m);
                    }
                    addAddConfiguredMethod(name, m);
                } else if (m.getName().startsWith("add")
                        && !m.getName().startsWith("addConfigured")
                        && m.getParameterTypes().length == 1 && Void.TYPE.equals(m.getReturnType())) {
                    String name = StringUtils.uncapitalize(m.getName().substring("add".length()));
                    if (name.length() == 0) {
                        addAddMethod(m);
                    }
                    addAddMethod(name, m);
                } else if (m.getName().startsWith("set") && m.getParameterTypes().length == 1
                        && Void.TYPE.equals(m.getReturnType())) {
                    String name = StringUtils.uncapitalize(m.getName().substring("set".length()));
                    if (name.length() == 0) {
                        continue;
                    }
                    addSetMethod(name, m);
                }
            }
        }

        public void addCreateMethod(String name, Method m) {
            createMethods.put(name, m);
        }

        public void addAddMethod(String name, Method m) {
            addMethods.put(name, m);
        }

        public void addAddConfiguredMethod(String name, Method m) {
            addConfiguredMethods.put(name, m);
        }

        private void addAddMethod(Method m) {
            typeAddMethods.put(m.getParameterTypes()[0], m);
        }

        private void addAddConfiguredMethod(Method m) {
            typeAddConfiguredMethods.put(m.getParameterTypes()[0], m);
        }

        public void addSetMethod(String name, Method m) {
            Method current = setMethods.get(name);
            if (current != null && current.getParameterTypes()[0] == String.class) {
                // setter methods with String attribute take precedence
                return;
            }
            setMethods.put(name, m);
        }

        public Object getObject() {
            return obj;
        }

        public Method getCreateMethod(String name) {
            return createMethods.get(name);
        }

        public Method getAddMethod(String name) {
            return addMethods.get(name);
        }

        public Method getAddConfiguredMethod(String name) {
            return addConfiguredMethods.get(name);
        }

        public Method getAddMethod(Class type) {
            return getTypeMatchingMethod(type, typeAddMethods);
        }

        public Method getAddConfiguredMethod(Class type) {
            return getTypeMatchingMethod(type, typeAddConfiguredMethods);
        }

        private Method getTypeMatchingMethod(Class<?> type, Map<Class<?>, Method> typeMethods) {
            Method m = typeMethods.get(type);
            if (m != null) {
                return m;
            }
            for (Class<?> clazz : typeMethods.keySet()) {
                if (clazz.isAssignableFrom(type)) {
                    return typeMethods.get(clazz);
                }
            }
            return null;
        }

        public Method getSetMethod(String name) {
            return setMethods.get(name);
        }

        public String getObjectName() {
            return objName;
        }
    }

    private FileResolver fileResolver = FileResolver.DEFAULT;

    private Map<String, Class<?>> typedefs = new HashMap<>();

    private Map<String, MacroDef> macrodefs = new HashMap<>();

    // stack in which the top is current configured object descriptor
    private Stack<ObjectDescriptor> objectStack = new Stack<>();

    private static final List TRUE_VALUES = Arrays.asList("true", "yes", "on");

    public void typeDef(String name, String className) throws ClassNotFoundException {
        typeDef(name, Class.forName(className));
    }

    public void typeDef(String name, Class clazz) {
        typedefs.put(name, clazz);
    }

    public void setRoot(Object root) {
        if (root == null) {
            throw new NullPointerException();
        }
        objectStack.clear();
        setCurrent(root, null);
    }

    public void clear() {
        objectStack.clear();
    }

    private void setCurrent(Object object, String name) {
        objectStack.push(new ObjectDescriptor(object, name));
    }

    public Object startCreateChild(String name) {
        if (objectStack.isEmpty()) {
            throw new IllegalStateException("set root before creating child");
        }
        ObjectDescriptor parentOD = objectStack.peek();
        Object parent = parentOD.getObject();
        if (parent instanceof MacroDef) {
            if (!"attribute".equals(name) && !"element".equals(name)) {
                MacroRecord record = ((MacroDef) parent).recordCreateChild(name);
                setCurrent(record, name);
                return record;
            }
        }
        if (parent instanceof Macro) {
            MacroRecord record = ((Macro) parent).recordCreateChild(name);
            setCurrent(record, name);
            return record;
        }
        if (parent instanceof MacroRecord) {
            MacroRecord record = ((MacroRecord) parent).recordChild(name);
            setCurrent(record, name);
            return record;
        }
        Object child = null;
        MacroDef macrodef = macrodefs.get(name);
        if (macrodef != null) {
            Macro macro = macrodef.createMacro();
            setCurrent(macro, name);
            return macro;
        }
        Class childClass = typedefs.get(name);
        Method addChild = null;
        try {
            if (childClass != null) {
                return addChild(parentOD, childClass, name, null);
            } else {
                addChild = parentOD.getCreateMethod(name);
                if (addChild != null) {
                    child = addChild.invoke(parent);
                    setCurrent(child, name);
                    return child;
                }
                addChild = parentOD.getAddMethod(name);
                if (addChild != null) {
                    childClass = addChild.getParameterTypes()[0];
                    child = childClass.newInstance();
                    addChild.invoke(parent, child);
                    setCurrent(child, name);
                    return child;
                }
                addChild = parentOD.getAddConfiguredMethod(name);
                if (addChild != null) {
                    childClass = addChild.getParameterTypes()[0];
                    if (Map.class == childClass) {
                        child = new HashMap();
                    } else {
                        child = childClass.newInstance();
                    }
                    setCurrent(child, name);
                    return child;
                }
            }
        } catch (InstantiationException ex) {
            throw new IllegalArgumentException("no default constructor on " + childClass
                    + " for adding " + name + " on " + parent.getClass());
        } catch (Exception ex) {
            throw new IllegalArgumentException("bad method found for " + name + " on "
                    + parent.getClass(), ex);
        }
        throw new IllegalArgumentException("no appropriate method found for adding " + name
                + " on " + parent.getClass());
    }

    public void addChild(String name, Object child) {
        if (objectStack.isEmpty()) {
            throw new IllegalStateException("set root before creating child");
        }
        ObjectDescriptor parentOD = objectStack.peek();
        try {
            addChild(parentOD, child.getClass(), name, child);
        } catch (InstantiationException ex) {
            throw new IllegalArgumentException("no default constructor on " + child.getClass()
                    + " for adding " + name + " on " + parentOD.getObject().getClass());
        } catch (Exception ex) {
            throw new IllegalArgumentException("bad method found for "
                    + name + " on " + parentOD.getObject().getClass(), ex);
        }
    }

    private Object addChild(ObjectDescriptor parentOD, Class childClass, String name, Object child)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Object parent = parentOD.getObject();
        if (parent instanceof MacroRecord) {
            MacroRecord record = (MacroRecord) parent;
            MacroRecord recordChild = record.recordChild(name, child);
            setCurrent(recordChild, name);
            return recordChild;
        }
        Method addChild = parentOD.getAddMethod(childClass);
        if (addChild != null) {
            if (child == null) {
                child = childClass.newInstance();
            }
            addChild.invoke(parent, child);
            setCurrent(child, name);
            return child;
        }
        addChild = parentOD.getAddConfiguredMethod(childClass);
        if (addChild != null) {
            if (child == null) {
                if (Map.class == childClass) {
                    child = new HashMap();
                } else {
                    child = childClass.newInstance();
                }
            }
            setCurrent(child, name);
            return child;
        }
        throw new IllegalArgumentException("no appropriate method found for adding " + name
                + " on " + parent.getClass());
    }

    public boolean isTopLevelMacroRecord() {
        if (objectStack.isEmpty()) {
            return false;
        }
        ObjectDescriptor od = objectStack.peek();
        return (od.getObject() instanceof MacroDef);
    }

    public void setAttribute(String attributeName, String value) {
        if (objectStack.isEmpty()) {
            throw new IllegalStateException("set root before setting attribute");
        }
        ObjectDescriptor od = objectStack.peek();
        if (od.getObject() instanceof Macro) {
            ((Macro) od.getObject()).defineAttribute(attributeName, value);
            return;
        }
        if (od.getObject() instanceof MacroRecord) {
            ((MacroRecord) od.getObject()).recordAttribute(attributeName, value);
            return;
        }
        Method m = od.getSetMethod(attributeName);
        if (m == null) {
            if (od.getObject() instanceof Map) {
                ((Map<String, String>) od.getObject()).put(attributeName, value);
                return;
            }
            throw new IllegalArgumentException("no set method found for " + attributeName + " on "
                    + od.getObject().getClass());
        }
        Object convertedValue = null;
        Class<?> paramClass = m.getParameterTypes()[0];
        try {
            if (paramClass.equals(String.class)) {
                convertedValue = value;
            } else if (paramClass.equals(Boolean.class) || paramClass.equals(boolean.class)) {
                convertedValue = TRUE_VALUES.contains(value);
            } else if (paramClass.equals(Character.class) || paramClass.equals(char.class)) {
                convertedValue = value.length() > 0 ? value.charAt(0) : ' ';
            } else if (paramClass.equals(Short.class) || paramClass.equals(short.class)) {
                convertedValue = Short.valueOf(value);
            } else if (paramClass.equals(Integer.class) || paramClass.equals(int.class)) {
                convertedValue = Integer.valueOf(value);
            } else if (paramClass.equals(Long.class) || paramClass.equals(long.class)) {
                convertedValue = Long.valueOf(value);
            } else if (paramClass.equals(Class.class)) {
                convertedValue = Class.forName(value);
            } else if (paramClass.equals(File.class)) {
                convertedValue = fileResolver.resolveFile(value, od.getObjectName() + "."
                        + attributeName);
            } else {
                convertedValue = paramClass.getConstructor(new Class[] {String.class}).newInstance(
                        value);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("impossible to convert "
                    + value + " to " + paramClass + " for setting " + attributeName + " on "
                    + od.getObject().getClass() + ": " + ex.getMessage(), ex);
        }
        try {
            m.invoke(od.getObject(), convertedValue);
        } catch (Exception ex) {
            throw new IllegalArgumentException("impossible to set " + attributeName + " to "
                    + convertedValue + " on " + od.getObject().getClass(), ex);
        }
    }

    public void addText(String text) {
        if (objectStack.isEmpty()) {
            throw new IllegalStateException("set root before adding text");
        }
        ObjectDescriptor od = objectStack.peek();
        try {
            od.getObject().getClass().getMethod("addText", new Class[] {String.class})
                    .invoke(od.getObject(), text);
        } catch (Exception ex) {
            throw new IllegalArgumentException("impossible to add text on "
                    + od.getObject().getClass(), ex);
        }
    }

    /**
     * @return the finished child
     */
    public Object endCreateChild() {
        if (objectStack.isEmpty()) {
            throw new IllegalStateException("set root before ending child");
        }
        ObjectDescriptor od = objectStack.pop();
        if (objectStack.isEmpty()) {
            objectStack.push(od); // back to previous state
            throw new IllegalStateException("cannot end root");
        }
        if (od.getObject() instanceof Macro) {
            return ((Macro) od.getObject()).play(this);
        }
        ObjectDescriptor parentOD = objectStack.peek();
        String name = od.getObjectName();
        Class<?> childClass = typedefs.get(name);
        Method m = (childClass == null) ? parentOD.getAddConfiguredMethod(name)
                : parentOD.getAddConfiguredMethod(childClass);
        try {
            if (m != null) {
                m.invoke(parentOD.getObject(), od.getObject());
            }
            return od.getObject();
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "impossible to add configured child for " + name + " on "
                            + parentOD.getObject().getClass() + ": "
                            + StringUtils.getErrorMessage(ex), ex);
        }
    }

    public Object getCurrent() {
        return objectStack.isEmpty() ? null : objectStack.peek().getObject();
    }

    public int getDepth() {
        return objectStack.size();
    }

    public MacroDef startMacroDef(String macroName) {
        MacroDef macroDef = new MacroDef(macroName);
        setCurrent(macroDef, macroName);
        return macroDef;
    }

    public void addMacroAttribute(String attName, String attDefaultValue) {
        ((MacroDef) getCurrent()).addAttribute(attName, attDefaultValue);
    }

    public void addMacroElement(String elementName, boolean optional) {
        ((MacroDef) getCurrent()).addElement(elementName, optional);
    }

    public void endMacroDef() {
        addConfiguredMacrodef(((MacroDef) getCurrent()));
        objectStack.pop();
    }

    public void addConfiguredMacrodef(MacroDef macrodef) {
        macrodefs.put(macrodef.getName(), macrodef);
    }

    public Class getTypeDef(String name) {
        return typedefs.get(name);
    }

    public FileResolver getFileResolver() {
        return fileResolver;
    }

    public void setFileResolver(FileResolver fileResolver) {
        Checks.checkNotNull(fileResolver, "fileResolver");
        this.fileResolver = fileResolver;
    }
}
