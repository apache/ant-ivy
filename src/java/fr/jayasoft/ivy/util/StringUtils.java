/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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

    /**
     * Joins the given object array in one string, each separated by the given separator.
     * Example: join(new String[] {"one", "two", "three"}, ", ") -> "one, two, three"
     * 
     * @param objs
     * @param sep
     * @return
     */
    public static String join(Object[] objs, String sep) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < objs.length; i++) {
            buf.append(objs[i]).append(sep);
        }
        if (objs.length > 0) { 
            buf.setLength(buf.length() - sep.length()); // delete sep
        }
        return buf.toString();
    }
    
    
    // basic string codec (same algo as CVS passfile, inspired by ant CVSPass class
    /** Array contain char conversion data */
    private final static char[] SHIFTS = {
          0,   1,   2,   3,   4,   5,   6,   7,   8,   9,  10,  11,  12,  13,  14,  15,
         16,  17,  18,  19,  20,  21,  22,  23,  24,  25,  26,  27,  28,  29,  30,  31,
        114, 120,  53,  79,  96, 109,  72, 108,  70,  64,  76,  67, 116,  74,  68,  87,
        111,  52,  75, 119,  49,  34,  82,  81,  95,  65, 112,  86, 118, 110, 122, 105,
         41,  57,  83,  43,  46, 102,  40,  89,  38, 103,  45,  50,  42, 123,  91,  35,
        125,  55,  54,  66, 124, 126,  59,  47,  92,  71, 115,  78,  88, 107, 106,  56,
         36, 121, 117, 104, 101, 100,  69,  73,  99,  63,  94,  93,  39,  37,  61,  48,
         58, 113,  32,  90,  44,  98,  60,  51,  33,  97,  62,  77,  84,  80,  85, 223,
        225, 216, 187, 166, 229, 189, 222, 188, 141, 249, 148, 200, 184, 136, 248, 190,
        199, 170, 181, 204, 138, 232, 218, 183, 255, 234, 220, 247, 213, 203, 226, 193,
        174, 172, 228, 252, 217, 201, 131, 230, 197, 211, 145, 238, 161, 179, 160, 212,
        207, 221, 254, 173, 202, 146, 224, 151, 140, 196, 205, 130, 135, 133, 143, 246,
        192, 159, 244, 239, 185, 168, 215, 144, 139, 165, 180, 157, 147, 186, 214, 176,
        227, 231, 219, 169, 175, 156, 206, 198, 129, 164, 150, 210, 154, 177, 134, 127,
        182, 128, 158, 208, 162, 132, 167, 209, 149, 241, 153, 251, 237, 236, 171, 195,
        243, 233, 253, 240, 194, 250, 191, 155, 142, 137, 245, 235, 163, 242, 178, 152
    };
    /**
     * Encrypt the given string in a way which anybody having access to this method
     * algorithm can easily decrypt.
     * 
     * This is useful only to avoid clear string storage in a file for example,
     * but shouldn't be considered as a real mean of security.
     * 
     * This only works with simple characters (char < 256).
     * @param str the string to encrypt
     * @return the encrypted version of the string
     */
    public final static String encrypt(String str) {
    	if (str == null) {
    		return null;
    	}
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= SHIFTS.length) {
            	throw new IllegalArgumentException("encrypt method can only be used with simple characters. '"+c+"' not allowed");
            }
			buf.append(SHIFTS[c]);
        }
        return buf.toString();
    }

    /**
     * Decrypts a string encrypted with encrypt.
     * @param str the encrypted string to decrypt
     * @return
     */
    public final static String decrypt(String str) {
    	if (str == null) {
    		return null;
    	}
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            buf.append(decrypt(str.charAt(i)));
        }
        return buf.toString();
    }

	private static char decrypt(char c) {
		for (char i = 0; i < SHIFTS.length; i++) {
			if (SHIFTS[i] == c) {
				return i;
			}
		}
    	throw new IllegalArgumentException("Impossible to decrypt '"+c+"'. Unhandled character.");
	}

}
