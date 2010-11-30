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
package org.apache.ivy.osgi.obr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

import org.apache.ivy.osgi.obr.xml.OBRXMLParser;
import org.apache.ivy.osgi.repo.BundleRepoResolver;
import org.apache.ivy.osgi.repo.RelativeURLRepository;
import org.apache.ivy.plugins.repository.file.FileRepository;
import org.xml.sax.SAXException;

public class OBRResolver extends BundleRepoResolver {

    private String repoXmlURL;

    private String repoXmlFile;

    public void setRepoXmlFile(String repositoryXmlFile) {
        this.repoXmlFile = repositoryXmlFile;
    }

    public void setRepoXmlURL(String repositoryXmlURL) {
        this.repoXmlURL = repositoryXmlURL;
    }

    protected void ensureInit() {
        if (repoXmlFile != null && repoXmlURL != null) {
            throw new RuntimeException("The OBR repository resolver " + getName()
                    + " couldn't be configured: repoXmlFile and repoXmlUrl cannot be set both");
        }
        if (repoXmlFile != null) {
            File f = new File(repoXmlFile);
            setRepository(new FileRepository(f.getParentFile()));
            FileInputStream in;
            try {
                in = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("The OBR repository resolver " + getName()
                        + " couldn't be configured: the file " + repoXmlFile + " was not found");
            }
            try {
                setRepoDescriptor(OBRXMLParser.parse(in));
            } catch (ParseException e) {
                throw new RuntimeException("The OBR repository resolver " + getName()
                        + " couldn't be configured: the file " + repoXmlFile
                        + " is incorrectly formed (" + e.getMessage() + ")");
            } catch (IOException e) {
                throw new RuntimeException("The OBR repository resolver " + getName()
                        + " couldn't be configured: the file " + repoXmlFile
                        + " could not be read (" + e.getMessage() + ")");
            } catch (SAXException e) {
                throw new RuntimeException("The OBR repository resolver " + getName()
                        + " couldn't be configured: the file " + repoXmlFile
                        + " has incorrect XML (" + e.getMessage() + ")");
            }
            try {
                in.close();
            } catch (IOException e) {
                // don't care
            }
        } else if (repoXmlURL != null) {
            URL url;
            try {
                url = new URL(repoXmlURL);
            } catch (MalformedURLException e) {
                throw new RuntimeException("The OBR repository resolver " + getName()
                        + " couldn't be configured: repoXmlURL '" + repoXmlURL + "' is not an URL");
            }
            URL baseUrl;
            String basePath = "/";
            int i = url.getPath().lastIndexOf("/");
            if (i > 0) {
                basePath = url.getPath().substring(0, i + 1);
            }
            try {
                baseUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), basePath);
            } catch (MalformedURLException e) {
                throw new RuntimeException(
                        "The OBR repository resolver "
                                + getName()
                                + " couldn't be configured: the base url couldn'd be extracted from the url "
                                + url + " (" + e.getMessage() + ")");
            }
            setRepository(new RelativeURLRepository(baseUrl));
            InputStream in;
            try {
                in = url.openStream();
            } catch (IOException e) {
                throw new RuntimeException("The OBR repository resolver " + getName()
                        + " couldn't be configured: the file " + repoXmlURL + " couldn't be read ("
                        + e.getMessage() + ")");
            }
            try {
                setRepoDescriptor(OBRXMLParser.parse(in));
            } catch (ParseException e) {
                throw new RuntimeException("The OBR repository resolver " + getName()
                        + " couldn't be configured: the file " + repoXmlURL
                        + " is incorrectly formed (" + e.getMessage() + ")");
            } catch (IOException e) {
                throw new RuntimeException("The OBR repository resolver " + getName()
                        + " couldn't be configured: the file " + repoXmlURL
                        + " could not be read (" + e.getMessage() + ")");
            } catch (SAXException e) {
                throw new RuntimeException("The OBR repository resolver " + getName()
                        + " couldn't be configured: the file " + repoXmlURL
                        + " has incorrect XML (" + e.getMessage() + ")");
            }
            try {
                in.close();
            } catch (IOException e) {
                // don't care
            }
        } else {
            throw new RuntimeException("The OBR repository resolver " + getName()
                + " couldn't be configured: repoXmlFile or repoXmlUrl is missing");
        }
    }
}
