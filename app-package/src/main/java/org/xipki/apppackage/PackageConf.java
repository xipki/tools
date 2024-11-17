package org.xipki.apppackage;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PackageConf {

  enum SuffixMode {
    NONE,
    EXTENSION,
    FILENAME
  }

  private SuffixMode suffixMode;

  private Set<String> zipExtensions;

  private Set<String> unpackZipFiles;

  private Map<String, Integer> posixPermissions;

  private final Set<String> dosLineEncodings;

  private final Set<String> unixLineEncodings;

  public PackageConf(File confFile) throws IOException  {
    Properties props = new Properties();
    try (Reader reader = new FileReader(confFile)) {
      props.load(reader);
    }

    String value = props.getProperty("suffixMode");
    suffixMode = (value == null || value.isEmpty()) ? SuffixMode.FILENAME : SuffixMode.valueOf(value);

    value = props.getProperty("zipExtensions");
    if (value == null) {
      value = "war,zip,ear";
    }
    this.zipExtensions = splitStr(value);

    value = props.getProperty("unpackZipFiles");
    this.unpackZipFiles = new HashSet<>();
    if (value != null) {
      Set<String> tokens = splitStr(value);
      for (String path : tokens) {
        this.unpackZipFiles.add(MyUtil.toUnixPath(path));
      }
    }

    value = props.getProperty("posixPermissions");
    this.posixPermissions = new HashMap<>();
    if (value != null) {
      Set<String> _posixPermissions = splitStr(value);
      for (String permission : _posixPermissions) {
        String[] tokens = permission.split(":");
        this.posixPermissions.put(MyUtil.toUnixPath(tokens[0]), Integer.parseInt(tokens[1]));
      }
    }

    value = props.getProperty("lineEnding.dos");
    this.dosLineEncodings = new HashSet<>();
    if (value != null) {
      Set<String> _files = splitStr(value);
      for (String m : _files) {
        this.dosLineEncodings.add(m.toLowerCase());
      }
    }

    value = props.getProperty("lineEnding.unix");
    this.unixLineEncodings = new HashSet<>();
    if (value != null) {
      Set<String> _files = splitStr(value);
      for (String m : _files) {
        this.unixLineEncodings.add(m.toLowerCase());
      }
    }
  }

  public String getSuffix(Path path) {
    switch (suffixMode) {
      case NONE:
        return "";
      case FILENAME:
        return "." + path.getFileName().toString();
      default: // Extension
        String extension = getExtension(path.getFileName().toString());
        return extension == null || extension.isEmpty() ? "" : "." + extension;
    }
  }

  public boolean unzipMe(Path baseDir, Path zipFilePath) {
    String extension = getExtension(zipFilePath.getFileName().toString());
    if (!this.zipExtensions.contains(extension)) {
      return false;
    }
    return null != getMatchElement(baseDir, zipFilePath, extension, unpackZipFiles);
  }

  public Integer posixPermission(Path baseDir, Path filePath) {
    String str = getMatchElement(baseDir, filePath, getExtension(filePath.getFileName().toString()),
        posixPermissions.keySet());
    return str == null ? null : posixPermissions.get(str);
  }

  public boolean isDosLineEncoding(Path baseDir, Path filePath)
  {
    String extension = getExtension(filePath.getFileName().toString());
    String path = getMatchElement(baseDir, filePath, extension, dosLineEncodings);
    return path != null;
  }

  public boolean isUnixLineEncoding(Path baseDir, Path filePath)
  {
    String extension = getExtension(filePath.getFileName().toString());
    String path = getMatchElement(baseDir, filePath, extension, unixLineEncodings);
    return path != null;
  }

  private static String getMatchElement(Path baseDir, Path filePath, String extension, Collection<String> coll) {
    String canonicalPath = MyUtil.toUnixPath(baseDir, filePath);
    // full pah mach
    if (coll.contains(canonicalPath)) {
      return canonicalPath;
    }

    Path p = Paths.get(canonicalPath);
    if (extension != null) {
      String a = p.getParent() + "/*";
      String str = a + "." + extension;
      if (coll.contains(str)) {
        return str;
      }

      if (coll.contains(a)) {
        return a;
      }

      str = "*." + extension;
      if (coll.contains(str)) {
        return str;
      }
    }

    return null;
  }

  static String getExtension(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex != -1 && dotIndex != fileName.length() - 1) {
      return fileName.substring(dotIndex + 1);
    }
    return null;
  }

  private static Set<String> splitStr(String text) {
    StringTokenizer tokenizer = new StringTokenizer(text, ", \t");
    Set<String> ret = new HashSet<>(tokenizer.countTokens() * 3 / 2);
    while (tokenizer.hasMoreTokens()) {
      ret.add(tokenizer.nextToken());
    }
    return ret;
  }

}
