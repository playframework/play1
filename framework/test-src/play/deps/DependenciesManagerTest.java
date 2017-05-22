package play.deps;

import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DependenciesManagerTest {
  private DependenciesManager manager = new DependenciesManager(new File("."), new File("."), new File("."));

  @Test
  public void usesDownloadedZipFileNameWithoutExtensions() {
    ArtifactDownloadReport artifact = artifact("pdf", "pdf-1.2.3.zip");
    assertEquals("pdf-1.2.3", manager.moduleName(artifact, false));
  }

  @Test
  public void usesDownloadedJarFileNameWithoutExtensions() {
    ArtifactDownloadReport artifact = artifact("pdf", "pdf-1.2.3.jar");
    assertEquals("pdf-1.2.3", manager.moduleName(artifact, false));
  }

  @Test
  public void shortModuleNames() {
    ArtifactDownloadReport artifact = artifact("pdf", "pdf-1.2.3.zip");
    assertEquals("pdf", manager.moduleName(artifact, true));
  }

  private ArtifactDownloadReport artifact(String name, String downloadedFileName) {
    ArtifactDownloadReport artifact = mock(ArtifactDownloadReport.class, RETURNS_DEEP_STUBS);
    when(artifact.getName()).thenReturn(name);
    when(artifact.getLocalFile().getName()).thenReturn(downloadedFileName);
    return artifact;
  }
}