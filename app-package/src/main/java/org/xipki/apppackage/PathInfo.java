package org.xipki.apppackage;

public class PathInfo {

  private Integer posixPermissions;

  private String path;

  // epoch millis
  private Long lastModified;

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

  public Long getLastModified() {
    return lastModified;
  }

  public void setLastModified(Long lastModified) {
    this.lastModified = lastModified;
  }
}
