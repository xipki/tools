package org.xipki.apppackage;

public class PathInfo {

  private Integer posixPermissions;

  private String path;

  public Integer getPosixPermissions() {
    return posixPermissions;
  }

  public void setPosixPermissions(Integer posixPermissions) {
    this.posixPermissions = posixPermissions;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
