/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
        Object child = null;
        Class childClass = (Class)_typedefs.get(name);
        Method addChild = null;
        try {
            if (childClass != null) {
    	        addChild = parentOD.getAddMethod(childClass);
    	        if (addChild != null) {
    	            child = childClass.newInstance();
    	            addChild.invoke(parent, new Object[] {child});
    	            setCurrent(child, name);
    	            return child;
    	        }
    	        addChild = parentOD.getAddConfiguredMethod(childClass);
    	        if (addChild != null) {
    	            child = childClass.newInstance();
    	            setCurrent(child, name);
    	            return child;
    	        }
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
    
    public void setAttribute(String attributeName, String value) {
        if (_objectStack.isEmpty()) {
            throw new IllegalStateException("set root before setting attribute");
        }
        ObjectDescriptor od = (ObjectDescriptor)_objectStack.peek();
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
}
