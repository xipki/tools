package org.xipki.apppackage;

public class ZipEntryInfo {

  private String name;

  private int size;
  private String comment;

  private long epochMilli;

  public long getEpochMilli() {
    return epochMilli;
  }

  public void setEpochMilli(long epochMilli) {
    this.epochMilli = epochMilli;
  }

  private byte[] extra;

  private String sha256;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public byte[] getExtra() {
    return extra;
  }

  public void setExtra(byte[] extra) {
    this.extra = extra;
  }

  public String getSha256() {
    return sha256;
  }

  public void setSha256(String sha256) {
    this.sha256 = sha256;
  }

}
