package org.xipki.apppackage;

import org.xipki.apppackage.cbor.CborDecoder;
import org.xipki.apppackage.cbor.CborEncoder;

import java.io.IOException;

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

  public void encode(CborEncoder encoder) throws IOException {
    encoder.writeArrayStart(3);
    encoder.writeTextString(path);

    if (posixPermissions == null) {
      encoder.writeNull();
    } else {
      encoder.writeInt(posixPermissions);
    }

    if (lastModified == null) {
      encoder.writeNull();
    } else {
      encoder.writeInt(lastModified);
    }
  }

  public static PathInfo decode(CborDecoder decoder) throws IOException {
    MyUtil.readArrayStart(3, decoder);
    PathInfo ret = new PathInfo();
    ret.setPath(decoder.readTextString());
    Long l = MyUtil.readLong(decoder);
    if (l != null) {
      ret.setPosixPermissions(l.intValue());
    }
    ret.setLastModified(MyUtil.readLong(decoder));

    return ret;
  }

}
