package org.xipki.apppackage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Path;

public class LineEncodingConverter
{

  private final PackageConf conf;

  public LineEncodingConverter(File confFile) {
    try {
      conf = new PackageConf(confFile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    try {
      if (args == null || args.length != 2) {
        System.out.println("Usage: java " + LineEncodingConverter.class.getName() +
            " <conf-file> <dir>");
        System.exit(1);
      }

      File confFile = new File(args[0]);
      File dir = new File(args[1]);
      new LineEncodingConverter(confFile).convertLineEncodingInDir(dir, dir.toPath());
    } catch (Exception e) {
      //e.printStackTrace();
    }
  }

  private void convertLineEncodingInDir(File dir, Path baseDir) throws Exception
  {
    if (!(dir.exists() && dir.isDirectory())) {
      throw new IllegalArgumentException("dir does not exist");
    }

    File[] subDirsOrFiles = dir.listFiles();
    for (File subDirOrFile : subDirsOrFiles) {
      if (subDirOrFile.isDirectory()) {
        convertLineEncodingInDir(subDirOrFile, baseDir);
      } else if (subDirOrFile.isFile()) {
        boolean isDosLineEncoding  = conf.isDosLineEncoding(baseDir, subDirOrFile.toPath());
        boolean isUnixLineEncoding = conf.isUnixLineEncoding(baseDir, subDirOrFile.toPath());
        if (isDosLineEncoding || isUnixLineEncoding) {
          String lineEncoding = isDosLineEncoding ? "\r\n" : "\n";
          StringBuilder sb = new StringBuilder();
          try (BufferedReader r = new BufferedReader(new FileReader(subDirOrFile))) {
            String line;
            while ((line = r.readLine()) != null) {
              sb.append(line);
              sb.append(lineEncoding);
            }
          }

          try (Writer w = new FileWriter(subDirOrFile)) {
            w.write(sb.toString());
          }
        }
      }
    }
  }

}
