package org.xipki.apppackage;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PackageConf {

  private Map<String, Integer> permissions;

  public Map<String, Integer> getPermissions() {
    return permissions;
  }

  public void setPermissions(Map<String, Integer> permissions) {
    this.permissions = permissions;
  }

  public void init() {
    if (permissions == null) {
      permissions = new HashMap<>(1);
    } else {
      Map<String, Integer> canonicalPaths = new HashMap<>(permissions.size());
      for (Map.Entry<String, Integer> entry : permissions.entrySet()) {
        canonicalPaths.put(MyUtil.toUnixPath(entry.getKey()), entry.getValue());
      }
      this.permissions = canonicalPaths;
    }
  }

  public Integer posixPermission(Path baseDir, Path filePath) {
    String canonicalPath = MyUtil.toUnixPath(baseDir, filePath);
    return permissions.get(canonicalPath);
  }


}
