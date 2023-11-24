package org.xipki.apppackage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PackageInfoBuilder {

  private final PackageConf conf;

  private final List<String> folders = new LinkedList<>();

  private final List<ZipFileInfo> zipFiles = new LinkedList<>();

  private final Map<String, FileInfo> files = new HashMap<>();

  public PackageInfoBuilder(PackageConf conf) {
    this.conf = Objects.requireNonNull(conf);
  }

  public void addFolder(Path baseSrcDir, Path folder) {
    folders.add(MyUtil.toUnixPath(baseSrcDir, folder));
  }

  public void addZipFile(ZipFileInfo zipFileInfo) {
    zipFiles.add(zipFileInfo);
  }

  public void addFile(byte[] bytes, long lastModified, Path relativePath,
                      Integer intPermission, File targetDir) throws IOException {
    String hexSha256 = MyUtil.hexSha256(bytes);
    FileInfo fileInfo = files.get(hexSha256);

    if (fileInfo == null) {
      fileInfo = new FileInfo();
      fileInfo.setPathInfos(new LinkedList<>());
      String fileName = hexSha256 + conf.getSuffix(relativePath);
      fileInfo.setFileName(fileName);
      fileInfo.setSize(bytes.length);
      files.put(hexSha256, fileInfo);

      File newEntryFile = new File(targetDir, fileName);
      Files.copy(new ByteArrayInputStream(bytes), newEntryFile.toPath());
    }

    PathInfo pathInfo = new PathInfo();
    fileInfo.getPathInfos().add(pathInfo);
    pathInfo.setPath(relativePath.toString());
    pathInfo.setLastModified(lastModified / 1000);

    if (intPermission != null) {
      pathInfo.setPosixPermissions(intPermission);
    }
  }

  public String addZipEntry(byte[] bytes, String name, File targetDir) throws IOException {
    String hexSha256 = MyUtil.hexSha256(bytes);
    FileInfo fileInfo = files.get(hexSha256);
    String fileName;

    if (fileInfo == null) {
      fileInfo = new FileInfo();
      fileInfo.setPathInfos(new LinkedList<>());
      fileInfo.setSize(bytes.length);
      fileName = hexSha256 + conf.getSuffix(Paths.get(name));
      fileInfo.setFileName(fileName);
      files.put(hexSha256, fileInfo);

      File newEntryFile = new File(targetDir, fileName);
      Files.copy(new ByteArrayInputStream(bytes), newEntryFile.toPath());
    } else {
      fileName = fileInfo.getFileName();
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

    return fileName;
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
