/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.jayasoft.ivy.repository.AbstractRepository;
import fr.jayasoft.ivy.repository.RepositoryCopyProgressListener;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.repository.TransferEvent;
import fr.jayasoft.ivy.util.FileUtil;

public class FileRepository extends AbstractRepository {
    private RepositoryCopyProgressListener _progress = new RepositoryCopyProgressListener(this);
    private File _baseDir;
    private boolean _local = true;

    public FileRepository() {
        _baseDir = null;
    }

    public FileRepository(File basedir) {
        _baseDir = basedir;
    }

    public Resource getResource(String source) throws IOException {
        return new FileResource(this, getFile(source));
    }

    public void get(String source, File destination) throws IOException {
        fireTransferInitiated(getResource(source), TransferEvent.REQUEST_GET);
        copy(getFile(source), destination, true);
    }

    public void put(File source, String destination, boolean overwrite) throws IOException {
        fireTransferInitiated(getResource(destination), TransferEvent.REQUEST_PUT);
        copy(source, getFile(destination), overwrite);
    }

    private void copy(File src, File destination, boolean overwrite) throws IOException {
        try {
            _progress.setTotalLength(new Long(src.length()));
            FileUtil.copy(src, destination, _progress, overwrite);
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

    public List list(String parent) throws IOException {
        File dir = getFile(parent);
        if (dir.exists() && dir.isDirectory()) {
            String[] names = dir.list();
            if (names != null) {
                List ret = new ArrayList(names.length);
                for (int i = 0; i < names.length; i++) {
                    ret.add(parent+getFileSeparator()+names[i]);
                }
                return ret;
            }
        }
        return null;
    }
    
    private File getFile(String source) {
        if (_baseDir != null) {
            return new File(_baseDir, source);
        } else {
            return new File(source);
        }
    }

	public boolean isLocal() {
		return _local;
	}

	public void setLocal(boolean local) {
		_local = local;
	}

}
