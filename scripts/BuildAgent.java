import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

// Run from project root: java scripts/BugAgent.java [maxRetries]
public class BuildAgent {

  private static final String MODULE = "student-grade-manager-core";
  private static final int DEFAULT_MAX_RETRIES = 3;

  private final Path projectRoot;
  private final int maxRetries;

  public BugAgent(Path projectRoot, int maxRetries) {
    this.projectRoot = projectRoot;
    this.maxRetries = maxRetries;
  }

  public static void main(String[] args) throws Exception {
    int maxRetries = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_MAX_RETRIES;
    new BugAgent(Path.of("").toAbsolutePath(), maxRetries).run();
  }

  private void run() throws Exception {
    System.out.println("=== bug-agent — max " + maxRetries + " Versuche ===");

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      System.out.println("\n--- Versuch " + attempt + " / " + maxRetries + " ---");

      BuildResult result = runGates();

      if (result.success()) {
        System.out.println("\nBuild erfolgreich nach " + attempt + " Versuch(en).");
        return;
      }

      String gate = detectGate(result.output());
      String errors = extractErrors(result.output());
      List<String> files = findAffectedFiles(result.output());

      System.out.println("Fehlgeschlagenes Gate: " + gate);
      System.out.println("Rufe Claude CLI auf...");

      callClaude(gate, attempt, errors, files);
    }

    System.err.println("\nBuild nach " + maxRetries + " Versuchen immer noch fehlerhaft.");
    System.exit(1);
  }

  private BuildResult runGates() throws Exception {
    StringBuilder output = new StringBuilder();

    if (exec(List.of("mvn", "verify", "-pl", MODULE), output) != 0) {
      return new BuildResult(false, output.toString());
    }
    return new BuildResult(
        exec(List.of("mvn", "checkstyle:check", "-pl", MODULE), output) == 0,
        output.toString());
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

  private String detectGate(String output) {
    if (output.contains("COMPILATION ERROR")) return "compile";
    if (output.contains("maven-surefire-plugin")) return "surefire";
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
            || l.contains("COMPILATION ERROR"))
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

  private void callClaude(String gate, int attempt, String errors, List<String> files)
      throws Exception {
    String prompt = String.format(
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

    new ProcessBuilder("claude", "-p", prompt, "--allowedTools", "Edit,Read,Bash")
        .directory(projectRoot.toFile())
        .inheritIO()
        .start()
        .waitFor();
  }

  private record BuildResult(boolean success, String output) { }
}
