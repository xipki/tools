package org.xipki.apppackage;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CompressPackage {

  private final boolean isPosix;
  private final MessageDigest sha256;

  private final Map<String, Integer> posixPermissions = new HashMap<>();

  public CompressPackage(File confFile) {
    try {
      sha256 = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    isPosix =  FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    Properties properties = new Properties();
    if (confFile != null) {
      try (InputStream is = Files.newInputStream(confFile.toPath())) {
        properties.load(is);
        for (String key : properties.stringPropertyNames()) {
          String value = properties.getProperty(key);
          int intPermission = Integer.parseInt(value);
          posixPermissions.put(Paths.get(key).toFile().getCanonicalPath(), intPermission);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
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

    PackageInfoBuilder builder = new PackageInfoBuilder();
    targetDir.mkdirs();
    compressDir(builder, srcDir.toPath(), srcDir, targetDir);
    PackageInfo packageInfo = builder.build();
    byte[] packageInfoBytes = JSON.toJSONBytes(packageInfo);
    byte[] packageInfoSha256 = sha256.digest(packageInfoBytes);
    Files.copy(new ByteArrayInputStream(packageInfoBytes), new File(targetDir, "meta-info.json").toPath());
    Files.copy(new ByteArrayInputStream(packageInfoSha256), new File(targetDir, "meta-info.json.sha256").toPath());
  }

  private void compressDir(PackageInfoBuilder packageInfoBuilder, Path baseSrcDir,
                           File srcDir, File targetDir) throws Exception {
    File[] subDirsOrFiles = srcDir.listFiles();
    for (File subDirOrFile : subDirsOrFiles) {
      if (subDirOrFile.isDirectory()) {
        packageInfoBuilder.addFolder(baseSrcDir, subDirOrFile.toPath());
        compressDir(packageInfoBuilder, baseSrcDir, subDirOrFile, targetDir);
      } else {
        compressFile(packageInfoBuilder, baseSrcDir, subDirOrFile, targetDir);
      }
    }
  }

  private void compressFile(PackageInfoBuilder packageInfoBuilder, Path baseSrcDir,
                            File file, File targetDir) throws Exception {
    byte[] fileBytes = Files.readAllBytes(file.toPath());
    byte[] sha256Value = sha256.digest(fileBytes);

    Path filePath = file.toPath();
    Path relativePath = filePath.subpath(baseSrcDir.getNameCount(), filePath.getNameCount());
    Integer posixPermission = posixPermissions.get(relativePath.toFile().getCanonicalPath());

    boolean newEntry = packageInfoBuilder.addFile(sha256Value, relativePath, posixPermission);
    if (newEntry) {
      File newEntryFile = new File(targetDir, PackageInfoBuilder.bytesToHex(sha256Value));
      Files.copy(file.toPath(), newEntryFile.toPath());
    }
  }

}
