/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Ant 1.6.1 like Configurator
 * 
 * This configurator is used to configure elements (initialised with setRoot)
 * using the behaviour defined by ant for its tasks.
 * 
 * Example (based on <a href="http://ant.apache.org/manual/develop.html#writingowntask">Ant Example</a>) :
 * Configurator conf = new Configurator();
 * conf.typeDef("buildpath", "Sample$BuildPath");
 * conf.typeDef("xinterface", "Sample$XInterface");
 * 
 * Sample.MyFileSelector mfs = new Sample.MyFileSelector();
 * conf.setRoot(mfs);
 * conf.startCreateChild("buildpath");
 * conf.setAttribute("path", ".");
 * conf.setAttribute("url", "abc");
 * conf.startCreateChild("xinterface");
 * conf.setAttribute("count", "4");
 * conf.endCreateChild(); // xinterface
 * conf.endCreateChild(); // buildpath
 * 
 * @author x.hanin
 *
 */
public class Configurator {
    public static class Macro {
        private MacroDef _macrodef;
        private Map _attValues = new HashMap();
        private Map _macroRecords = new HashMap();

        public Macro(MacroDef def) {
            _macrodef = def;
        }

        public void defineAttribute(String attributeName, String value) {
            if (_macrodef.getAttribute(attributeName) == null) {
                throw new IllegalArgumentException("undeclared attribute "+attributeName+" on macro "+_macrodef.getName());
            }
            _attValues.put(attributeName, value);
        }

        public MacroRecord recordCreateChild(String name) {
             MacroRecord macroRecord = new MacroRecord(name);
             List records = (List)_macroRecords.get(name);
             if (records == null) {
                 records = new ArrayList();
                 _macroRecords.put(name, records);
             }
             records.add(macroRecord);
            return macroRecord;
        }

        public Object play(Configurator conf) {
            return _macrodef.play(conf, _attValues, _macroRecords);
        }

    }

    public static class Attribute {
        private String _name;
        private String _default;
        
        public String getDefault() {
            return _default;
        }
        public void setDefault(String default1) {
            _default = default1;
        }
        public String getName() {
            return _name;
        }
        public void setName(String name) {
            _name = name;
        }
    }
    
    public static class Element {
        private String _name;
        private boolean _optional = false;
        public String getName() {
            return _name;
        }
        public void setName(String name) {
            _name = name;
        }
        public boolean isOptional() {
            return _optional;
        }
        public void setOptional(boolean optional) {
            _optional = optional;
        }
    }

    public static class MacroRecord {
        private String _name;
        private Map _attributes = new LinkedHashMap();
        private List _children = new ArrayList();
        public MacroRecord(String name) {
            _name = name;
        }
        public String getName() {
            return _name;
        }
        public void recordAttribute(String name, String value) {
            _attributes.put(name, value);
        }
        public MacroRecord recordChild(String name) {
            MacroRecord child = new MacroRecord(name);
            _children.add(child);
            return child;
        }
        public Map getAttributes() {
            return _attributes;
        }
        public List getChildren() {
            return _children;
        }
    }

    public static class MacroDef {
        private String _name;
        private Map _attributes = new HashMap();
        private Map _elements = new HashMap();
        private MacroRecord _macroRecord;

        public MacroDef(String macroName) {
            _name = macroName;
        }

        public Attribute getAttribute(String attributeName) {
            return (Attribute)_attributes.get(attributeName);
        }

        public Object play(Configurator conf, Map attValues, Map macroRecords) {
            for (Iterator iter = _attributes.values().iterator(); iter.hasNext();) {
                Attribute att = (Attribute)iter.next();
                String val = (String)attValues.get(att.getName());
                if (val == null) {
                    if (att.getDefault() == null) {
                        throw new IllegalArgumentException("attribute "+att.getName()+" is required in "+getName());
                    } else {
                        attValues.put(att.getName(), att.getDefault());
                    }
                }
            }
            return play(conf, _macroRecord, attValues, macroRecords);
        }

        private Object play(Configurator conf, MacroRecord macroRecord, Map attValues, Map childrenRecords) {
            conf.startCreateChild(macroRecord.getName());
            Map attributes = macroRecord.getAttributes();
            for (Iterator iter = attributes.keySet().iterator(); iter.hasNext();) {
                String attName = (String)iter.next();
                String attValue = replaceParam((String)attributes.get(attName), attValues);
                conf.setAttribute(attName, attValue);
            }
            for (Iterator iter = macroRecord.getChildren().iterator(); iter.hasNext();) {
                MacroRecord child = (MacroRecord)iter.next();
                Element elt = (Element)_elements.get(child.getName());
                if (elt != null) {
                    List elements = (List)childrenRecords.get(child.getName());
                    if (elements != null) {
                        for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
                            MacroRecord element = (MacroRecord)iterator.next();
                            for (Iterator it2 = element.getChildren().iterator(); it2.hasNext();) {
                                MacroRecord r = (MacroRecord)it2.next();
                                play(conf, r, attValues, Collections.EMPTY_MAP);
                            }
                        }
                    } else if (!elt.isOptional()) {
                        throw new IllegalArgumentException("non optional element is not specified: "+elt.getName()+" in macro "+getName());
                    }                    
                    continue;
                }
                play(conf, child, attValues, childrenRecords);
            }
            return conf.endCreateChild();
        }

        private String replaceParam(String string, Map attValues) {
            return IvyPatternHelper.substituteParams(string, attValues);
        }

        public String getName() {
            return _name;
        }
        
        public void addConfiguredAttribute(Attribute att) {
            _attributes.put(att.getName(), att);
        }
        
        public void addConfiguredElement(Element elt) {
            _elements.put(elt.getName(), elt);
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
            _macroRecord = new MacroRecord(name);
            return _macroRecord;
        }
    }

    private static class ObjectDescriptor {
        private Object _obj;
        private String _objName;
        private Map _createMethods = new HashMap();
        private Map _addMethods = new HashMap();
        private Map _addConfiguredMethods = new HashMap();
        private Map _setMethods = new HashMap();
        private Map _typeAddMethods = new HashMap();
        private Map _typeAddConfiguredMethods = new HashMap();
        
        public ObjectDescriptor(Object object, String objName) {
            _obj = object;
            _objName = objName;
            Method[] methods = object.getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method m = methods[i];
                if (m.getName().startsWith("create")
                        && m.getParameterTypes().length == 0
                        && !Void.TYPE.equals(m.getReturnType())) {
                    String name = StringUtils.uncapitalize(m.getName().substring("create".length()));
                    if (name.length() == 0) {
                        continue;
                    }
                    addCreateMethod(name, m);
                } else if (m.getName().startsWith("addConfigured")
                        && m.getParameterTypes().length == 1
                        && Void.TYPE.equals(m.getReturnType())) {
                    String name = StringUtils.uncapitalize(m.getName().substring("addConfigured".length()));
                    if (name.length() == 0) {
                        addAddConfiguredMethod(m);
                    }
                    addAddConfiguredMethod(name, m);
                } else if (m.getName().startsWith("add")
                        && !m.getName().startsWith("addConfigured")
                        && m.getParameterTypes().length == 1
                        && Void.TYPE.equals(m.getReturnType())) {
                    String name = StringUtils.uncapitalize(m.getName().substring("add".length()));
                    if (name.length() == 0) {
                        addAddMethod(m);
                    }
                    addAddMethod(name, m);
                } else if (m.getName().startsWith("set")
                        && m.getParameterTypes().length == 1
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
            _createMethods.put(name, m);
        }
        public void addAddMethod(String name, Method m) {
            _addMethods.put(name, m);
        }
        public void addAddConfiguredMethod(String name, Method m) {
            _addConfiguredMethods.put(name, m);
        }
        private void addAddMethod(Method m) {
            _typeAddMethods.put(m.getParameterTypes()[0], m);
        }
        private void addAddConfiguredMethod(Method m) {
            _typeAddConfiguredMethods.put(m.getParameterTypes()[0], m);
        }
        public void addSetMethod(String name, Method m) {
            _setMethods.put(name, m);
        }
        public Object getObject() {
            return _obj;
        }
        public Method getCreateMethod(String name) {
            return (Method)_createMethods.get(name);
        }
        public Method getAddMethod(String name) {
            return (Method)_addMethods.get(name);
        }
        public Method getAddConfiguredMethod(String name) {
            return (Method)_addConfiguredMethods.get(name);
        }
        public Method getAddMethod(Class type) {
            return getTypeMatchingMethod(type, _typeAddMethods);
        }
        public Method getAddConfiguredMethod(Class type) {
            return getTypeMatchingMethod(type, _typeAddConfiguredMethods);
        }
        private Method getTypeMatchingMethod(Class type, Map typeMethods) {
            Method m = (Method)typeMethods.get(type);
            if (m != null) {
                return m;
            }
            for (Iterator iter = typeMethods.keySet().iterator(); iter.hasNext();) {
                Class clss = (Class)iter.next();
                if (clss.isAssignableFrom(type)) {
                    return (Method)typeMethods.get(clss);
                }
            }
            return null;
        }
        public Method getSetMethod(String name) {
            return (Method)_setMethods.get(name);
        }
        public String getObjectName() {
            return _objName;
        }
    }

    private Map _typedefs = new HashMap();
    private Map _macrodefs = new HashMap();
    
    // stack in which the top is current configured object descriptor
    private Stack _objectStack = new Stack();


    private static final List TRUE_VALUES = Arrays.asList(new String[] {"true", "yes", "on"});

    public void typeDef(String name, String className) throws ClassNotFoundException {
        typeDef(name, Class.forName(className));
    }
    
    public void typeDef(String name, Class clazz) {
        _typedefs.put(name, clazz);
    }
    
    public void setRoot(Object root) {
        if (root == null) {
            throw new NullPointerException();
        }
        _objectStack.clear();
        setCurrent(root, null);
    }
    
    public void clear() {
        _objectStack.clear();
    }
    
    private void setCurrent(Object object, String name) {
        _objectStack.push(new ObjectDescriptor(object, name));
    }

    public Object startCreateChild(String name) {
        if (_objectStack.isEmpty()) {
            throw new IllegalStateException("set root before creating child");
        }
        ObjectDescriptor parentOD = (ObjectDescriptor)_objectStack.peek();
        Object parent = parentOD.getObject();
        if (parent instanceof MacroDef) {
            if (!"attribute".equals(name) && !"element".equals(name)) {
                MacroRecord record = ((MacroDef)parent).recordCreateChild(name);
                setCurrent(record, name);
                return record;
            }
        }
        if (parent instanceof Macro) {
            MacroRecord record = ((Macro)parent).recordCreateChild(name);
            setCurrent(record, name);
            return record;
        }
        if (parent instanceof MacroRecord) {
            MacroRecord record = ((MacroRecord)parent).recordChild(name);
            setCurrent(record, name);
            return record;
        }
        Object child = null;
        MacroDef macrodef = (MacroDef)_macrodefs.get(name);
        if (macrodef != null) {
            Macro macro = macrodef.createMacro();
            setCurrent(macro, name);
            return macro;
        }
        Class childClass = (Class)_typedefs.get(name);
        Method addChild = null;
        try {
            if (childClass != null) {
    	        return addChild(parentOD, childClass, name, null);
            } else {
                addChild = parentOD.getCreateMethod(name);
    	        if (addChild != null) {
    	            child = addChild.invoke(parent, new Object[0]);
    	            setCurrent(child, name);
    	            return child;
    	        }
    	        addChild = parentOD.getAddMethod(name);
    	        if (addChild != null) {
    	            childClass = addChild.getParameterTypes()[0];
    	            child = childClass.newInstance();
    	            addChild.invoke(parent, new Object[] {child});
    	            setCurrent(child, name);
    	            return child;
    	        }
    	        addChild = parentOD.getAddConfiguredMethod(name);
    	        if (addChild != null) {
    	            childClass = addChild.getParameterTypes()[0];
    	            child = childClass.newInstance();
    	            setCurrent(child, name);
    	            return child;
    	        }
            }
        } catch (InstantiationException ex) {
            throw new IllegalArgumentException("no default constructor on "+childClass+" for adding "+name+" on "+parent.getClass());
        } catch (Exception ex) {
            IllegalArgumentException iae = new IllegalArgumentException("bad method found for "+name+" on "+parent.getClass());
            iae.initCause(ex);
            throw iae;
        }
        throw new IllegalArgumentException("no appropriate method found for adding "+name+" on "+parent.getClass());
    }
    
    public void addChild(String name, Object child) {
        if (_objectStack.isEmpty()) {
            throw new IllegalStateException("set root before creating child");
        }
        ObjectDescriptor parentOD = (ObjectDescriptor)_objectStack.peek();
        try {
            addChild(parentOD, child.getClass(), name, child);
        } catch (InstantiationException ex) {
            throw new IllegalArgumentException("no default constructor on "+child.getClass()+" for adding "+name+" on "+parentOD.getObject().getClass());
        } catch (Exception ex) {
            IllegalArgumentException iae = new IllegalArgumentException("bad method found for "+name+" on "+parentOD.getObject().getClass());
            iae.initCause(ex);
            throw iae;
        }
    }

    private Object addChild(ObjectDescriptor parentOD, Class childClass, String name, Object child) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Object parent = parentOD.getObject();
        Method addChild;
        addChild = parentOD.getAddMethod(childClass);
        if (addChild != null) {
            if (child == null) {
                child = childClass.newInstance();
            }
            addChild.invoke(parent, new Object[] {child});
            setCurrent(child, name);
            return child;
        }
        addChild = parentOD.getAddConfiguredMethod(childClass);
        if (addChild != null) {
            if (child == null) {
                child = childClass.newInstance();
            }
            setCurrent(child, name);
            return child;
        }
        throw new IllegalArgumentException("no appropriate method found for adding "+name+" on "+parent.getClass());
    }
    
    public boolean isTopLevelMacroRecord() {
        if (_objectStack.isEmpty()) {
            return false;
        }
        ObjectDescriptor od = (ObjectDescriptor)_objectStack.peek();
        return (od.getObject() instanceof MacroDef);
    }
    
    public void setAttribute(String attributeName, String value) {
        if (_objectStack.isEmpty()) {
            throw new IllegalStateException("set root before setting attribute");
        }
        ObjectDescriptor od = (ObjectDescriptor)_objectStack.peek();
        if (od.getObject() instanceof Macro) {
            ((Macro)od.getObject()).defineAttribute(attributeName, value);
            return;
        }
        if (od.getObject() instanceof MacroRecord) {
            ((MacroRecord)od.getObject()).recordAttribute(attributeName, value);
            return;
        }
        Method m = od.getSetMethod(attributeName);
        if (m == null) {
            throw new IllegalArgumentException("no set method found for "+attributeName+" on "+od.getObject().getClass());
        }
        Object convertedValue = null;
        Class paramClass = m.getParameterTypes()[0];
		try {
            if (paramClass.equals(String.class)) {
                convertedValue = value;
            } else if (paramClass.equals(Boolean.class) || paramClass.equals(boolean.class)) {
                convertedValue = Boolean.valueOf(TRUE_VALUES.contains(value));
            } else if (paramClass.equals(Character.class) || paramClass.equals(char.class)) {
                convertedValue = new Character(value.length() > 0 ? value.charAt(0) : ' ');
            } else if (paramClass.equals(Short.class) || paramClass.equals(short.class)) {
                convertedValue = Short.valueOf(value);
            } else if (paramClass.equals(Integer.class) || paramClass.equals(int.class)) {
                convertedValue = Integer.valueOf(value);
            } else if (paramClass.equals(Long.class) || paramClass.equals(long.class)) {
                convertedValue = Long.valueOf(value);
            } else if (paramClass.equals(Class.class)) {
                convertedValue = Class.forName(value);
            } else {
                convertedValue = paramClass.getConstructor(new Class[] {String.class}).newInstance(new Object[] {value});
            }
        } catch (Exception ex) {
            IllegalArgumentException iae = new IllegalArgumentException("impossible to convert "+value+" to "+paramClass+" for setting "+attributeName+" on "+od.getObject().getClass());
            iae.initCause(ex);
            throw iae;
        }
        try {
            m.invoke(od.getObject(), new Object[] {convertedValue});
        } catch (Exception ex) {
            IllegalArgumentException iae = new IllegalArgumentException("impossible to set "+attributeName+" to "+convertedValue+" on "+od.getObject().getClass());
            iae.initCause(ex);
            throw iae;
        }
    }
    
    public void addText(String text) {
        if (_objectStack.isEmpty()) {
            throw new IllegalStateException("set root before adding text");
        }
        ObjectDescriptor od = (ObjectDescriptor)_objectStack.peek();
        try {
            od.getObject().getClass().getMethod("addText", new Class[] {String.class}).invoke(od.getObject(), new Object[] {text});
        } catch (Exception ex) {
            IllegalArgumentException iae = new IllegalArgumentException("impossible to add text on "+od.getObject().getClass());
            iae.initCause(ex);
            throw iae;
        } 
    }
    
    /**
     * 
     * @return the finished child 
     */
    public Object endCreateChild() {
        if (_objectStack.isEmpty()) {
            throw new IllegalStateException("set root before ending child");
        }
        ObjectDescriptor od = (ObjectDescriptor)_objectStack.pop();
        if (_objectStack.isEmpty()) {
            _objectStack.push(od); // back to previous state
            throw new IllegalStateException("cannot end root");
        }
        if (od.getObject() instanceof Macro) {
            return ((Macro)od.getObject()).play(this);
        }
        ObjectDescriptor parentOD = (ObjectDescriptor)_objectStack.peek();
        String name = od.getObjectName();
        Class childClass = (Class)_typedefs.get(name);
        Method m = null;
        if (childClass != null) {
            m = parentOD.getAddConfiguredMethod(childClass);
        } else {
            m = parentOD.getAddConfiguredMethod(name);
        }
        try {
            if (m != null) {
                m.invoke(parentOD.getObject(), new Object[] {od.getObject()});
            }
            return od.getObject();
        } catch (Exception ex) {
            IllegalArgumentException iae = new IllegalArgumentException("impossible to add configured child for "+name+" on "+parentOD.getObject().getClass());
            iae.initCause(ex);
            throw iae;
        }
    }
    
    public Object getCurrent() {
        return _objectStack.isEmpty()?null:((ObjectDescriptor)_objectStack.peek()).getObject();
    }

    public int getDepth() {
        return _objectStack.size();
    }

    public MacroDef startMacroDef(String macroName) {
        MacroDef macroDef = new MacroDef(macroName);
        setCurrent(macroDef, macroName);
        return macroDef;
    }

    public void addMacroAttribute(String attName, String attDefaultValue) {
        ((MacroDef)getCurrent()).addAttribute(attName, attDefaultValue);
    }

    public void addMacroElement(String elementName, boolean optional) {
        ((MacroDef)getCurrent()).addElement(elementName, optional);
    }

    public void endMacroDef() {
        addConfiguredMacrodef(((MacroDef)getCurrent()));
        _objectStack.pop();
    }

    public void addConfiguredMacrodef(MacroDef macrodef) {
        _macrodefs.put(macrodef.getName(), macrodef);
    }

    public Class getTypeDef(String name) {
        return (Class)_typedefs.get(name);
    }
}
