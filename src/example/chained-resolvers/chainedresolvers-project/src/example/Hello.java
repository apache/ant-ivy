/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package example;

import org.apache.commons.lang.WordUtils;

/**
 * Simple example world to show how easy it is to retreive libs with ivy !!! 
 */
public class Hello {
    public static void main(String[] args) {
        String  message = "example world !";
        System.out.println("standard message :" + message);
        System.out.println("capitalized by " + WordUtils.class.getName() + " : " + WordUtils.capitalizeFully(message));
        System.out.println("upperCased by " + test.StringUtils.class.getName() + " : " + test.StringUtils.upperCase(message));
    }
}
