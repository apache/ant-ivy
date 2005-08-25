package sizewhere;

import version.Version;
import size.FileSize;
import find.FindFile;

import java.util.Collection;
import java.util.ArrayList;
import java.io.File;

public class SizeWhere {
  static {
    Version.register("sizewhere");
  }
  
  public static long totalSize(File dir, String name) {
    return FileSize.totalSize(FindFile.find(dir, name));
  }
}
