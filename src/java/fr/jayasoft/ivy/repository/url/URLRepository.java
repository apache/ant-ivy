/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository.url;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import fr.jayasoft.ivy.repository.AbstractRepository;
import fr.jayasoft.ivy.repository.RepositoryCopyProgressListener;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.repository.TransferEvent;
import fr.jayasoft.ivy.url.ApacheURLLister;
import fr.jayasoft.ivy.util.FileUtil;

public class URLRepository extends AbstractRepository {
    private RepositoryCopyProgressListener _progress = new RepositoryCopyProgressListener(this);
    private Map _resourcesCache = new HashMap();

    public Resource getResource(String source) throws IOException {
        Resource res = (Resource)_resourcesCache.get(source);
        if (res == null) {
            res = new URLResource(new URL(source));
            _resourcesCache.put(source, res);
        }
        return res;
    }

    public void get(String source, File destination) throws IOException {
        fireTransferInitiated(getResource(source), TransferEvent.REQUEST_GET);
        try {
            Resource res = getResource(source);
            long totalLength = res.getContentLength();
            if (totalLength > 0) {
                _progress.setTotalLength(new Long(totalLength));
            }
            FileUtil.copy(new URL(source), destination, _progress);
        } catch (IOException ex) {
            fireTransferError(ex);
            throw ex;
        } catch (RuntimeException ex) {
            fireTransferError(ex);
            throw ex;
        } finally {
            _progress.setTotalLength(null);
        }
    }

    public void put(File source, String destination, boolean overwrite) throws IOException {
        throw new UnsupportedOperationException("URL repository is not able to put files for the moment");
    }

    private ApacheURLLister _lister = new ApacheURLLister();
    public List list(String parent) throws IOException {
        if (parent.startsWith("http")) {
            List urls = _lister.listAll(new URL(parent));
            if (urls != null) {
                List ret = new ArrayList(urls.size());
                for (ListIterator iter = urls.listIterator(); iter.hasNext();) {
                    URL url = (URL)iter.next();
                    ret.add(url.toExternalForm());
                }
                return ret;
            }
        } else if (parent.startsWith("file")) {
            String path = new URL(parent).getPath();
            File file = new File(path);
            if (file.exists() && file.isDirectory()) {
                String[] files = file.list();
                List ret = new ArrayList(files.length);
                URL context = path.endsWith("/") ? new URL(parent) : new URL(parent+"/");
                for (int i = 0; i < files.length; i++) {
                    ret.add(new URL(context, files[i]).toExternalForm());
                }
                return ret;
            } else {
                return Collections.EMPTY_LIST;
            }

        }
        return null;
    }

}
