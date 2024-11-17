package org.xipki.apppackage;

import org.xipki.apppackage.cbor.CborConstants;
import org.xipki.apppackage.cbor.CborDecoder;
import org.xipki.apppackage.cbor.CborType;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyUtil {

  private static final String pathSep = File.separator;

  private static final boolean isPosix;
  private static final MessageDigest sha256;

  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  static {
    try {
      sha256 = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    isPosix =  FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  }

  public static boolean isIsPosix() {
    return isPosix;
  }

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static String hexSha256(byte[] bytes) {
    synchronized (sha256) {
      return bytesToHex(sha256.digest(bytes));
    }
  }

  public static String toUnixPath(String path) {
    if (!pathSep.equals("/")) {
      path = path.replace(pathSep, "/");
    }
    return path;
  }

  public static String toUnixPath(Path basePath, Path path) {
    if (path.startsWith(basePath)) {
      Path relativePath = path.subpath(basePath.getNameCount(), path.getNameCount());
      return toUnixPath(relativePath.toString());
    } else {
      return toUnixPath(path.toString());
    }
  }

  public static Set<PosixFilePermission> toPosixFilePermissions(int intPermission) {
    int otherPermission = intPermission % 10;
    int groupPermission = intPermission / 10 % 10;
    int ownerPermission = intPermission / 100 % 10;

    Set<PosixFilePermission> filePermissions = new HashSet<>();
    if ((otherPermission & 0x01) != 0) {
      filePermissions.add(PosixFilePermission.OTHERS_EXECUTE);
    }
    if ((otherPermission & 0x02) != 0) {
      filePermissions.add(PosixFilePermission.OTHERS_WRITE);
    }
    if ((otherPermission & 0x04) != 0) {
      filePermissions.add(PosixFilePermission.OTHERS_READ);
    }

    if ((groupPermission & 0x01) != 0) {
      filePermissions.add(PosixFilePermission.GROUP_EXECUTE);
    }
    if ((groupPermission & 0x02) != 0) {
      filePermissions.add(PosixFilePermission.GROUP_WRITE);
    }
    if ((groupPermission & 0x04) != 0) {
      filePermissions.add(PosixFilePermission.GROUP_READ);
    }

    if ((ownerPermission & 0x01) != 0) {
      filePermissions.add(PosixFilePermission.OWNER_EXECUTE);
    }
    if ((ownerPermission & 0x02) != 0) {
      filePermissions.add(PosixFilePermission.OWNER_WRITE);
    }
    if ((ownerPermission & 0x04) != 0) {
      filePermissions.add(PosixFilePermission.OWNER_READ);
    }

    return filePermissions;
  }

  public static boolean isNull(CborDecoder decoder) throws IOException {
    CborType type = decoder.peekType();
    return type.getMajorType() == CborConstants.TYPE_FLOAT_SIMPLE && type.getAdditionalInfo() == CborConstants.NULL;
  }

  public static String readText(CborDecoder decoder) throws IOException {
    if (MyUtil.isNull(decoder)) {
      decoder.readNull();
      return null;
    } else {
      return decoder.readTextString();
    }
  }

  public static Long readLong(CborDecoder decoder) throws IOException {
    if (MyUtil.isNull(decoder)) {
      decoder.readNull();
      return null;
    } else {
      return decoder.readInt();
    }
  }

  public static byte[] readByteString(CborDecoder decoder) throws IOException {
    if (MyUtil.isNull(decoder)) {
      decoder.readNull();
      return null;
    } else {
      return decoder.readByteString();
    }
  }

  public static List<String> readTextList(CborDecoder decoder) throws IOException {
    if (MyUtil.isNull(decoder)) {
      decoder.readNull();
      return null;
    } else {
      int size = (int) decoder.readArrayLength();
      List<String> list = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        list.add(decoder.readTextString());
      }
      return list;
    }
  }

  public static int readArrayStart(int expectedArrayLen, CborDecoder decoder) throws IOException {
    int arrayLen = (int) decoder.readArrayLength();
    if (arrayLen != expectedArrayLen) {
      throw new IOException("array.length is " + arrayLen +
          " but expected " + expectedArrayLen);
    }
    return arrayLen;
  }

}
