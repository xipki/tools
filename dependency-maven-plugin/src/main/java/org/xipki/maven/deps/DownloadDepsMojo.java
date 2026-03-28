package org.xipki.maven.deps;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maven goal that resolves the configured root artifacts and copies them together with their
 * transitive dependencies into the target output directory.
 */
@Mojo(name = "download", defaultPhase = LifecyclePhase.NONE, threadSafe = true,
    requiresProject = true)
public class DownloadDepsMojo extends AbstractMojo {

  /**
   * Plugin configuration for one root artifact to resolve and copy.
   */
  public static class ArtifactItem {

    @Parameter(required = true)
    private String groupId;

    @Parameter(required = true)
    private String artifactId;

    @Parameter(required = true)
    private String version;

    @Parameter
    private String classifier;

    @Parameter(defaultValue = "jar")
    private String type;

    @Parameter
    private File outputDirectory;

    Artifact toArtifact() {
      String ext = emptyToNull(type);
      return new DefaultArtifact(groupId, artifactId, emptyToNull(classifier),
          (ext == null) ? "jar" : ext, version);
    }
  }

  @Component
  private RepositorySystem repositorySystem;

  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true,
      required = true)
  private RepositorySystemSession repositorySystemSession;

  @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true,
      required = true)
  private List<RemoteRepository> remoteRepositories;

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(required = true)
  private List<ArtifactItem> artifactItems;

  @Parameter(defaultValue = "runtime")
  private String includeScope;

  @Parameter(defaultValue = "${project.build.directory}/download", required = true)
  private File outputDirectory;

  @Parameter(defaultValue = "false")
  private boolean stripVersion;

  @Parameter(defaultValue = "true")
  private boolean overwrite;

  @Override
  public void execute() throws MojoExecutionException {
    if (artifactItems == null || artifactItems.isEmpty()) {
      throw new MojoExecutionException("artifactItems must not be empty");
    }

    String scope = normalizeScope(includeScope);
    int copied = 0;

    for (ArtifactItem item : artifactItems) {
      Artifact rootArtifact = item.toArtifact();
      Path targetDir = resolveOutputDirectory(item);
      ensureDirectoryExists(targetDir);

      Map<String, Artifact> artifactsToCopy = new LinkedHashMap<>();
      Artifact resolvedRoot = resolveArtifact(rootArtifact);
      artifactsToCopy.put(targetFileName(resolvedRoot), resolvedRoot);

      for (Artifact artifact : resolveDependencies(rootArtifact, scope)) {
        String targetFileName = targetFileName(artifact);
        Artifact previous = artifactsToCopy.put(targetFileName, artifact);
        if (previous != null && !sameArtifact(previous, artifact)) {
          getLog().warn("overwriting artifact mapped to " + targetFileName +
              " with " + artifact);
        }
      }

      for (Map.Entry<String, Artifact> entry : artifactsToCopy.entrySet()) {
        copyArtifact(entry.getValue(), targetDir.resolve(entry.getKey()));
        copied++;
      }
    }

    getLog().info("downloaded " + copied + " artifact(s)");
  }

  private Path resolveOutputDirectory(ArtifactItem item) {
    File dir = (item.outputDirectory == null) ? outputDirectory : item.outputDirectory;
    return dir.toPath();
  }

  private void ensureDirectoryExists(Path outputDir) throws MojoExecutionException {
    try {
      Files.createDirectories(outputDir);
    } catch (IOException ex) {
      throw new MojoExecutionException("could not create output directory " + outputDir, ex);
    }
  }

  private Artifact resolveArtifact(Artifact artifact) throws MojoExecutionException {
    try {
      ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, null);
      ArtifactResult result = repositorySystem.resolveArtifact(repositorySystemSession, request);
      return result.getArtifact();
    } catch (ArtifactResolutionException ex) {
      throw new MojoExecutionException("could not resolve artifact " + artifact, ex);
    }
  }

  private List<Artifact> resolveDependencies(Artifact artifact, String scope)
      throws MojoExecutionException {
    try {
      Dependency dependency = new Dependency(artifact, scope);
      CollectRequest collectRequest = new CollectRequest();
      collectRequest.setRoot(dependency);
      collectRequest.setRepositories(remoteRepositories);

      DependencyRequest dependencyRequest =
          new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(scope));

      return repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest)
          .getArtifactResults()
          .stream()
          .map(ArtifactResult::getArtifact)
          .collect(Collectors.toList());
    } catch (DependencyResolutionException ex) {
      throw new MojoExecutionException("could not resolve dependencies for " + artifact, ex);
    }
  }

  private void copyArtifact(Artifact artifact, Path target) throws MojoExecutionException {
    Path source = artifact.getFile().toPath();

    try {
      if (!overwrite && Files.exists(target)) {
        getLog().info("skipping existing " + target.getFileName());
        return;
      }

      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
      getLog().info("copied " + artifact + " to " + target);
    } catch (IOException ex) {
      throw new MojoExecutionException("could not copy " + source + " to " + target, ex);
    }
  }

  private String targetFileName(Artifact artifact) {
    if (!stripVersion) {
      return artifact.getFile().getName();
    }

    StringBuilder sb = new StringBuilder();
    sb.append(artifact.getArtifactId());
    String classifier = emptyToNull(artifact.getClassifier());
    if (classifier != null) {
      sb.append('-').append(classifier);
    }

    String extension = emptyToNull(artifact.getExtension());
    if (extension != null) {
      sb.append('.').append(extension);
    }

    return sb.toString();
  }

  private static boolean sameArtifact(Artifact a, Artifact b) {
    return a.getGroupId().equals(b.getGroupId())
        && a.getArtifactId().equals(b.getArtifactId())
        && a.getVersion().equals(b.getVersion())
        && String.valueOf(a.getClassifier()).equals(String.valueOf(b.getClassifier()))
        && String.valueOf(a.getExtension()).equals(String.valueOf(b.getExtension()));
  }

  private static String normalizeScope(String scope) throws MojoExecutionException {
    if (scope == null || scope.isBlank()) {
      return "runtime";
    }

    switch (scope) {
      case "compile":
      case "runtime":
      case "provided":
      case "system":
      case "test":
        return scope;
      default:
        throw new MojoExecutionException("unsupported includeScope: " + scope);
    }
  }

  private static String emptyToNull(String str) {
    return (str == null || str.isBlank()) ? null : str;
  }
}
