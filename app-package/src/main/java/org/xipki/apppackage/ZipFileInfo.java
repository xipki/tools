package org.xipki.apppackage;

import org.xipki.apppackage.cbor.CborDecoder;
import org.xipki.apppackage.cbor.CborEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ZipFileInfo {

  private String path;

  private List<ZipEntryInfo> entries;

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

  public void encode(CborEncoder encoder) throws IOException {
    encoder.writeArrayStart(2);
    encoder.writeTextString(path);
    if (entries == null) {
      encoder.writeNull();
    } else {
      encoder.writeArrayStart(entries.size());
      for (ZipEntryInfo entry : entries) {
        entry.encode(encoder);
      }
    }
  }

  public static ZipFileInfo decode(CborDecoder decoder) throws IOException {
    MyUtil.readArrayStart(2, decoder);

    String path = MyUtil.readText(decoder);
    List<ZipEntryInfo> entries;
    if (MyUtil.isNull(decoder)) {
      decoder.readNull();
      entries = null;
    } else {
      int size = (int) decoder.readArrayLength();
      entries = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        entries.add(ZipEntryInfo.decode(decoder));
      }
    }

    ZipFileInfo ret = new ZipFileInfo();
    ret.setPath(path);
    ret.setEntries(entries);
    return ret;
  }

}
