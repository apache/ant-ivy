/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.util.url;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.url.URLHandler.URLInfo;

/**
 * Utility class which helps to list urls under a given url. This has been tested with Apache 1.3.33
 * server listing, as the one used at ibiblio, and with Apache 2.0.53 server listing, as the one on
 * mirrors.sunsite.dk.
 */
public class ApacheURLLister {
    // ~ Static variables/initializers ------------------------------------------

    private static final Pattern PATTERN = Pattern.compile(
        "<a[^>]*href=\"([^\"]*)\"[^>]*>(?:<[^>]+>)*?([^<>]+?)(?:<[^>]+>)*?</a>",
        Pattern.CASE_INSENSITIVE);

    // ~ Methods ----------------------------------------------------------------

    /**
     * Returns a list of sub urls of the given url. The returned list is a list of URL.
     * 
     * @param url
     *            The base URL from which to retrieve the listing.
     * @return a list of sub urls of the given url.
     * @throws IOException
     *             If an error occures retrieving the HTML.
     */
    public List<URL> listAll(URL url) throws IOException {
        return retrieveListing(url, true, true);
    }

    /**
     * Returns a list of sub 'directories' of the given url. The returned list is a list of URL.
     * 
     * @param url
     *            The base URL from which to retrieve the listing.
     * @return a list of sub 'directories' of the given url.
     * @throws IOException
     *             If an error occures retrieving the HTML.
     */
    public List<URL> listDirectories(URL url) throws IOException {
        return retrieveListing(url, false, true);
    }

    /**
     * Returns a list of sub 'files' (in opposition to directories) of the given url. The returned
     * list is a list of URL.
     * 
     * @param url
     *            The base URL from which to retrieve the listing.
     * @return a list of sub 'files' of the given url.
     * @throws IOException
     *             If an error occures retrieving the HTML.
     */
    public List<URL> listFiles(URL url) throws IOException {
        return retrieveListing(url, true, false);
    }

    /**
     * Retrieves a {@link List} of {@link URL}s corresponding to the files and/or directories found
     * at the supplied base URL.
     * 
     * @param url
     *            The base URL from which to retrieve the listing.
     * @param includeFiles
     *            If true include files in the returned list.
     * @param includeDirectories
     *            If true include directories in the returned list.
     * @return A {@link List} of {@link URL}s.
     * @throws IOException
     *             If an error occures retrieving the HTML.
     */
    public List<URL> retrieveListing(URL url, boolean includeFiles, boolean includeDirectories)
            throws IOException {
        List<URL> urlList = new ArrayList<URL>();

        // add trailing slash for relative urls
        if (!url.getPath().endsWith("/") && !url.getPath().endsWith(".html")) {
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath() + "/");
        }

        URLHandler urlHandler = URLHandlerRegistry.getDefault();
        URLInfo urlInfo = urlHandler.getURLInfo(url);
        if (urlInfo == URLHandler.UNAVAILABLE) {
            return urlList; // not found => return empty list
        }
        // here, urlInfo is valid
        String charset = urlInfo.getBodyCharset();

        InputStream contentStream = urlHandler.openStream(url);
        BufferedReader r = null;
        if (charset == null) {
            r = new BufferedReader(new InputStreamReader(contentStream));
        } else {
            r = new BufferedReader(new InputStreamReader(contentStream, charset));
        }

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

            // handle complete URL listings
            if (href.startsWith("http:") || href.startsWith("https:")) {
                try {
                    href = new URL(href).getPath();
                    if (!href.startsWith(url.getPath())) {
                        // ignore URLs which aren't children of the base URL
                        continue;
                    }
                    href = href.substring(url.getPath().length());
                } catch (Exception ignore) {
                    // incorrect URL, ignore
                    continue;
                }
            }

            if (href.startsWith("../")) {
                // we are only interested in sub-URLs, not parent URLs, so skip this one
                continue;
            }

            // absolute href: convert to relative one
            if (href.startsWith("/")) {
                int slashIndex = href.substring(0, href.length() - 1).lastIndexOf('/');
                href = href.substring(slashIndex + 1);
            }

            // relative to current href: convert to simple relative one
            if (href.startsWith("./")) {
                href = href.substring("./".length());
            }

            // exclude those where they do not match
            // href will never be truncated, text may be truncated by apache
            if (text.endsWith("..>")) {
                // text is probably truncated, we can only check if the href starts with text
                if (!href.startsWith(text.substring(0, text.length() - 3))) {
                    continue;
                }
            } else if (text.endsWith("..&gt;")) {
                // text is probably truncated, we can only check if the href starts with text
                if (!href.startsWith(text.substring(0, text.length() - 6))) {
                    continue;
                }
            } else {
                // text is not truncated, so it must match the url after stripping optional
                // trailing slashes
                String strippedHref = href.endsWith("/") ? href.substring(0, href.length() - 1)
                        : href;
                String strippedText = text.endsWith("/") ? text.substring(0, text.length() - 1)
                        : text;
                if (!strippedHref.equalsIgnoreCase(strippedText)) {
                    continue;
                }
            }

            boolean directory = href.endsWith("/");

            if ((directory && includeDirectories) || (!directory && includeFiles)) {
                URL child = new URL(url, href);
                urlList.add(child);
                Message.debug("ApacheURLLister found URL=[" + child + "].");
            }
        }

        return urlList;
    }
}
