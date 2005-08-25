package list;

import version.Version;
import java.util.Collection;
import java.util.ArrayList;
import java.io.File;

public class ListFile {
  static {
    Version.register("list");
  }
  
  public static Collection list(File dir) {
    Collection files = new ArrayList();
    
    return list(dir, files);
  }
  
  private static Collection list(File file, Collection files) {
    if (file.isDirectory()) {
      File[] f = file.listFiles();
      for (int i=0; i<f.length; i++) {
        list(f[i], files);
      }
    } else {
      files.add(file);
    }
    return files;
  }
}
