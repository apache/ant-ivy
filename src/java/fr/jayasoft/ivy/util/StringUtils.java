/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

/**
 * Convenient class used only for uncapitalization
 * Usually use commons lang but here we do not want to have such 
 * a dependency for only one feature
 * 
 * @author X. Hanin
 *
 */
public class StringUtils {
    public static String uncapitalize(String string) {
        if (string == null || string.length() == 0) {
            return string;
        }
        if (string.length() == 1) {
            return string.toLowerCase();
        }
        return string.substring(0,1).toLowerCase() + string.substring(1);
    }

}
