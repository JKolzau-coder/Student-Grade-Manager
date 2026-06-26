package de.student.grademanager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class DependencyCveTest {

  private static final int GSON_SAFE_MAJOR = 2;
  private static final int GSON_SAFE_MINOR = 8;
  private static final int GSON_SAFE_PATCH = 9;

  private static final int LANG3_SAFE_MAJOR = 3;
  private static final int LANG3_SAFE_MINOR = 18;
  private static final int LANG3_SAFE_PATCH = 0;

  private static final int SNAKEYAML_SAFE_MAJOR = 2;
  private static final int SNAKEYAML_SAFE_MINOR = 0;
  private static final int SNAKEYAML_SAFE_PATCH = 0;

  @Test
  public void gsonNotVulnerableToGhsa4jrvPpp4Jm57() throws IOException {
    String version = readVersionFromPomProperties("com.google.code.gson", "gson");
    assertNotNull("Gson version not detectable from pom.properties", version);
    assertTrue(
        "GHSA-4jrv-ppp4-jm57: Gson " + version + " < 2.8.9 is vulnerable. Upgrade to >= 2.8.9.",
        isSafe(version, GSON_SAFE_MAJOR, GSON_SAFE_MINOR, GSON_SAFE_PATCH));
  }

  @Test
  public void commonsLang3NotVulnerableToGhsaJ288Q9x72f5v() {
    // Implementation-Version from the JAR manifest of the actually loaded class avoids
    // reading the wrong pom.properties from surefire-shared-utils (which bundles lang3 3.14.0).
    String version = StringUtils.class.getPackage().getImplementationVersion();
    assertNotNull("commons-lang3 version not detectable via Package manifest", version);
    assertTrue(
        "GHSA-j288-q9x7-2f5v: commons-lang3 " + version
            + " < 3.18.0 is vulnerable. Upgrade to >= 3.18.0.",
        isSafe(version, LANG3_SAFE_MAJOR, LANG3_SAFE_MINOR, LANG3_SAFE_PATCH));
  }

  @Test
  public void snakeYamlNotVulnerableToCve20221471() throws IOException {
    String version = readVersionFromPomProperties("org.yaml", "snakeyaml");
    assertNotNull("SnakeYAML version not detectable from pom.properties", version);
    assertTrue(
        "CVE-2022-1471: SnakeYAML " + version + " < 2.0 is vulnerable (CVSS 9.8)."
            + " Upgrade to >= 2.0.",
        isSafe(version, SNAKEYAML_SAFE_MAJOR, SNAKEYAML_SAFE_MINOR, SNAKEYAML_SAFE_PATCH));
  }

  private String readVersionFromPomProperties(String groupId, String artifactId)
      throws IOException {
    String path = "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
    try (InputStream is = getClass().getResourceAsStream(path)) {
      if (is == null) {
        return null;
      }
      Properties props = new Properties();
      props.load(is);
      return props.getProperty("version");
    }
  }

  private boolean isSafe(String version, int safeMajor, int safeMinor, int safePatch) {
    String[] parts = version.split("\\.");
    if (parts.length < 2) {
      return false;
    }
    int major = Integer.parseInt(parts[0]);
    int minor = Integer.parseInt(parts[1]);
    int patch = parts.length > 2 ? Integer.parseInt(parts[2].replaceAll("\\D.*", "")) : 0;
    return major > safeMajor
        || (major == safeMajor && minor > safeMinor)
        || (major == safeMajor && minor == safeMinor && patch >= safePatch);
  }
}
