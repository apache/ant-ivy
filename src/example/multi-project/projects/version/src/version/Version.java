package version;

import java.io.InputStream;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

public class Version {
  static {
    versions = new HashMap();
    register("version");
  }
  
  private static Map versions;
  
  public static void register(String module) {
    try {
      InputStream moduleVersion = Version.class.getResourceAsStream("/"+module+".properties");
      Properties props = new Properties();
      props.load(moduleVersion);
      String version = (String)props.get("version");
      versions.put(module, version);
      System.out.println("--- using "+module+" v"+version);
    } catch (Exception ex) {
      System.err.println("an error occured while registering "+module+": "+ex.getMessage());
      ex.printStackTrace();
    }
  }
}
