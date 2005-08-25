package size;

import version.Version;
import java.util.Collection;
import java.util.Iterator;
import java.io.File;

public class FileSize {
  static {
    Version.register("size");
  }

  public static long totalSize(File dir) {
    return totalSize(list.ListFile.list(dir));
  }
  
  public static long totalSize(Collection files) {
    long total = 0;
    for (Iterator it = files.iterator(); it.hasNext(); ) {
      File f = (File)it.next();
      total += f.length();
    }
    return total;
  }  
}
