package org.xipki.apppackage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CompressPackage {

  private final PackageConf conf;

  public CompressPackage(File confFile) {
    try {
      conf = new PackageConf(confFile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    try {
      if (args == null || args.length != 3) {
        System.out.println("Usage: java " + CompressPackage.class.getName() +
            " <conf-file> <source dir> <dest dir>");
        System.exit(1);
      }

      File confFile = new File(args[0]);
      File srcDir = new File(args[1]);
      File destDir = new File(args[2]);
      new CompressPackage(confFile).compressDir(srcDir, destDir);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void compressDir(File srcDir, File targetDir) throws Exception {
    if (targetDir.toPath().startsWith(srcDir.toPath())) {
      throw new IllegalArgumentException("targetDir must not be under srcDir");
    }

    if (targetDir.exists()) {
      throw new IllegalArgumentException("targetDir already exists");
    }

    PackageInfoBuilder builder = new PackageInfoBuilder(conf);
    targetDir.mkdirs();
    compressDir(builder, srcDir.toPath(), srcDir, targetDir);
    PackageInfo packageInfo = builder.build();
    byte[] packageInfoBytes = packageInfo.encode();
    String packageInfoSha256 = MyUtil.hexSha256(packageInfoBytes);
    Files.copy(new ByteArrayInputStream(packageInfoBytes), new File(targetDir, "meta-info.cbor").toPath());
    Files.copy(new ByteArrayInputStream(packageInfoSha256.getBytes(StandardCharsets.UTF_8)),
        new File(targetDir, "meta-info.cbor.sha256").toPath());
  }

  private void compressDir(PackageInfoBuilder packageInfoBuilder, Path baseSrcDir,
                           File srcDir, File targetDir) throws Exception {
    File[] subDirsOrFiles = srcDir.listFiles();
    for (File subDirOrFile : subDirsOrFiles) {
      if (subDirOrFile.isDirectory()) {
        File[] subFolders = subDirOrFile.listFiles(pathname -> pathname.isDirectory());

        if (subFolders == null || subFolders.length == 0) {
          packageInfoBuilder.addFolder(baseSrcDir, subDirOrFile.toPath());
        }

        compressDir(packageInfoBuilder, baseSrcDir, subDirOrFile, targetDir);
      } else {
        boolean unpackZipFile = conf.unzipMe(baseSrcDir, subDirOrFile.toPath());
        if (unpackZipFile) {
          compressZipFile(packageInfoBuilder, baseSrcDir, subDirOrFile, targetDir);
        } else {
          compressFile(packageInfoBuilder, baseSrcDir, subDirOrFile, targetDir);
        }
      }
    }
  }

  private void compressFile(PackageInfoBuilder packageInfoBuilder, Path baseSrcDir,
                            File file, File targetDir) throws Exception {
    byte[] fileBytes = Files.readAllBytes(file.toPath());

    Path filePath = file.toPath();
    Integer posixPermission = conf.posixPermission(baseSrcDir, filePath);

    Path relativePath = filePath.subpath(baseSrcDir.getNameCount(), filePath.getNameCount());
    packageInfoBuilder.addFile(fileBytes, file.lastModified(), relativePath, posixPermission, targetDir);
  }

  private void compressZipFile(PackageInfoBuilder packageInfoBuilder, Path baseSrcDir,
                            File file, File targetDir) throws Exception {
    ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ);

    ZipFileInfo zipFileInfo = new ZipFileInfo();
    zipFileInfo.setPath(MyUtil.toUnixPath(baseSrcDir, file.toPath()));
    // zipFileInfo.setEpochSecond(Instant.now().getEpochSecond());

    List<ZipEntryInfo> zipEntryInfos = new LinkedList<>();
    zipFileInfo.setEntries(zipEntryInfos);

    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      if (entry.isDirectory()) {
        continue;
      }

      byte[] entryBytes;
      try (InputStream entryStream = zipFile.getInputStream(entry);
           ByteArrayOutputStream bout = new ByteArrayOutputStream(entryStream.available())) {
        byte[] buffer = new byte[4096];
        int readed;
        while ((readed = entryStream.read(buffer)) != -1) {
          bout.write(buffer, 0, readed);
        }
        entryBytes = bout.toByteArray();
      }

      String fileName = packageInfoBuilder.addZipEntry(entryBytes, entry.getName(), targetDir);
      ZipEntryInfo zipEntryInfo = new ZipEntryInfo();
      zipEntryInfo.setComment(entry.getComment());
      zipEntryInfo.setName(entry.getName());
      zipEntryInfo.setSize(entryBytes.length);
      zipEntryInfo.setLastModified(entry.getTime() / 1000);
      zipEntryInfo.setExtra(entry.getExtra());
      zipEntryInfo.setFileName(fileName);
      zipEntryInfos.add(zipEntryInfo);
    }

    packageInfoBuilder.addZipFile(zipFileInfo);
  }

}
