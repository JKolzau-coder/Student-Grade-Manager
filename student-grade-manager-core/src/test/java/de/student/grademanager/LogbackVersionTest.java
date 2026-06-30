package de.student.grademanager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ch.qos.logback.classic.Logger;
import org.junit.Test;

/**
 * Security regression test guarding against CVE-2021-42550.
 *
 * <p>CVE-2021-42550 allows JNDI injection via a malicious Logback configuration file
 * in versions below 1.2.9. This test prevents silent downgrade through dependency
 * management changes by failing the build if the resolved Logback version is vulnerable.
 */
public class LogbackVersionTest {

  private static final int SAFE_MAJOR = 1;
  private static final int SAFE_MINOR = 2;
  private static final int SAFE_PATCH = 9;
  private static final int VERSION_PARTS = 3;

  @Test
  public void logbackVersionIsNotVulnerableToCve202142550() {
    String version = Logger.class.getPackage().getImplementationVersion();
    assertNotNull("Logback version not detectable from manifest", version);

    String[] parts = version.split("\\.");
    assertTrue("Logback version string malformed: " + version, parts.length >= VERSION_PARTS);

    int major = Integer.parseInt(parts[0]);
    int minor = Integer.parseInt(parts[1]);
    int patch = Integer.parseInt(parts[2].replaceAll("\\D.*", ""));

    boolean safe = major > SAFE_MAJOR
        || (major == SAFE_MAJOR && minor > SAFE_MINOR)
        || (major == SAFE_MAJOR && minor == SAFE_MINOR && patch >= SAFE_PATCH);

    assertTrue(
        "CVE-2021-42550: Logback " + version + " < 1.2.9 is vulnerable. Upgrade to >= 1.2.9.",
        safe);
  }
}
