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
package org.apache.ivy.plugins.repository.url;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.plugins.repository.AbstractRepository;
import org.apache.ivy.plugins.repository.RepositoryCopyProgressListener;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.TransferEvent;
import org.apache.ivy.util.FileUtil;
import org.apache.ivy.util.url.ApacheURLLister;

public class URLRepository extends AbstractRepository {
    private RepositoryCopyProgressListener progress = new RepositoryCopyProgressListener(this);

    private final Map<String, Resource> resourcesCache = new HashMap<>();

    public Resource getResource(String source) throws IOException {
        Resource res = resourcesCache.get(source);
        if (res == null) {
            res = new URLResource(new URL(source));
            resourcesCache.put(source, res);
        }
        return res;
    }

    public void get(String source, File destination) throws IOException {
        fireTransferInitiated(getResource(source), TransferEvent.REQUEST_GET);
        try {
            Resource res = getResource(source);
            long totalLength = res.getContentLength();
            if (totalLength > 0) {
                progress.setTotalLength(totalLength);
            }
            FileUtil.copy(new URL(source), destination, progress);
        } catch (IOException | RuntimeException ex) {
            fireTransferError(ex);
            throw ex;
        } finally {
            progress.setTotalLength(null);
        }
    }

    public void put(File source, String destination, boolean overwrite) throws IOException {
        if (!overwrite && getResource(destination).exists()) {
            throw new IOException("destination file exists and overwrite == false");
        }

        fireTransferInitiated(getResource(destination), TransferEvent.REQUEST_PUT);
        try {
            long totalLength = source.length();
            if (totalLength > 0) {
                progress.setTotalLength(totalLength);
            }
            FileUtil.copy(source, new URL(destination), progress);
        } catch (IOException | RuntimeException ex) {
            fireTransferError(ex);
            throw ex;
        } finally {
            progress.setTotalLength(null);
        }
    }

    private ApacheURLLister lister = new ApacheURLLister();

    public List<String> list(String parent) throws IOException {
        if (parent.startsWith("http")) {
            List<URL> urls = lister.listAll(new URL(parent));
            if (urls != null) {
                List<String> ret = new ArrayList<>(urls.size());
                for (URL url : urls) {
                    ret.add(url.toExternalForm());
                }
                return ret;
            }
        } else if (parent.startsWith("file")) {
            String path;
            try {
                URI uri = new URI(parent);
                if (uri.isOpaque()) {
                    path = uri.getSchemeSpecificPart();
                } else {
                    path = uri.getPath();
                }
            } catch (URISyntaxException e) {
                throw new IOException("Couldn't list content of '" + parent + "'", e);
            }

            File file = new File(path);
            if (file.exists() && file.isDirectory()) {
                String[] files = file.list();
                List<String> ret = new ArrayList<>(files.length);
                URL context = path.endsWith("/") ? new URL(parent) : new URL(parent + "/");
                for (String fileName : files) {
                    ret.add(new URL(context, fileName).toExternalForm());
                }
                return ret;
            } else {
                return Collections.emptyList();
            }

        }
        return null;
    }

}
