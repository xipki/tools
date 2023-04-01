package org.xipki.apppackage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackageConf {

  private List<String> unpackZipFiles;

  private Map<String, Integer> permissions;

  public List<String> getUnpackZipFiles() {
    return unpackZipFiles;
  }

  public void setUnpackZipFiles(List<String> unpackZipFiles) {
    this.unpackZipFiles = unpackZipFiles;
  }

  public Map<String, Integer> getPermissions() {
    return permissions;
  }

  public void setPermissions(Map<String, Integer> permissions) {
    this.permissions = permissions;
  }

  public void init() {
    if (unpackZipFiles == null) {
      unpackZipFiles = new ArrayList<>(1);
    } else {
      List<String> canonicalPaths = new ArrayList<>(unpackZipFiles.size());
      for (String path : unpackZipFiles) {
        canonicalPaths.add(MyUtil.toUnixPath(path));
      }
      this.unpackZipFiles = canonicalPaths;
    }

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

  public boolean unzipMe(Path baseDir, Path zipFilePath) {
    String canonicalPath = MyUtil.toUnixPath(baseDir, zipFilePath);
    return unpackZipFiles.contains(canonicalPath);
  }

  public Integer posixPermission(Path baseDir, Path filePath) {
    String canonicalPath = MyUtil.toUnixPath(baseDir, filePath);
    return permissions.get(canonicalPath);
  }


}
