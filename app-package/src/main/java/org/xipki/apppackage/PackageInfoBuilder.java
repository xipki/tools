package org.xipki.apppackage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PackageInfoBuilder {

  private final List<String> folders = new LinkedList<>();

  private final List<ZipFileInfo> zipFiles = new LinkedList<>();

  private final Map<String, FileInfo> files = new HashMap<>();

  public void addFolder(Path baseSrcDir, Path folder) {
    folders.add(MyUtil.toUnixPath(baseSrcDir, folder));
  }

  public void addZipFile(ZipFileInfo zipFileInfo) {
    zipFiles.add(zipFileInfo);
  }

  public void addFile(byte[] bytes, Path relativePath, Integer intPermission, File targetDir) throws IOException {
    String hexSha256 = MyUtil.hexSha256(bytes);
    FileInfo fileInfo = files.get(hexSha256);

    if (fileInfo == null) {
      fileInfo = new FileInfo();
      fileInfo.setPathInfos(new LinkedList<>());
      fileInfo.setSha256(hexSha256);
      fileInfo.setSize(bytes.length);
      files.put(hexSha256, fileInfo);

      File newEntryFile = new File(targetDir, hexSha256);
      Files.copy(new ByteArrayInputStream(bytes), newEntryFile.toPath());
    }

    PathInfo pathInfo = new PathInfo();
    fileInfo.getPathInfos().add(pathInfo);
    pathInfo.setPath(relativePath.toString());

    if (intPermission != null) {
      pathInfo.setPosixPermissions(intPermission);
    }
  }

  public String addZipEntry(byte[] bytes, String name, File targetDir) throws IOException {
    String hexSha256 = MyUtil.hexSha256(bytes);
    FileInfo fileInfo = files.get(hexSha256);

    if (fileInfo == null) {
      fileInfo = new FileInfo();
      fileInfo.setPathInfos(new LinkedList<>());
      fileInfo.setSha256(hexSha256);
      fileInfo.setSize(bytes.length);
      files.put(hexSha256, fileInfo);

      File newEntryFile = new File(targetDir, hexSha256);
      Files.copy(new ByteArrayInputStream(bytes), newEntryFile.toPath());
    }

    String path = "zip:" + name;
    List<PathInfo> pathInfos = fileInfo.getPathInfos();
    boolean contained = false;
    for (PathInfo pathInfo : pathInfos) {
      if (pathInfo.getPath().equals(path)) {
        contained = true;
        break;
      }
    }

    if (!contained) {
      PathInfo pathInfo = new PathInfo();
      pathInfo.setPath("zip:" + name);
      pathInfos.add(pathInfo);
    }

    return hexSha256;
  }

  public PackageInfo build() {
    PackageInfo packageInfo = new PackageInfo();

    List<FileInfo> fileInfos = new ArrayList<>(files.size());

    for (Map.Entry<String, FileInfo> entry : files.entrySet()) {
      fileInfos.add(entry.getValue());
    }

    packageInfo.setFiles(fileInfos);
    packageInfo.setFolders(folders);
    if (!zipFiles.isEmpty()) {
      packageInfo.setZipFiles(zipFiles);
    }
    return packageInfo;
  }

}
