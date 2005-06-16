/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import fr.jayasoft.ivy.url.ApacheURLLister;

public class ApacheHttpURLLister implements URLLister {
    private ApacheURLLister _lister = new ApacheURLLister();
    public boolean accept(String pattern) {
        return pattern.startsWith("http");
    }
    public List listAll(URL url) throws IOException {
        return _lister.listAll(url);
    }

    public String toString() {
        return "apache http lister";
    }
}
