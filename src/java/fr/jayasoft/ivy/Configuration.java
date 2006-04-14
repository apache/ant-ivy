/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;


/**
 * Represents a module configuration
 */
public class Configuration extends DefaultExtendableItem {
    public static class Visibility {
        public static Visibility PUBLIC = new Visibility("public");
        public static Visibility PRIVATE = new Visibility("private");
        public static Visibility getVisibility(String name) {
            if ("private".equals(name)) {
                return PRIVATE;
            } else if ("public".equals(name)) {
                return PUBLIC;
            } else {
                throw new IllegalArgumentException("unknwon visibility "+name);
            }
        }
        private String _name;
        private Visibility(String name) {
            _name = name;
        }
        public String toString() {
            return _name;
        }
    }
    
    private String _name;
    private String _description;
    private String[] _extends;
    private Visibility _visibility;
    private boolean _transitive = true;
    
    /**
     * @param name
     * @param visibility
     * @param description
     * @param ext
     */
    public Configuration(String name, Visibility visibility,
            String description, String[] ext) {
        this(name, visibility, description, ext, true);
    }
    
    /**
     * @param name
     * @param visibility
     * @param description
     * @param ext
     * @param transitive
     */
    public Configuration(String name, Visibility visibility,
            String description, String[] ext, boolean transitive) {
        if (name == null) {
            throw new NullPointerException("null configuration name not allowed");
        }
        if (visibility == null) {
            throw new NullPointerException("null visibility not allowed");
        }
        _name = name;
        _visibility = visibility;
        _description = description;
        if (ext == null) {
            _extends = new String[0];
        } else {
            _extends = new String[ext.length];
            for (int i = 0; i < ext.length; i++) {
                _extends[i] = ext[i].trim();
            }
        }
        _transitive=transitive;
    }
    
    /**
     * @param name
     */
    public Configuration(String name) {
        this(name, Visibility.PUBLIC, null, null);
    }
    
    /**
     * @return Returns the description. It may be null.
     */
    public String getDescription() {
        return _description;
    }
    /**
     * @return Returns the extends. May be empty, but never null.
     */
    public String[] getExtends() {
        return _extends;
    }
    /**
     * @return Returns the name. Never null;
     */
    public String getName() {
        return _name;
    }
    /**
     * @return Returns the visibility. Never null.
     */
    public Visibility getVisibility() {
        return _visibility;
    }
    
    /**
     * @return Returns the transitive.
     */
    public final boolean isTransitive() {
        return _transitive;
    }
   
    public String toString() {
        return _name;
    }
    
    public boolean equals(Object obj) {
        if (! (obj instanceof Configuration)) {
            return false;
        }
        return ((Configuration)obj).getName().equals(getName());
    }
    
    public int hashCode() {
        return getName().hashCode();
    }
}
