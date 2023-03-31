package org.xipki.apppackage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RestorePackage {

  private final MessageDigest sha256;

  private final boolean isPosix;

  public RestorePackage() {
    try {
      sha256 = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    isPosix =  FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  }

  public static void main(String[] args) {
    try {
      if (args == null || args.length != 2) {
        System.out.println("Usage: java " + RestorePackage.class.getName() +
            " <source dir> <dest dir>");
        System.exit(1);
      }

      File srcDir = new File(args[0]);
      File destDir = new File(args[1]);
      new RestorePackage().decompressDir(srcDir, destDir);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void decompressDir(File srcDir, File targetDir) throws Exception {
    if (targetDir.exists()) {
      throw new IllegalArgumentException("targetDir already exists");
    }

    if (targetDir.toPath().startsWith(srcDir.toPath())) {
      throw new IllegalArgumentException("targetDir must not be under srcDir");
    }

    byte[] packageInfoBytes = Files.readAllBytes(Paths.get(srcDir.toString(), "meta-info.json"));
    byte[] expectedPackageInfoSha256 = Files.readAllBytes(Paths.get(srcDir.toString(), "meta-info.json.sha256"));
    byte[] packageInfoSha256 = sha256.digest(packageInfoBytes);
    if (!Arrays.equals(expectedPackageInfoSha256, packageInfoSha256)) {
      throw new GeneralSecurityException("meta-info.json and meta-info.json.sha256 do not match");
    }

    PackageInfo packageInfo = JSON.parseObject(packageInfoBytes, PackageInfo.class);
    targetDir.mkdirs();
    for (String folder : packageInfo.getFolders()) {
      new File(targetDir, folder).mkdirs();
    }

    String srcDirPath = srcDir.getCanonicalPath();
    String targetDirPath = targetDir.getCanonicalPath();

    for (FileInfo fileInfo : packageInfo.getFiles()) {
      Path valueFilePath = Paths.get(srcDirPath, fileInfo.getSha256());
      byte[] fileValue = Files.readAllBytes(valueFilePath);
      byte[] fileValueSha256 = sha256.digest(fileValue);
      if (!fileInfo.getSha256().equals(PackageInfoBuilder.bytesToHex(fileValueSha256))) {
        throw new GeneralSecurityException("File " + valueFilePath.toFile() + " has been manipulated.");
      }

      for (PathInfo pathInfo : fileInfo.getPathInfos()) {
        Path targetPath = Paths.get(targetDirPath ,pathInfo.getPath());
        Files.copy(new ByteArrayInputStream(fileValue), targetPath, StandardCopyOption.REPLACE_EXISTING);

        if (isPosix && pathInfo.getUnixPermissions() != null) {
          Files.setPosixFilePermissions(targetPath, getPermissions(pathInfo.getUnixPermissions()));
        }
      }
    }
  }

  private static Set<PosixFilePermission> getPermissions(int intPermission) {
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

}
