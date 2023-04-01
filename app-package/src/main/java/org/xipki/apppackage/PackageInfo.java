package org.xipki.apppackage;

import java.util.List;

public class PackageInfo {

  private List<String> folders;

  private List<ZipFileInfo> zipFiles;

  private List<FileInfo> files;

  public List<String> getFolders() {
    return folders;
  }

  public void setFolders(List<String> folders) {
    this.folders = folders;
  }

  public List<FileInfo> getFiles() {
    return files;
  }

  public void setFiles(List<FileInfo> files) {
    this.files = files;
  }

  public List<ZipFileInfo> getZipFiles() {
    return zipFiles;
  }

  public void setZipFiles(List<ZipFileInfo> zipFiles) {
    this.zipFiles = zipFiles;
  }
}
