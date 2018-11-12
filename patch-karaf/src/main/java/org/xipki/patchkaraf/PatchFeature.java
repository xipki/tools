/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.patchkaraf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * TODO.
 * @author Lijun Liao
 * @since 3.1.0
 */

public class PatchFeature {

  public PatchFeature() {
  }

  public static void main(String[] args) {
    try {
      System.exit(func(args));
    } catch (IOException ex) {
      System.err.println(ex.getMessage());
      System.exit(-1);
    }
  }

  private static int func(String[] args) throws IOException {
    if (args == null || args.length == 0 || args[0].equals("--help") || args.length % 2 != 0) {
      return printUsage("");
    }

    String fileName = null;
    String repos = null;
    String featuresStr = null;
    boolean backup = true;

    for (int i = 0; i < args.length; i += 2) {
      String option = args[i];
      String value = args[i + 1];
      if ("--file".equalsIgnoreCase(option)) {
        fileName = value;
      } else if ("--repos".equalsIgnoreCase(option)) {
        repos = value;
      } else if ("--features".equalsIgnoreCase(option)) {
        featuresStr = value.trim();
      } else if ("--backup".equalsIgnoreCase(option)) {
        backup = Boolean.parseBoolean(value);
      }
    }

    if (PatchUtil.isBlank(fileName)) {
      return printUsage("file is not specified");
    }

    if (PatchUtil.isBlank(repos) && PatchUtil.isBlank(featuresStr)) {
      return printUsage("nothing to patch");
    }

    List<String> featuresToRemove = new LinkedList<>();
    List<String> featuresToAddPhase0 = new LinkedList<>();
    List<String> featuresToAddPhase1 = new LinkedList<>();
    boolean addPhase = featuresStr.startsWith("(");

    if (addPhase) {
      int phase0EndIndex = featuresStr.indexOf(')');
      String phase0FeaturesStr = featuresStr.substring(1, phase0EndIndex);
      featuresStr = featuresStr.substring(phase0EndIndex + 1);
      StringTokenizer tokenizer = new StringTokenizer(phase0FeaturesStr, ", ");
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        if (token.startsWith("-")) {
          featuresToRemove.add(token.substring(1));
        } else {
          featuresToAddPhase0.add(token);
        }
      }
    }

    StringTokenizer tokenizer = new StringTokenizer(featuresStr, ", ");
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      if (token.startsWith("-")) {
        featuresToRemove.add(token.substring(1));
      } else {
        featuresToAddPhase1.add(token);
      }
    }

    if (featuresToAddPhase1.isEmpty()) {
      addPhase = false;
    }

    boolean patchBootFeatures = addPhase
        || !(featuresToRemove.isEmpty()
            && featuresToAddPhase0.isEmpty()
            && featuresToAddPhase1.isEmpty());

    if (PatchUtil.isBlank(repos) && !patchBootFeatures) {
      return printUsage("nothing to patch");
    }

    File file = new File(fileName);
    File tmpNewFile = new File(fileName + ".new");
    BufferedReader reader = new BufferedReader(new FileReader(file));
    BufferedWriter writer = new BufferedWriter(new FileWriter(tmpNewFile));
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("featuresRepositories =") && !PatchUtil.isBlank(repos)) {
          String line2 = PatchUtil.readContinuedLine(reader, line);
          StringBuilder sb = new StringBuilder();
          sb.append("featuresRepositories = \\\n");
          StringTokenizer reposTokenizer = new StringTokenizer(repos, ", \n\r");
          while (reposTokenizer.hasMoreTokens()) {
            sb.append("    ").append(reposTokenizer.nextToken()).append(", \\\n");
          }

          String value2 = line2.substring("featuresRepositories =".length()).trim();
          reposTokenizer = new StringTokenizer(value2, ", \n\r");
          while (reposTokenizer.hasMoreTokens()) {
            sb.append("    ").append(reposTokenizer.nextToken()).append(", \\\n");
          }
          int len = sb.length();
          sb.delete(len - 4, len);
          writer.write(sb.toString());
        } else if (line.startsWith("featuresBoot =") && patchBootFeatures) {
          String line2 = PatchUtil.readContinuedLine(reader, line);

          StringBuilder sb = new StringBuilder();
          String value2 = line2.substring("featuresBoot =".length()).trim();
          StringTokenizer featuresTokenizer = new StringTokenizer(value2, ", \n\r");
          
          List<String> origFeaturesInPhase0 = new LinkedList<>();
          while (featuresTokenizer.hasMoreTokens()) {
            String feature = featuresTokenizer.nextToken().trim();
            boolean add = true;
            for (String featureToRemove : featuresToRemove) {
              if (feature.equals(featureToRemove) || feature.startsWith(featureToRemove + "/")) {
                add = false;
                break;
              }
            }
            
            if (add) {
              origFeaturesInPhase0.add(feature);
            }
          }

          List<String> featuresInPhase0 = new LinkedList<>();
          featuresInPhase0.addAll(origFeaturesInPhase0);
          featuresInPhase0.addAll(featuresToAddPhase0);
          
          if (addPhase) {
            sb.append("featuresBoot = ( \\\n");
          } else {
            sb.append("featuresBoot = \\\n");
          }

          int n = featuresInPhase0.size();
          for (int i = 0; i < n; i++) {
            String feature = featuresInPhase0.get(i);
            if (i == n - 1) {
              sb.append("    ").append(feature);
              if (addPhase) {
                sb.append(")");
              }
              
              if (featuresToAddPhase1.isEmpty()) {
                sb.append("\n");
              } else {
                sb.append(", \\\n");
              }
            } else {
              sb.append("    ").append(feature).append(", \\\n");
            }
          }

          n = featuresToAddPhase1.size();
          for (int i = 0; i < n; i++) {
            String feature = featuresToAddPhase1.get(i);
            if (i == n - 1) {
              sb.append("    ").append(feature);
            } else {
              sb.append("    ").append(feature).append(", \\\n");
            }
          }
          writer.write(sb.toString());
        } else {
          writer.write(line);
        }

        writer.write('\n');
      }
    } finally {
      reader.close();
      writer.close();
    }

    if (backup) {
      File origFile = new File(fileName + ".orig");
      if (!PatchUtil.rename(file, origFile)) {
        return printUsage("could not rename " + file.getPath() + " to " + origFile.getPath());
      }
    }

    if (!PatchUtil.rename(tmpNewFile, file)) {
      return printUsage("could not rename " + tmpNewFile.getPath() + " to " + file.getPath());
    }

    System.out.println("Patched file " + fileName);
    return 0;
  }

  private static int printUsage(String message) {
    StringBuilder sb = new StringBuilder();
    if (!PatchUtil.isBlank(message)) {
      sb.append(message).append("\n");
    }

    sb.append("\nSYNTAX");
    sb.append("\n\tjava " + PatchFeature.class.getName() + " [options]");
    sb.append("\nOPTIONS");
    sb.append("\n\t--file");
    sb.append("\n\t\tFile to be patched");
    sb.append("\n\t--backup");
    sb.append("\n\t\tWhether to create a backup of the patched file (with appendix .orig)");
    sb.append("\n\t\t(defaults to true)");
    sb.append("\n\t--repos");
    sb.append("\n\t\tComma-separated repositories");
    sb.append("\n\t--features");
    sb.append("\n\t\tFeatures in form of [(f1,...,fk),]fk+1,fn where fx is the feature name");
    sb.append("\n\t--help");
    sb.append("\n\t\tDisplay this help message");

    System.out.println(sb.toString());
    return -1;
  }

}
