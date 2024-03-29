package org.xipki.apppackage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    byte[] packageInfoBytes = Files.readAllBytes(Paths.get(srcDir.toString(), "meta-info.cbor"));
    String expectedPackageInfoSha256 = new String(
        Files.readAllBytes(Paths.get(srcDir.toString(), "meta-info.cbor.sha256"))).trim();
    String packageInfoSha256 = MyUtil.hexSha256(packageInfoBytes);
    if (!expectedPackageInfoSha256.equals(packageInfoSha256)) {
      throw new GeneralSecurityException("meta-info.cbor and meta-info.cbor.sha256 do not match");
    }

    PackageInfo packageInfo = PackageInfo.decode(packageInfoBytes);
    targetDir.mkdirs();
    // restore the folders.
    List<String> folders = packageInfo.getFolders();
    if (folders != null) {
      for (String folder : packageInfo.getFolders()) {
        new File(targetDir, folder).mkdirs();
      }
    }

    // restore the zip files.
    List<ZipFileInfo> zipFileInfos = packageInfo.getZipFiles();
    if (zipFileInfos != null) {
      for (ZipFileInfo zipFileInfo : zipFileInfos) {
        try (ZipOutputStream zipOs = new ZipOutputStream(
            Files.newOutputStream(new File(targetDir, zipFileInfo.getPath()).toPath()))) {
          zipOs.setMethod(ZipOutputStream.DEFLATED);
          zipOs.setLevel(Deflater.DEFAULT_COMPRESSION);

          for (ZipEntryInfo entryInfo : zipFileInfo.getEntries()) {
            ZipEntry zipEntry = new ZipEntry(entryInfo.getName());
            zipEntry.setTime(entryInfo.getLastModified() * 1000);
            if (entryInfo.getComment() != null) {
              zipEntry.setComment(entryInfo.getComment());
            }
            if (entryInfo.getExtra() != null) {
              zipEntry.setExtra(entryInfo.getExtra());
            }
            zipOs.putNextEntry(zipEntry);

            Path p0 = new File(srcDir, entryInfo.getFileName()).toPath();
            try {
              zipOs.write(Files.readAllBytes(p0));
            } catch (Exception e) {
              System.out.println(p0);
              e.printStackTrace();
              throw e;
            }
          }
        }
      }
    }

    String srcDirPath = srcDir.getCanonicalPath();
    String targetDirPath = targetDir.getCanonicalPath();

    for (FileInfo fileInfo : packageInfo.getFiles()) {
      Path valueFilePath = Paths.get(srcDirPath, fileInfo.getFileName());
      byte[] fileValue = Files.readAllBytes(valueFilePath);
      String fileValueSha256 = MyUtil.hexSha256(fileValue);

      if (!fileInfo.getFileName().startsWith(fileValueSha256)) {
        throw new GeneralSecurityException("File " + valueFilePath.toFile() + " has been manipulated.");
      }

      for (PathInfo pathInfo : fileInfo.getPathInfos()) {
        String path = pathInfo.getPath();
        if (path.startsWith("zip:")) {
          continue;
        }

        Path targetPath = Paths.get(targetDirPath, path);
        Files.copy(new ByteArrayInputStream(fileValue), targetPath, StandardCopyOption.REPLACE_EXISTING);

        if (MyUtil.isIsPosix() && pathInfo.getPosixPermissions() != null) {
          Files.setPosixFilePermissions(targetPath, MyUtil.toPosixFilePermissions(pathInfo.getPosixPermissions()));
        }
        if (pathInfo.getLastModified() != null) {
          FileTime lastModified = FileTime.from(Instant.ofEpochSecond(pathInfo.getLastModified()));
          Files.setLastModifiedTime(targetPath, lastModified);
        }
      }
    }
  }

}
