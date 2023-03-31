package org.xipki.apppackage;

import java.util.List;

public class FileInfo {

  private String sha256;

  private List<PathInfo> pathInfos;

  public String getSha256() {
    return sha256;
  }

  public void setSha256(String sha256) {
    this.sha256 = sha256;
  }

  public List<PathInfo> getPathInfos() {
    return pathInfos;
  }

  public void setPathInfos(List<PathInfo> pathInfos) {
    this.pathInfos = pathInfos;
  }

}
