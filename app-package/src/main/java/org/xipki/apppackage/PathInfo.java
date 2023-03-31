package org.xipki.apppackage;

public class PathInfo {

  private Integer unixPermissions;

  private String path;

  public Integer getUnixPermissions() {
    return unixPermissions;
  }

  public void setUnixPermissions(Integer unixPermissions) {
    this.unixPermissions = unixPermissions;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
