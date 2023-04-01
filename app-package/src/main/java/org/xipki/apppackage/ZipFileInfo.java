package org.xipki.apppackage;

import java.util.List;

public class ZipFileInfo {

  private String path;

  private long epochSecond;

  private List<ZipEntryInfo> entries;

  public long getEpochSecond() {
    return epochSecond;
  }

  public void setEpochSecond(long epochSecond) {
    this.epochSecond = epochSecond;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<ZipEntryInfo> getEntries() {
    return entries;
  }

  public void setEntries(List<ZipEntryInfo> entries) {
    this.entries = entries;
  }
}
