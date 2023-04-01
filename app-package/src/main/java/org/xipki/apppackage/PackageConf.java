package org.xipki.apppackage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackageConf {

  private List<String> unpackZipFiles;

  private Map<String, Integer> posixPermissions;

  public List<String> getUnpackZipFiles() {
    return unpackZipFiles;
  }

  public void setUnpackZipFiles(List<String> unpackZipFiles) {
    this.unpackZipFiles = unpackZipFiles;
  }

  public Map<String, Integer> getPosixPermissions() {
    return posixPermissions;
  }

  public void setPosixPermissions(Map<String, Integer> posixPermissions) {
    this.posixPermissions = posixPermissions;
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

    if (posixPermissions == null) {
      posixPermissions = new HashMap<>(1);
    } else {
      Map<String, Integer> canonicalPaths = new HashMap<>(posixPermissions.size());
      for (Map.Entry<String, Integer> entry : posixPermissions.entrySet()) {
        canonicalPaths.put(MyUtil.toUnixPath(entry.getKey()), entry.getValue());
      }
      this.posixPermissions = canonicalPaths;
    }
  }

  public boolean unzipMe(Path baseDir, Path zipFilePath) {
    String canonicalPath = MyUtil.toUnixPath(baseDir, zipFilePath);
    return unpackZipFiles.contains(canonicalPath);
  }

  public Integer posixPermission(Path baseDir, Path filePath) {
    String canonicalPath = MyUtil.toUnixPath(baseDir, filePath);
    return posixPermissions.get(canonicalPath);
  }


}
