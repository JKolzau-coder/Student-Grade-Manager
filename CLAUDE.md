# Student Grade Manager — Project Rules

## Project Overview

Multi-module Maven project for structured student and grade management.

| Module | Purpose |
|---|---|
| `student-grade-manager-core` | Core business logic as executable Spring Boot JAR |

---

## Project Milestones

| Milestone | Status | Focus | Acceptance criteria |
|---|---|---|---|
| **M1** | ✓ Done | Data model + service layer | `Grade` and `Student` are immutable; `GradeService` interface defined before `GradeServiceImpl`; constructor validation throws `IllegalArgumentException` on null or out-of-range |
| **M2** | ✓ Done | Tests + quality gates | 100% tests green; 0 Checkstyle / PMD / SpotBugs violations; `-DskipTests` blocked by Enforcer; `getGrades()` returns `unmodifiableList` |
| **M3** | ✓ Done | Spring Boot + Docker | `@SpringBootApplication` with `CommandLineRunner`; layered fat JAR; `eclipse-temurin:17-jre-alpine` Docker image; SLF4J throughout |
| **M4** | ✓ Done | BuildWrapper + CI/CD | `BuildWrapper.java` detects gate, calls Claude CLI, retries up to 3×; `activate.sh` wrapper; GitHub Actions pipeline; Docker push to ghcr.io on `dev/**` |
| **M5** | → Next | Coverage gate + integration test | `jacoco:check` enforced at `verify` (instruction ≥ 80%, branch ≥ 70%); `getAllStudents()` added to `GradeService`; `@SpringBootTest` smoke test covers `App.main()` |
| **M6–M7** | ○ Planned | JPA + REST API | `GradeServiceImpl` replaced by JPA implementation — `App.java` unchanged; Spring MVC controller sits above the interface; all existing 19 tests remain green |
| **M8–M9** | ○ Planned | Auth + scale | Spring Security `@PreAuthorize` on interface methods only; multi-tenancy; Kubernetes Helm chart |

### Current milestone: M5

**What is in scope:**
- Add `jacoco:check` goal to `student-grade-manager-core/pom.xml` (bound to `verify`)
- Add `getAllStudents()` to `GradeService` interface and `GradeServiceImpl`
- Add `@SpringBootTest` smoke test for `App.main()` (removes the 0% coverage gap)
- Set coverage thresholds: instruction ≥ 80%, branch ≥ 70%

**What is out of scope until M6:**
- JPA, REST controllers, persistence layer
- Spring Security or authentication
- Additional service methods beyond `getAllStudents()`

**Key constraint:** The `jacoco:check` goal must use late-binding `@{argLine}` in Surefire —
a static `${argLine}` overwrites JaCoCo's property and produces 0% coverage.

---

## Technology Stack

| Technology | Version | Role |
|---|---|---|
| **Java** | 17 | Language and runtime (LTS) |
| **Maven** | 3.x | Build tool, dependency management |
| **Spring Boot** | 3.3.5 | IoC container, `CommandLineRunner`, `@Service` wiring |
| **Spring Boot BOM** | 3.3.5 | Single version authority for all Spring + Logback transitive deps |
| **JUnit** | 4.13.2 | Test framework |
| **Mockito** | 5.14.2 | Mocking framework (interface-level mocks only) |
| **SLF4J + Logback** | via Spring BOM | Logging — replaces all `System.out.println` |
| **Gson** | 2.11.0 | JSON serialization |
| **Commons Lang 3** | 3.14.0 | Utility library |
| **Checkstyle** | 3.3.1 | Style enforcement — Google Style Guide |
| **PMD** | 3.21.2 | Static analysis — unused vars, empty blocks, casts |
| **SpotBugs** | 4.9.8.3 | Bug pattern detection — null paths, resource leaks, mutable exposure |
| **JaCoCo** | 0.8.12 | Test coverage — instruction, branch, method |
| **Maven Enforcer** | 3.4.1 | Blocks `-DskipTests` at build level |
| **Maven Surefire** | 3.2.5 | Test runner — `@{argLine}` late-binding, `-ea` assertions active |
| **Spring Boot Maven Plugin** | 3.3.5 | Layered fat JAR packaging |
| **Docker base image** | eclipse-temurin:17-jre-alpine | Minimal JRE — no JDK in production container |

---

## Quick Reference

| Item | Value |
|---|---|
| **Group ID** | `de.student` |
| **Root artifact** | `student-grade-manager` `1.0-SNAPSHOT` |
| **Core artifact** | `student-grade-manager-core` |
| **Main class** | `de.student.grademanager.App` |
| **Main branch** | `main` |
| **Base package** | `de.student.grademanager` |

---

## Build and Test Commands

### Individual Gates

| Action | Command |
|---|---|
| Compile only | `mvn compile -pl student-grade-manager-core -q` |
| Run tests + JaCoCo coverage | `mvn test -pl student-grade-manager-core` |
| Style check (Checkstyle) | `mvn checkstyle:check -pl student-grade-manager-core` |
| Static analysis (PMD) | `mvn pmd:check -pl student-grade-manager-core` |
| Bug detection (SpotBugs) | `mvn spotbugs:check -pl student-grade-manager-core` |
| All gates (compile → test → style → PMD → SpotBugs) | `mvn verify -pl student-grade-manager-core` |
| Package fat JAR | `mvn package -pl student-grade-manager-core` |

### Reporting

| Action | Command |
|---|---|
| Open JaCoCo coverage report | `open student-grade-manager-core/target/site/jacoco/index.html` |
| Dependency tree | `mvn dependency:tree -pl student-grade-manager-core` |

### Running the Application

| Action | Command |
|---|---|
| Run via Maven | `mvn spring-boot:run -pl student-grade-manager-core` |
| Run packaged JAR | `java -jar student-grade-manager-core/target/student-grade-manager-core-1.0-SNAPSHOT.jar` |
| Build Docker image | `docker build -t student-grade-manager student-grade-manager-core/` |
| Run Docker container | `docker run student-grade-manager` |

### BuildWrapper

| Action | Command |
|---|---|
| Run BuildWrapper manually | `java scripts/BuildWrapper.java` |
| Run with custom retry count | `java scripts/BuildWrapper.java 5` |
| Enable local auto-fix on `mvn` failure | `source scripts/activate.sh` |

---

## Project Structure

```
student_grade_manager/                        ← project root
├── pom.xml                                   ← parent POM (groupId: de.student)
├── CLAUDE.md                                 ← this file
├── scripts/
│   ├── BuildWrapper.java                       ← agentic build-fixer (Java single-file)
│   └── activate.sh                           ← local mvn wrapper with auto-fix
└── student-grade-manager-core/               ← only executable module
    ├── pom.xml                               ← child POM
    ├── Dockerfile                            ← layered multi-stage build
    ├── spotbugs-exclude.xml                  ← accepted SpotBugs suppressions (with rationale)
    └── src/
        ├── main/
        │   └── java/de/student/grademanager/
        │       ├── App.java                  ← @SpringBootApplication, CommandLineRunner
        │       ├── model/
        │       │   ├── Student.java          ← final, immutable entity
        │       │   └── Grade.java            ← final, immutable entity, 1.0–5.0 range
        │       └── service/
        │           ├── GradeService.java     ← interface — DEFINE BEFORE IMPLEMENTATION
        │           └── GradeServiceImpl.java ← @Service, HashMap-backed, assertions active
        └── test/
            └── java/de/student/grademanager/
                ├── AppTest.java              ← Mockito — mocks GradeService
                ├── model/
                │   ├── GradeTest.java        ← no mock — tests Grade directly
                │   └── StudentTest.java      ← no mock — tests Student directly
                └── service/
                    └── GradeServiceTest.java ← no mock — tests GradeServiceImpl directly
```

**Package rule:** All production classes live under `de.student.grademanager`.
Test classes mirror the production package structure exactly.
Never place a test class in a package different from its subject class.

---

## Fitness Gates — Required Before Done

Run in order — each gate is a prerequisite for the next:

| Gate | Command | Pass condition |
|---|---|---|
| **Compile** | `mvn compile -pl student-grade-manager-core -q` | exit 0 — no broken code enters the codebase |
| **Test** | `mvn test -pl student-grade-manager-core` | 100% tests green — new functionality requires at least one new test |
| **Style** | `mvn checkstyle:check -pl student-grade-manager-core` | 0 violations against Google Style Guide |
| **PMD** | `mvn pmd:check -pl student-grade-manager-core` | 0 violations — unused vars, empty blocks, unnecessary casts |
| **SpotBugs** | `mvn spotbugs:check -pl student-grade-manager-core` | 0 bugs — null risks, resource leaks, mutable collection exposure |

All gates at once: `mvn verify -pl student-grade-manager-core`

**`-DskipTests` is blocked by Maven Enforcer.** There is no legitimate bypass.

---

## Programming Conventions

### Formatting (enforced by Checkstyle — google_checks.xml)

- **Indent:** 2 spaces — no tabs
- **Line length:** max 100 characters
- **Opening brace:** same line as the declaration
- **No trailing whitespace**
- **One blank line** between methods; no blank lines at the start or end of a block
- **Imports:** no wildcard imports; sorted alphabetically within groups

### Naming

| Element | Convention | Example |
|---|---|---|
| Classes, interfaces, enums | `UpperCamelCase` | `GradeServiceImpl` |
| Methods, variables, parameters | `lowerCamelCase` | `calculateAverage` |
| Constants (`static final`) | `UPPER_SNAKE_CASE` | `MAX_GRADE` |
| Packages | all lowercase, no underscores | `de.student.grademanager.service` |
| Test methods | descriptive sentence in `lowerCamelCase` | `addGradeForUnknownStudentThrowsException` |

### Visibility and Immutability

- **Fields:** `private` always — no package-private or public fields
- **Model classes:** `final` — prevents subclassing and `EI_EXPOSE_REP2` SpotBugs findings
- **Model fields:** `final` — assigned once in constructor, no setters
- **Return collections:** wrap with `Collections.unmodifiableList()` — never return a reference to an internal mutable list
- **No null returns from collection methods:** return `Collections.emptyList()` instead

### Null Handling

- Validate in constructors: throw `IllegalArgumentException` immediately on `null` input
- Guard method entries with `if (!students.containsKey(id)) throw new IllegalArgumentException(...)`
- Never return `null` from a method that callers chain — return empty collections or `Optional`

### Logging

- **SLF4J + Logback only** — `System.out.println` is forbidden in production code
- Console I/O in interactive methods is the only accepted exception:
  annotate with `@SuppressWarnings("PMD.SystemPrintln")` and document why
- Use parameterized logging: `logger.info("Average: {}", avg)` — never string concatenation
- Log level guidance: `info` for normal flow, `warn` for recoverable issues, `error` for caught exceptions

### Assertions

- JVM flag `-ea` must be active in all test runs — enforced via Surefire `argLine`
- `assert` statements guard internal invariants in `GradeServiceImpl`
  (map consistency, result range) — these are not input validation, they are invariant checks
- Every test class that relies on assertions must include:
  ```java
  @BeforeClass
  public static void assertionsEnabled() {
    assertTrue(MyClass.class.desiredAssertionStatus());
  }
  ```

### Interface-First Rule

Define `GradeService` (interface) **before** writing `GradeServiceImpl`.
This enables Mockito tests to be written immediately and keeps the contract
explicit. Never create an implementation class without a corresponding interface
in the same package.

---

## Testing Rules

### Mock Decision

```
Is the dependency external to the class under test?
  YES → Mock with Mockito
  NO  → Test the real class directly
```

| Test class | Subject | GradeService mocked? | Why |
|---|---|---|---|
| `AppTest` | `App` | **Yes** | `GradeService` is App's external dependency |
| `GradeServiceTest` | `GradeServiceImpl` | **No** | Own class — test the real logic |
| `GradeTest` | `Grade` | **No** | No dependencies |
| `StudentTest` | `Student` | **No** | No dependencies |

**Never mock `GradeServiceImpl`** — if you find yourself doing this, the test
is in the wrong class.

### Test Coverage per Method

Every public method needs: **1 happy path + 1 boundary case + 1 error case**

| Category | Description |
|---|---|
| Happy path | Normal inputs, expected output |
| Boundary low | Minimum valid value (e.g., grade 1.0) |
| Boundary high | Maximum valid value (e.g., grade 5.0) |
| Invalid input | `null`, empty, out-of-range |
| Error case | State that triggers exception or assertion |

### Test Isolation

- Each test creates its own fresh instance in `@Before` — no shared mutable state between tests
- No `System.out.println` in tests — use `verify()` and assertions only
- Full test suite must complete in under 30 seconds

---

## Hooks — Automated Quality Checks

| Event | Trigger | Action |
|---|---|---|
| `PostToolUse` | Any source file edited | Immediate compile — syntax errors reported instantly |
| `Stop` | Claude ends work session | Full test suite — task cannot be marked done while tests are red |

---

## Skills — When to Use Which

| Skill | When |
|---|---|
| `/code-review` | After implementing new methods — checks for duplication, unnecessary complexity |
| `/security-baseline` | Before every commit to `main` — checks for security vulnerabilities |
| `/skill-sync` | Regularly — keeps cognitive-core framework up to date |
| `/session-resume` | At the start of a new session — loads context from last session |

**Required skills per workflow:**
- New feature → `/code-review` after implementation → `/security-baseline` before commit
- Bug fix → `/code-review` to verify
- Refactoring → `/code-review`

---

## Agent Profiles

### Profile: Solution Architect

**Focus:** Module boundaries, interface design, and technology decisions

**Key questions:**
1. Which layer does this logic belong to?
2. Can this external dependency be hidden behind an interface?
3. Will this decision still be understandable in 6 months?

**Quality criteria:**
- No module uses classes it has not declared as an explicit dependency
- Interfaces are defined before implementations
- Architectural decisions are documented

---

### Profile: Test Specialist

**Focus:** Coverage, isolation, and boundary analysis

**Key questions:**
1. Does this test need external resources (network, DB)? → Use a mock
2. Are upper and lower boundaries explicitly tested?
3. Is the test name self-explanatory without a comment?

**Quality criteria:**
- Every public method: 1 happy path + 1 boundary + 1 error case
- No test writes to stdout
- Full suite runs in under 30 seconds
- Mocks only for external dependencies, never for own classes

---

## Allowed Exceptions

If a fitness gate cannot pass for a documented reason:

1. Add a comment at the suppression point explaining **why** (not what)
2. Add the suppression annotation (`@SuppressWarnings`, SpotBugs filter entry) with justification
3. Document in the commit message

Never permanently disable a gate without discussion.

---

## Agents

See `.claude/AGENTS_README.md` for the agent team documentation.

## Imported Rules

@import .claude/rules/java-conventions.md
@import .claude/rules/testing.md
