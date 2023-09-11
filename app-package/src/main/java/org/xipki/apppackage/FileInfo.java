package org.xipki.apppackage;

import org.xipki.apppackage.jacob.CborDecoder;
import org.xipki.apppackage.jacob.CborEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileInfo {

  private String sha256;

  private int size;

  private List<PathInfo> pathInfos;

  public String getSha256() {
    return sha256;
  }

  public void setSha256(String sha256) {
    this.sha256 = sha256;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public List<PathInfo> getPathInfos() {
    return pathInfos;
  }

  public void setPathInfos(List<PathInfo> pathInfos) {
    this.pathInfos = pathInfos;
  }

  public void encode(CborEncoder encoder) throws IOException {
    encoder.writeArrayStart(3);
    encoder.writeTextString(sha256);
    encoder.writeInt(size);
    if (pathInfos == null) {
      encoder.writeNull();
    } else {
      encoder.writeArrayStart(pathInfos.size());
      for (PathInfo pathInfo : pathInfos) {
        pathInfo.encode(encoder);
      }
    }
  }

  public static FileInfo decode(CborDecoder decoder) throws IOException {
    MyUtil.readArrayStart(3, decoder);

    FileInfo ret = new FileInfo();
    ret.setSha256(decoder.readTextString());
    ret.setSize((int) decoder.readInt());

    List<PathInfo> pathInfos;
    if (MyUtil.isNull(decoder)) {
      decoder.readNull();
      pathInfos = null;
    } else {
      int size = (int) decoder.readArrayLength();
      pathInfos = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        pathInfos.add(PathInfo.decode(decoder));
      }
    }
    ret.setPathInfos(pathInfos);

    return ret;
  }

}
