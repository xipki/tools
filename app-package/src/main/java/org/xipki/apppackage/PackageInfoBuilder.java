package org.xipki.apppackage;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class PackageInfoBuilder {

  private static final String pathSep = File.separator;

  private final List<String> folders = new LinkedList<>();

  private final Map<String, FileInfo> files = new HashMap<>();

  public void addFolder(Path baseSrcDir, Path folder) {
    String path = folder.subpath(baseSrcDir.getNameCount(), folder.getNameCount()).toString();
    if (!pathSep.equals("/")) {
      path = path.replace(pathSep, "/");
    }

    folders.add(path);
  }

  public boolean addFile(byte[] sha256, Path relativePath, Integer intPermission) {
    String hexSha256 = bytesToHex(sha256);
    FileInfo fileInfo = files.get(hexSha256);

    boolean newEntry = false;
    if (fileInfo == null) {
      fileInfo = new FileInfo();
      fileInfo.setPathInfos(new LinkedList<>());
      fileInfo.setSha256(hexSha256);
      files.put(hexSha256, fileInfo);
      newEntry = true;
    }

    PathInfo pathInfo = new PathInfo();
    fileInfo.getPathInfos().add(pathInfo);

    String path = relativePath.toString();
    if (!pathSep.equals("/")) {
      path = path.replace(pathSep, "/");
    }
    pathInfo.setPath(path);

    if (intPermission != null) {
      pathInfo.setUnixPermissions(intPermission);
    }

    return newEntry;
  }

  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  public PackageInfo build() {
    PackageInfo packageInfo = new PackageInfo();

    List<FileInfo> fileInfos = new ArrayList<>(files.size());

    for (Map.Entry<String, FileInfo> entry : files.entrySet()) {
      fileInfos.add(entry.getValue());
    }

    packageInfo.setFiles(fileInfos);
    packageInfo.setFolders(folders);
    return packageInfo;
  }

}
