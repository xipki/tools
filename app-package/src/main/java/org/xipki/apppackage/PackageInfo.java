package org.xipki.apppackage;

import org.xipki.apppackage.cbor.CborDecoder;
import org.xipki.apppackage.cbor.CborEncoder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PackageInfo {

  private List<String> folders;

  private List<ZipFileInfo> zipFiles;

  private List<FileInfo> files;

  public List<String> getFolders() {
    return folders;
  }

  public void setFolders(List<String> folders) {
    this.folders = folders;
  }

  public List<FileInfo> getFiles() {
    return files;
  }

  public void setFiles(List<FileInfo> files) {
    this.files = files;
  }

  public List<ZipFileInfo> getZipFiles() {
    return zipFiles;
  }

  public void setZipFiles(List<ZipFileInfo> zipFiles) {
    this.zipFiles = zipFiles;
  }

  public byte[] encode() throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    CborEncoder encoder = new CborEncoder(bout);
    encoder.writeArrayStart(3);

    // folders
    if (folders == null) {
      encoder.writeNull();
    } else {
      encoder.writeArrayStart(folders.size());
      for (String folder : folders) {
        encoder.writeTextString(folder);
      }
    }

    // zipFiles
    if (zipFiles == null) {
      encoder.writeNull();
    } else {
      encoder.writeArrayStart(zipFiles.size());
      for (ZipFileInfo zipFile : zipFiles) {
        zipFile.encode(encoder);
      }
    }

    // files
    if (files == null) {
      encoder.writeNull();
    } else {
      encoder.writeArrayStart(files.size());
      for (FileInfo file : files) {
        file.encode(encoder);
      }
    }

    bout.flush();
    return bout.toByteArray();
  }

  public static PackageInfo decode(byte[] encoded) throws IOException {
    try (InputStream is = new ByteArrayInputStream(encoded)) {
      CborDecoder decoder = new CborDecoder(is);
      MyUtil.readArrayStart(3, decoder);

      List<String> folders = MyUtil.readTextList(decoder);
      List<ZipFileInfo> zipFiles;
      if (MyUtil.isNull(decoder)) {
        decoder.readNull();
        zipFiles = null;
      } else {
        int size = (int) decoder.readArrayLength();
        zipFiles = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
          zipFiles.add(ZipFileInfo.decode(decoder));
        }
      }

      List<FileInfo> files;
      if (MyUtil.isNull(decoder)) {
        decoder.readNull();
        files = null;
      } else {
        int size = (int) decoder.readArrayLength();
        files = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
          files.add(FileInfo.decode(decoder));
        }
      }

      PackageInfo pi = new PackageInfo();
      pi.setFolders(folders);
      pi.setZipFiles(zipFiles);
      pi.setFiles(files);
      return pi;
    }
  }

}
