/*
 * This file is subject to the licence found in LICENCE.TXT in the root directory of the project.
 * Copyright Jayasoft 2005 - All rights reserved
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.repository.file;

import java.io.File;

import fr.jayasoft.ivy.repository.Resource;

public class FileResource implements Resource {
    private File _file;

    public FileResource(File f) {
        _file = f;
    }

    public String getName() {
        return _file.getPath();
    }

    public long getLastModified() {
        return _file.lastModified();
    }

    public long getContentLength() {
        return _file.length();
    }

    public boolean exists() {
        return _file.exists();
    }

    public String toString() {
        return getName();
    }

    public File getFile() {
        return _file;
    }
}
