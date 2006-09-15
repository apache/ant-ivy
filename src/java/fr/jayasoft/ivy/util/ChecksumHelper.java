package fr.jayasoft.ivy.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class ChecksumHelper {
    
	private static Map _algorithms = new HashMap();
	static {
		_algorithms.put("md5", "MD5");
		_algorithms.put("sha1", "SHA-1");
	}
	
	public static boolean check(File dest, File checksumFile, String algorithm) throws IOException {
		String csFileContent = FileUtil.readEntirely(new BufferedReader(new FileReader(checksumFile))).trim().toLowerCase();
		String expected;
		int spaceIndex = csFileContent.indexOf(' ');
		if (spaceIndex != -1) {
			expected = csFileContent.substring(0, spaceIndex);
		} else {
			expected = csFileContent;
		}
		
		String computed = computeAsString(dest, algorithm).trim().toLowerCase();
		return expected.equals(computed);
	}    
	
    public static String computeAsString(File f, String algorithm) throws IOException {
    	return byteArrayToHexString(compute(f, algorithm));
    }
    
    private static byte[] compute(File f, String algorithm) throws IOException {
    	InputStream is = new FileInputStream(f);

    	try {
    		MessageDigest md = getMessageDigest(algorithm);
    		md.reset();

    		byte[] buf = new byte[2048];
    		int len = 0;
    		while ((len = is.read(buf)) != -1) {
    			md.update(buf, 0, len);
    		}
    		return md.digest();
    	} finally {
    		is.close();
    	}
	}


	private static MessageDigest getMessageDigest(String algorithm) {
		String mdAlgorithm = (String) _algorithms.get(algorithm);
		if (mdAlgorithm == null) {
			throw new IllegalArgumentException("unknown algorithm "+algorithm);
		}
		try {
			return MessageDigest.getInstance(mdAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("unknown algorithm "+algorithm);
		}
	}


	// byte to hex string converter
	private final static char[] CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	/**
	 * Convert a byte[] array to readable string format. This makes the "hex" readable!
	 * @return result String buffer in String format 
	 * @param in byte[] buffer to convert to string format
	 */
    public static String byteArrayToHexString(byte in[]) {
        byte ch = 0x00;

        if (in == null || in.length <= 0) {
            return null;
        }

        StringBuffer out = new StringBuffer(in.length * 2);

        for (int i = 0; i < in.length; i++) {
            ch = (byte) (in[i] & 0xF0); // Strip off high nibble
            ch = (byte) (ch >>> 4); // shift the bits down
            ch = (byte) (ch & 0x0F); // must do this is high order bit is on!

            out.append(CHARS[ (int) ch]); // convert the nibble to a String Character
            ch = (byte) (in[i] & 0x0F); // Strip off low nibble 
            out.append(CHARS[ (int) ch]); // convert the nibble to a String Character
        }

        return out.toString();
    }

}
