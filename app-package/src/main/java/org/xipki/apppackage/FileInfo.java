package org.xipki.apppackage;

import org.xipki.apppackage.cbor.CborDecoder;
import org.xipki.apppackage.cbor.CborEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileInfo {

  private String fileName;

  private int size;

  private List<PathInfo> pathInfos;

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
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
    encoder.writeTextString(fileName);
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
    ret.setFileName(decoder.readTextString());
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
