package find;

import version.Version;
import list.ListFile;

import java.util.Collection;
import java.util.ArrayList;
import java.io.File;

import  org.apache.commons.collections.CollectionUtils;
import  org.apache.commons.collections.Predicate;

public class FindFile {
  static {
    Version.register("find");
  }
  
  public static Collection find(File dir, String name) {
    return find(ListFile.list(dir), name);
  }
  
  private static Collection find(Collection files, final String name) {    
    return CollectionUtils.select(files, new Predicate() {
      public boolean evaluate(Object o) {
        return ((File)o).getName().indexOf(name) != -1;
      }
    });
  }
}
