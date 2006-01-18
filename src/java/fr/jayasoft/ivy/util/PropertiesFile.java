package fr.jayasoft.ivy.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * A simple Properties extension easing the loading and saving of data
 */
public class PropertiesFile extends Properties {
    private File _file;
    private String _header;

    public PropertiesFile(File file, String header) {
        _file = file;
        _header = header;
        if (_file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(_file);
                load(fis);                    
            } catch (Exception ex) {
                Message.warn("exception occured while reading properties file "+_file+": "+ex.getMessage());
            }
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public void save() {
        FileOutputStream fos = null;
        try {
            if (_file.getParentFile() != null && !_file.getParentFile().exists()) {
                _file.getParentFile().mkdirs();
            }
            fos = new FileOutputStream(_file);
            store(fos, _header);
        } catch (Exception ex) {
            Message.warn("exception occured while writing properties file "+_file+": "+ex.getMessage());
        }
        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
        }
    }

}