# Student Grade Manager

Java Maven application for managing students and their grades. The project follows
the Maven multi-module standard with automated quality gates, a CI/CD pipeline, and
AI-assisted development via [cognitive-core](https://github.com/mindcockpit-ai/cognitive-core).

---

## Project Structure

```
student_grade_manager/
├── pom.xml                              # Parent POM: dependencies, plugins, distributionManagement
├── CLAUDE.md                            # Project rules for Claude Code
├── cognitive-core.conf                  # cognitive-core configuration
├── owasp-suppressions.xml               # OWASP Dependency-Check suppressions
├── .github/
│   └── workflows/
│       ├── ci.yml                       # CI: parallel quality agents on push/PR
│       └── release.yml                  # Release: JAR + GitHub Release on git tag
└── student-grade-manager-core/          # Core module
    ├── pom.xml
    ├── pmd-rules.xml                    # Project-specific PMD ruleset
    └── src/
        ├── main/java/de/student/grademanager/
        │   ├── App.java                 # Entry point (@SpringBootApplication)
        │   ├── model/
        │   │   ├── Student.java         # Immutable student entity
        │   │   └── Grade.java           # Immutable grade entity (validates 1.0–5.0)
        │   └── service/
        │       ├── GradeService.java    # Service interface
        │       └── GradeServiceImpl.java # HashMap-backed implementation
        └── test/java/de/student/grademanager/
            ├── AppTest.java             # Mockito — mocks GradeService
            ├── AppSmokeTest.java        # @SpringBootTest — context + main()
            ├── LogbackVersionTest.java  # CVE-2021-42550 regression guard
            ├── model/GradeTest.java
            ├── model/StudentTest.java
            └── service/GradeServiceTest.java
```

---

## Technology Stack

| Component | Version | Role |
|---|---|---|
| Java | 17 | Language and runtime (LTS) |
| Maven | 3.x | Build tool, dependency management |
| Spring Boot | 3.3.5 | IoC container, `CommandLineRunner` |
| JUnit | 4.13.2 | Test framework |
| Mockito | 5.14.2 | Interface-level mocks |
| SLF4J + Logback | via Spring BOM | Logging — replaces all `System.out.println` |
| Gson | 2.11.0 | JSON serialization |
| Commons Lang 3 | 3.14.0 | Utility library |
| Checkstyle | 3.3.1 | Style enforcement — Google Style Guide |
| PMD | 3.21.2 | Static analysis — unused vars, empty blocks |
| SpotBugs | 4.9.8.3 | Bug pattern detection — null paths, mutable exposure |
| JaCoCo | 0.8.12 | Test coverage — instruction ≥ 80%, branch ≥ 70% |
| OWASP Dependency Check | 9.2.0 | CVE scanning — blocks CVSS ≥ 7 |

---

## Build & Run

```bash
# Compile only
mvn compile -pl student-grade-manager-core -q

# Run tests with coverage
mvn test -pl student-grade-manager-core

# All quality gates at once
mvn verify -pl student-grade-manager-core

# Build and run the fat JAR
mvn package -pl student-grade-manager-core
java -jar student-grade-manager-core/target/student-grade-manager-core-1.0-SNAPSHOT.jar

# Generate Javadoc site
mvn javadoc:javadoc -pl student-grade-manager-core
# → open student-grade-manager-core/target/site/apidocs/index.html

# Open JaCoCo coverage report
open student-grade-manager-core/target/site/jacoco/index.html
```

---

## Quality Gates

Run in order — each gate is a prerequisite for the next:

```bash
mvn compile -pl student-grade-manager-core -q       # Gate 1: no compilation errors
mvn test -pl student-grade-manager-core              # Gate 2: 100% tests green
mvn checkstyle:check -pl student-grade-manager-core  # Gate 3: 0 style violations
mvn pmd:check -pl student-grade-manager-core         # Gate 4: 0 PMD violations
mvn spotbugs:check -pl student-grade-manager-core    # Gate 5: 0 bug patterns
```

`-DskipTests` is blocked by Maven Enforcer — there is no legitimate bypass.

---

## CI/CD

| Workflow | Trigger | Action |
|---|---|---|
| `ci.yml` | Push to `main`/`develop`, PR to `main` | compile → [test, checkstyle, pmd, spotbugs] parallel |
| `release.yml` | Git tag `v*` | `mvn verify` + JAR as GitHub Release |

Create a release:
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Architecture

Layered architecture with interface + adapter pattern:

```
App (@SpringBootApplication, CommandLineRunner)
 └── GradeService (interface — defined before implementation)
      └── GradeServiceImpl (@Service, HashMap-backed)
           ├── Student (final, immutable entity)
           └── Grade (final, immutable entity — validates 1.0–5.0)
```

Base package: `de.student.grademanager` — Google Style Guide (2-space indent, max 100 chars/line).

---

## Code Style

| Element | Convention |
|---|---|
| Classes, interfaces | `PascalCase` |
| Methods, variables | `camelCase` |
| Constants | `UPPER_SNAKE_CASE` |
| Packages | lowercase, no underscores |
| Indentation | 2 spaces |
| Line length | max 100 characters |
| Logging | SLF4J + Logback — `System.out.println` forbidden in production code |

---

## License

Private educational project.
