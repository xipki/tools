package org.xipki.apppackage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.List;

public class RestorePackage {

  public RestorePackage() {
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
    String expectedPackageInfoSha256 = new String(
        Files.readAllBytes(Paths.get(srcDir.toString(), "meta-info.json.sha256"))).trim();
    String packageInfoSha256 = MyUtil.hexSha256(packageInfoBytes);
    if (!expectedPackageInfoSha256.equals(packageInfoSha256)) {
      throw new GeneralSecurityException("meta-info.json and meta-info.json.sha256 do not match");
    }

    PackageInfo packageInfo = JSON.parseObject(packageInfoBytes, PackageInfo.class);
    targetDir.mkdirs();
    // restore the folders.
    List<String> folders = packageInfo.getFolders();
    if (folders != null) {
      for (String folder : packageInfo.getFolders()) {
        new File(targetDir, folder).mkdirs();
      }
    }

    String srcDirPath = srcDir.getCanonicalPath();
    String targetDirPath = targetDir.getCanonicalPath();

    for (FileInfo fileInfo : packageInfo.getFiles()) {
      Path valueFilePath = Paths.get(srcDirPath, fileInfo.getSha256());
      byte[] fileValue = Files.readAllBytes(valueFilePath);
      String fileValueSha256 = MyUtil.hexSha256(fileValue);
      if (!fileInfo.getSha256().equals(fileValueSha256)) {
        throw new GeneralSecurityException("File " + valueFilePath.toFile() + " has been manipulated.");
      }

      for (PathInfo pathInfo : fileInfo.getPathInfos()) {
        Path targetPath = Paths.get(targetDirPath ,pathInfo.getPath());
        Files.copy(new ByteArrayInputStream(fileValue), targetPath, StandardCopyOption.REPLACE_EXISTING);

        if (MyUtil.isIsPosix() && pathInfo.getUnixPermissions() != null) {
          Files.setPosixFilePermissions(targetPath, MyUtil.toPosixFilePermissions(pathInfo.getUnixPermissions()));
        }
      }
    }
  }

}