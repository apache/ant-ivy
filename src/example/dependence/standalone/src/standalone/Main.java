/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package standalone;

import java.util.Properties;

import org.apache.commons.lang.WordUtils;

/**
 * TODO write javadoc
 */
public class Main {
    /**
     * Returns the version of the project
     * @return a string representation of the version, null if the version could not be retreived
     */
    public static String getVersion() {
        Properties p = new Properties();
        try {
            p.load(Main.class.getResourceAsStream("/version.properties"));
            String version = p.getProperty("version");
            if (version != null) {
                return String.valueOf(Integer.parseInt(version));
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Return the same string with all words capitalized.
     * @param str the string conatining the words to capitalize
     * @return null if the string was null, the string with all words capitalized otherwise
     */
    public static String capitalizeWords(String str) {
        System.out.println("    [" + Main.class.getName() + "] capitalizing string \"" + str + "\" using " + WordUtils.class.getName());
        return WordUtils.capitalizeFully(str);
    }
    public static void main(String[] args) {
        String message="sentence to capitalize";
        System.out.println("standard message : " + message);
        System.out.println("capitalized message : " + capitalizeWords(message));
    }
}
