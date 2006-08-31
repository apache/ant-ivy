/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.url;

import fr.jayasoft.ivy.util.FileUtil;
import fr.jayasoft.ivy.util.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utility class which helps to list urls under a given url. This has been
 * tested with Apache 1.3.33 server listing, as the one used at ibiblio, and
 * with Apache 2.0.53 server listing, as the one on mirrors.sunsite.dk.
 *
 * @author Glen Marchesani
 * @author Xavier Hanin
 * @author <a href="mailto:johnmshields@yahoo.com">John M. Shields</a>
 */
public class ApacheURLLister {
    //~ Static variables/initializers ------------------------------------------

    private static final Pattern PATTERN =
        Pattern.compile("<a[^>]*href=\"([^\"]*)\"[^>]*>(?:<[^>]+>)*?([^<>]+?)(?:<[^>]+>)*?</a>",
            Pattern.CASE_INSENSITIVE);

    //~ Methods ----------------------------------------------------------------

    /**
     * Returns a list of sub urls of the given url. The returned list is a list
     * of URL.
     *
     * @param url The base URL from which to retrieve the listing.
     *
     * @return a list of sub urls of the given url.
     *
     * @throws IOException If an error occures retrieving the HTML.
     */
    public List listAll(URL url) throws IOException {
        return retrieveListing(url, true, true);
    }


    /**
     * Returns a list of sub 'directories' of the given url. The returned list
     * is a list of URL.
     *
     * @param url The base URL from which to retrieve the listing.
     *
     * @return a list of sub 'directories' of the given url.
     *
     * @throws IOException If an error occures retrieving the HTML.
     */
    public List listDirectories(URL url) throws IOException {
        return retrieveListing(url, false, true);
    }


    /**
     * Returns a list of sub 'files' (in opposition to directories) of the
     * given url. The returned list is a list of URL.
     *
     * @param url The base URL from which to retrieve the listing.
     *
     * @return a list of sub 'files' of the given url.
     *
     * @throws IOException If an error occures retrieving the HTML.
     */
    public List listFiles(URL url) throws IOException {
        return retrieveListing(url, true, false);
    }


    /**
     * Retrieves a {@link List} of {@link URL}s corresponding to the files
     * and/or directories found at the supplied base URL.
     *
     * @param url The base URL from which to retrieve the listing.
     * @param includeFiles If true include files in the returned list.
     * @param includeDirectories If true include directories in the returned
     *        list.
     *
     * @return A {@link List} of {@link URL}s.
     *
     * @throws IOException If an error occures retrieving the HTML.
     */
    public List retrieveListing(URL url, boolean includeFiles,
        boolean includeDirectories) throws IOException {
        List urlList = new ArrayList();

        // add trailing slash for relative urls
        if (!url.getPath().endsWith("/") && !url.getPath().endsWith(".html")) {
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(),
                    url.getPath() + "/");
        }

        BufferedReader r =
            new BufferedReader(new InputStreamReader(URLHandlerRegistry.getDefault()
                                                                       .openStream(url)));

        String htmlText = FileUtil.readEntirely(r);

        Matcher matcher = PATTERN.matcher(htmlText);

        while (matcher.find()) {
            // get the href text and the displayed text
            String href = matcher.group(1);
            String text = matcher.group(2);
            
            if ((href == null) || (text == null)) {
                // the groups were not found (shouldn't happen, really)
                continue;
            }
            
            text = text.trim(); 

            // absolute href: convert to relative one
            if (href.startsWith("/")) {
                int slashIndex = href.substring(0, href.length() - 1).lastIndexOf('/');
                href= href.substring(slashIndex+1);
            }

            // exclude those where they do not match
            // href will never be truncated, text may be truncated by apache
            // may have a '.' from either the extension (.jar) or "..&gt;"
            int dotIndex = text.indexOf('.');

            if ( ((dotIndex != -1) && !href.startsWith(text.substring(0, dotIndex)))
                || ((dotIndex == -1) && !href.equalsIgnoreCase(text)) ) {
                // the href and the text do not "match"
                continue;
            }

            boolean directory = href.endsWith("/");

            if ((directory && includeDirectories)
                || (!directory && includeFiles)) {
                URL child = new URL(url, href);
                urlList.add(child);
                Message.debug("ApacheURLLister found URL=[" + child + "].");
            }
        }

        return urlList;
    }
}
