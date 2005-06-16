/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.net.URL;

import fr.jayasoft.ivy.url.URLHandler;
import fr.jayasoft.ivy.url.URLHandlerRegistry;

/**
 * TODO write javadoc
 */
public class IBiblioHelper {
    private static boolean _checked = false;
    private static String _mirror = null;
    private static URLHandler handler = URLHandlerRegistry.getHttp();
    public static String getIBiblioMirror() throws Exception {
//        String[] mirrors = new String[] {
//                "http://download.au.kde.org",
//                "http://ftp.up.ac.za",
//                "http://mirrors.sunsite.dk",
//                "http://planetmirror.com",
//                "http://www.ibiblio.org"
//        };
        String[] mirrors = new String[] {
                "http://ftp.up.ac.za/pub/linux/maven",
                "http://mirrors.sunsite.dk/maven",
                "http://public.planetmirror.com/pub/maven",
                "http://www.ibiblio.org/maven"
        };
        String[] mirrorsRoot = new String[] {
                "http://ftp.up.ac.za/pub/linux/maven",
                "http://mirrors.sunsite.dk/maven",
                "http://public.planetmirror.com/pub/maven",
                "http://www.ibiblio.org/maven"
        };
        if (!_checked) {
            long best = -1;
            for (int i = 0; i < mirrors.length; i++) {
                long start = System.currentTimeMillis();
                if (handler.isReachable(new URL(mirrors[i]), 300)) {
                    long took = System.currentTimeMillis() - start;
                    System.out.println("reached "+mirrors[i]+" in "+took+"ms");
                    if (best == -1 || took < best) {
                        best = took;
                        _mirror = mirrorsRoot[i];
                    }
                }
            }
            if (_mirror == null) {
                System.out.println("No ibiblio mirror available: no ibiblio test done");
            }
        }
        return _mirror;
    }
    
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        String biblioMirror = getIBiblioMirror();
        System.out.println("best mirror is "+biblioMirror+ " - found in "+(System.currentTimeMillis() - start)+"ms");
    }

}
