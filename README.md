# Student Grade Manager

Java Maven-Anwendung zur Verwaltung von Studenten und deren Noten. Das Projekt folgt dem Maven-Archetype-Standard mit automatisierten Qualitätsprüfungen, CI/CD-Pipeline und kognitiver Entwicklungsunterstützung durch [cognitive-core](https://github.com/mindcockpit-ai/cognitive-core).

---

## Projektstruktur

```
student_grade_manager/
├── pom.xml                              # Root-POM: Abhängigkeiten, Plugins, distributionManagement
├── CLAUDE.md                            # Projektregeln für Claude Code
├── cognitive-core.conf                  # cognitive-core Konfiguration
├── owasp-suppressions.xml               # OWASP Dependency-Check Ausnahmen
├── .github/
│   └── workflows/
│       ├── ci.yml                       # CI: parallele Quality Agents bei Push/PR
│       └── release.yml                  # Release: JAR + GitHub Release bei Git-Tag
└── student-grade-manager-core/          # Kernmodul
    ├── pom.xml
    └── src/
        ├── main/java/de/student/grademanager/
        │   ├── App.java                 # Einstiegspunkt
        │   ├── model/
        │   │   ├── Student.java         # Studenten-Entität
        │   │   └── Grade.java           # Noten-Entität (validiert 1,0–5,0)
        │   └── service/
        │       ├── GradeService.java    # Interface
        │       └── GradeServiceImpl.java
        └── test/java/de/student/grademanager/
            ├── AppTest.java
            ├── model/GradeTest.java
            ├── model/StudentTest.java
            └── service/GradeServiceTest.java
```

---

## Technologie-Stack

| Komponente | Version | Zweck |
|---|---|---|
| Java | 17 | Sprache |
| Maven | 3.x | Build-Tool |
| JUnit | 4.13.2 | Testframework |
| Mockito | 5.14.2 | Mock-Objekte in Tests |
| commons-lang3 | 3.14.0 | Hilfsmethoden |
| SLF4J + Logback | 2.0.12 / 1.5.3 | Logging |
| Checkstyle | 3.3.1 | Coding-Standards (Google Style) |
| PMD | 3.21.2 | Statische Analyse |
| SpotBugs | 4.9.8.3 | Bug-Erkennung |
| OWASP Dependency Check | 9.2.0 | CVE-Scanning (CVSS ≥ 7 blockiert) |

---

## Build & Ausführen

```bash
# Kompilieren
mvn compile -pl student-grade-manager-core -q

# Tests ausführen
mvn test -pl student-grade-manager-core

# Alle Quality Gates auf einmal
mvn verify -pl student-grade-manager-core

# JAR bauen und ausführen
mvn package -pl student-grade-manager-core
java -jar student-grade-manager-core/target/student-grade-manager-core-1.0-SNAPSHOT.jar
```

---

## Quality Gates

Reihenfolge einhalten — jedes Gate ist Voraussetzung für das nächste:

```bash
mvn compile -pl student-grade-manager-core -q      # Gate 1: 0 Kompilierfehler
mvn test -pl student-grade-manager-core             # Gate 2: 100 % Tests grün
mvn checkstyle:check -pl student-grade-manager-core # Gate 3: 0 Style-Verstöße
mvn pmd:check -pl student-grade-manager-core        # Gate 4: 0 PMD-Fehler
mvn spotbugs:check -pl student-grade-manager-core   # Gate 5: 0 Bugs
```

---

## CI/CD

| Workflow | Trigger | Aktion |
|---|---|---|
| `ci.yml` | Push auf `main`/`develop`, PR auf `main` | compile → [test, checkstyle, pmd, spotbugs] parallel |
| `release.yml` | Git-Tag `v*` | `mvn verify` + JAR als GitHub Release |

Release erstellen:
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Architektur

Geschichtetes Modell (Layered Architecture) mit Interface + Adapter-Muster:

```
App (Einstiegspunkt)
 └── GradeService (Interface)
      └── GradeServiceImpl
           ├── Student (model)
           └── Grade (model, validiert 1,0–5,0)
```

Paket: `de.student.grademanager` — Google Style Guide (2 Leerzeichen, max. 100 Zeichen/Zeile).

---

## Codestil

| Element | Konvention |
|---|---|
| Klassen | `PascalCase` |
| Methoden/Variablen | `camelCase` |
| Konstanten | `UPPER_SNAKE_CASE` |
| Pakete | Kleinbuchstaben |
| Einrückung | 2 Leerzeichen |
| Zeilenlänge | max. 100 Zeichen |
| Logging | SLF4J + Logback (kein `System.out`) |

---

## Lizenz

Privates Lehr- und Übungsprojekt.
