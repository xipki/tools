package org.xipki.apppackage;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.*;

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

  public PackageConf(File confFile) throws IOException  {
    Properties props = new Properties();
    try (Reader reader = new FileReader(confFile)) {
      props.load(reader);
    }

    String value = props.getProperty("unpackZipFiles");
    if (value == null) {
      this.unpackZipFiles = new ArrayList<>(1);
    } else {
      String[] _unpackZipFiles = value.split(", ");
      this.unpackZipFiles = new ArrayList<>(_unpackZipFiles.length);
      for (String path : _unpackZipFiles) {
        this.unpackZipFiles.add(MyUtil.toUnixPath(path));
      }
    }

    value = props.getProperty("posixPermissions");
    if (value == null) {
      posixPermissions = new HashMap<>(1);
    } else {
      String[] _posixPermissions = value.split(", ");
      this.posixPermissions = new HashMap<>(_posixPermissions.length);
      for (String permission : _posixPermissions) {
        String[] tokens = permission.split(":");
        this.posixPermissions.put(MyUtil.toUnixPath(tokens[0]), Integer.parseInt(tokens[1]));
      }
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
