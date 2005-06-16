/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package depending;

/**
 * TODO write javadoc
 */
public class Main {
    public static void main(String[] args) {
        String standaloneVersion = standalone.Main.getVersion();
        if (standaloneVersion!=null) {
            System.out.println("you are using version " + standaloneVersion + " of class " + standalone.Main.class.getName());
        } else {
            System.err.println("failed to get version of " + standalone.Main.class.getName());
        }
        String message = "i am " + Main.class.getName() + " and " + standalone.Main.class.getName() + " will do the job for me";
        System.out.println("standard message : " + message);
        System.out.println("capitalized message : " + standalone.Main.capitalizeWords(message));
    }
}
