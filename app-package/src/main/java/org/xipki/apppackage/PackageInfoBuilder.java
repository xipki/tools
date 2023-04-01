package org.xipki.apppackage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PackageInfoBuilder {

  private final List<String> folders = new LinkedList<>();

  private final Map<String, FileInfo> files = new HashMap<>();

  public void addFolder(Path baseSrcDir, Path folder) {
    folders.add(MyUtil.toUnixPath(baseSrcDir, folder));
  }

  public void addFile(byte[] bytes, Path relativePath, Integer intPermission, File targetDir) throws IOException {
    String hexSha256 = MyUtil.hexSha256(bytes);
    FileInfo fileInfo = files.get(hexSha256);

    if (fileInfo == null) {
      fileInfo = new FileInfo();
      fileInfo.setPathInfos(new LinkedList<>());
      fileInfo.setSha256(hexSha256);
      files.put(hexSha256, fileInfo);

      File newEntryFile = new File(targetDir, hexSha256);
      Files.copy(new ByteArrayInputStream(bytes), newEntryFile.toPath());
    }

    PathInfo pathInfo = new PathInfo();
    fileInfo.getPathInfos().add(pathInfo);
    pathInfo.setPath(relativePath.toString());

    if (intPermission != null) {
      pathInfo.setUnixPermissions(intPermission);
    }
  }

  public String addZipEntry(byte[] bytes, File targetDir) throws IOException {
    String hexSha256 = MyUtil.hexSha256(bytes);
    FileInfo fileInfo = files.get(hexSha256);

    if (fileInfo == null) {
      fileInfo = new FileInfo();
      fileInfo.setPathInfos(new LinkedList<>());
      fileInfo.setSha256(hexSha256);
      files.put(hexSha256, fileInfo);

      File newEntryFile = new File(targetDir, hexSha256);
      Files.copy(new ByteArrayInputStream(bytes), newEntryFile.toPath());
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
    return packageInfo;
  }

}
