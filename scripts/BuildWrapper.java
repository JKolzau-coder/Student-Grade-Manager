import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

// Run from project root: java scripts/BuildWrapper.java [maxRetries]
public class BuildWrapper {

  private static final String MODULE = "student-grade-manager-core";
  private static final int DEFAULT_MAX_RETRIES = 3;

  private final Path projectRoot;
  private final int maxRetries;

  public BuildWrapper(Path projectRoot, int maxRetries) {
    this.projectRoot = projectRoot;
    this.maxRetries = maxRetries;
  }

  public static void main(String[] args) throws Exception {
    int maxRetries = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_MAX_RETRIES;
    new BuildWrapper(Path.of("").toAbsolutePath(), maxRetries).run();
  }

  private void run() throws Exception {
    System.out.println("=== build-wrapper — max " + maxRetries + " Versuche ===");

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      System.out.println("\n--- Versuch " + attempt + " / " + maxRetries + " ---");

      BuildResult result = runGates();

      if (result.success()) {
        System.out.println("\nBuild erfolgreich nach " + attempt + " Versuch(en).");
        return;
      }

      System.out.println("Fehlgeschlagenes Gate: " + result.gate());
      System.out.println("Rufe Claude CLI auf...");

      callClaude(result.gate(), attempt, result.output());
    }

    System.err.println("\nBuild nach " + maxRetries + " Versuchen immer noch fehlerhaft.");
    System.exit(1);
  }

  private BuildResult runGates() throws Exception {
    StringBuilder out = new StringBuilder();

    if (exec(List.of("mvn", "verify", "-pl", MODULE), out) != 0) {
      return new BuildResult(false, out.toString(), detectMavenGate(out.toString()));
    }
    if (exec(List.of("mvn", "checkstyle:check", "-pl", MODULE), out) != 0) {
      return new BuildResult(false, out.toString(), "checkstyle");
    }

    return new BuildResult(true, out.toString(), "");
  }

  private int exec(List<String> cmd, StringBuilder collector) throws Exception {
    Process p = new ProcessBuilder(cmd)
        .directory(projectRoot.toFile())
        .redirectErrorStream(true)
        .start();
    try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      String line;
      while ((line = r.readLine()) != null) {
        System.out.println(line);
        collector.append(line).append('\n');
      }
    }
    return p.waitFor();
  }

  private String detectMavenGate(String output) {
    if (output.contains("COMPILATION ERROR")) return "compile";
    if (output.contains("maven-surefire-plugin")) {
      // CVE regression test failures → same fix path as OSV scanner (pom.xml upgrade allowed)
      if (output.contains("GHSA-") || output.contains("CVE-")) return "osv";
      return "surefire";
    }
    if (output.contains("maven-checkstyle-plugin")) return "checkstyle";
    if (output.contains("maven-pmd-plugin")) return "pmd";
    if (output.contains("spotbugs-maven-plugin")) return "spotbugs";
    return "unknown";
  }

  private String extractErrors(String output) {
    return Arrays.stream(output.split("\n"))
        .filter(l -> l.startsWith("[ERROR]")
            || (l.contains("[WARNING]") && l.contains(".java"))
            || l.contains("Tests run")
            || l.contains("COMPILATION ERROR")
            || l.contains("GHSA-")
            || l.contains("CVE-")
            || l.contains("known vulnerabilit")
            || l.contains("FIXED VERSION"))
        .limit(60)
        .collect(Collectors.joining("\n"));
  }

  private List<String> findAffectedFiles(String output) throws IOException {
    Matcher m = Pattern.compile("[A-Za-z_/]+\\.java").matcher(output);
    Set<String> names = new LinkedHashSet<>();
    while (m.find()) {
      names.add(Path.of(m.group()).getFileName().toString());
    }

    List<String> paths = new ArrayList<>();
    for (String name : names) {
      try (Stream<Path> walk = Files.walk(projectRoot)) {
        walk.filter(p -> p.getFileName().toString().equals(name))
            .map(Path::toString)
            .findFirst()
            .ifPresent(paths::add);
      }
    }
    return paths;
  }

  private void callClaude(String gate, int attempt, String output) throws Exception {
    String prompt = "osv".equals(gate)
        ? buildOsvPrompt(attempt, output)
        : buildDefaultPrompt(gate, attempt, output);

    String claudeBin = findClaude();
    new ProcessBuilder(claudeBin, "-p", prompt, "--allowedTools", "Edit,Read,Bash")
        .directory(projectRoot.toFile())
        .inheritIO()
        .start()
        .waitFor();
  }

  private String findClaude() throws IOException {
    // 1. claude already on PATH
    try {
      Process p = new ProcessBuilder("which", "claude")
          .redirectErrorStream(true).start();
      String line = new BufferedReader(new InputStreamReader(p.getInputStream()))
          .readLine();
      if (p.waitFor() == 0 && line != null && !line.isBlank()) return line.trim();
    } catch (Exception ignored) { }

    // 2. common npx / global install locations
    List<String> candidates = List.of(
        System.getProperty("user.home") + "/.npm-global/bin/claude",
        "/opt/homebrew/bin/claude",
        "/usr/local/bin/claude"
    );
    for (String c : candidates) {
      if (new File(c).canExecute()) return c;
    }

    // 3. any claude binary under ~/.npm/_npx
    Path npx = Path.of(System.getProperty("user.home"), ".npm", "_npx");
    if (Files.exists(npx)) {
      try (Stream<Path> walk = Files.walk(npx, 4)) {
        Optional<Path> found = walk
            .filter(p -> p.getFileName().toString().equals("claude") && p.toFile().canExecute())
            .findFirst();
        if (found.isPresent()) return found.get().toString();
      }
    }

    throw new IOException("claude CLI not found. Install via: npm install -g @anthropic-ai/claude-code");
  }

  private String buildOsvPrompt(int attempt, String osvOutput) {
    String findings = parseOsvFindings(osvOutput);
    return String.format(
        "Fix OSV vulnerability findings in the student-grade-manager Maven project.%n%n"
        + "Attempt     : %d of %d%n"
        + "Root pom.xml: %s/pom.xml%n%n"
        + "--- Vulnerable packages and required fixes ---%n%s%n%n"
        + "Steps:%n"
        + "1. Read %s/pom.xml%n"
        + "2. For each package above, find its <xxx.version> property and change the value%n"
        + "   to the FIXED VERSION listed. Edit ONLY those version properties. Do NOT touch%n"
        + "   any source files under src/.%n"
        + "3. Run: mvn verify -pl %s to confirm the build still passes",
        attempt, maxRetries, projectRoot,
        findings,
        projectRoot,
        MODULE);
  }

  private String parseOsvFindings(String output) {
    // OSV scanner table format: lines containing GHSA-/CVE- and pipe separators
    List<String> tableLines = Arrays.stream(output.split("\n"))
        .filter(l -> (l.contains("GHSA-") || l.contains("CVE-")) && l.contains("|"))
        .collect(Collectors.toList());

    if (!tableLines.isEmpty()) {
      return tableLines.stream().map(l -> {
        String[] parts = l.split("\\|");
        if (parts.length >= 7) {
          return String.format("  Package %-45s  current: %-10s  fix to: %s",
              parts[4].trim(), parts[5].trim(), parts[6].trim());
        }
        return "  " + l.trim();
      }).collect(Collectors.joining("\n"));
    }

    // Fallback: surefire CVE assertion messages (e.g. "GHSA-xxxx: Gson 2.8.5 < 2.8.9 ...")
    return Arrays.stream(output.split("\n"))
        .filter(l -> l.contains("GHSA-") || l.contains("CVE-"))
        .map(l -> "  " + l.trim())
        .collect(Collectors.joining("\n"));
  }

  private String buildDefaultPrompt(String gate, int attempt, String output) throws Exception {
    List<String> files = findAffectedFiles(output);
    String errors = extractErrors(output);
    return String.format(
        "Fix a Maven build failure in the student-grade-manager Java project.%n%n"
        + "Failed gate : %s%n"
        + "Attempt     : %d of %d%n"
        + "Project root: %s%n%n"
        + "--- Maven error output ---%n%s%n%n"
        + "--- Affected source files ---%n%s%n%n"
        + "Instructions:%n"
        + "- Read the affected files, identify the root cause, and edit only source files%n"
        + "  under src/main/java or src/test/java.%n"
        + "- Do NOT modify pom.xml or any build configuration.%n"
        + "- After editing, verify with: mvn verify -pl %s"
        + " (and mvn checkstyle:check if gate was checkstyle).%n"
        + "- Fix all violations in one pass so the next build attempt succeeds.",
        gate, attempt, maxRetries, projectRoot,
        errors,
        String.join("\n", files),
        MODULE);
  }

  private record BuildResult(boolean success, String output, String gate) { }
}
