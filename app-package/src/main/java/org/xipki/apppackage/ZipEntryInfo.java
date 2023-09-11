package org.xipki.apppackage;

import org.xipki.apppackage.jacob.CborDecoder;
import org.xipki.apppackage.jacob.CborEncoder;

import java.io.IOException;

public class ZipEntryInfo {

  private String name;

  private int size;
  private String comment;

  private long lastModified;

  private String sha256;

  private byte[] extra;

  public long getLastModified() {
    return lastModified;
  }

  public void setLastModified(long lastModified) {
    this.lastModified = lastModified;
  }

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

  public void encode(CborEncoder encoder) throws IOException {
    /*
      private String name;

  private int size;
  private String comment;

  private long lastModified;

  private String sha256;

  private byte[] extra;

     */
    encoder.writeArrayStart(6);
    encoder.writeTextString(name);
    encoder.writeInt(size);
    if (comment == null) {
      encoder.writeNull();
    } else {
      encoder.writeTextString(comment);
    }

    encoder.writeInt(lastModified);
    encoder.writeTextString(sha256);
    if (extra == null) {
      encoder.writeNull();
    } else {
      encoder.writeByteString(extra);
    }
  }

  public static ZipEntryInfo decode(CborDecoder decoder) throws IOException {
    MyUtil.readArrayStart(6, decoder);
    ZipEntryInfo ret = new ZipEntryInfo();
    ret.setName(decoder.readTextString());
    ret.setSize((int) decoder.readInt());
    ret.setComment(MyUtil.readText(decoder));
    ret.setLastModified(decoder.readInt());
    ret.setSha256(decoder.readTextString());
    ret.setExtra(MyUtil.readByteString(decoder));
    return ret;
  }

}
