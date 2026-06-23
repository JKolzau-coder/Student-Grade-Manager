# Student Grade Manager
### Präsentationsgliederung

---

## 1. Projektidee

### 1.1 Problemstellung
- Notenverwaltung erfolgt manuell ohne strukturierte Datenhaltung
- Keine automatische Durchschnittsberechnung je Student
- Keine Validierung von Notenwerten und Eingaben
- Fehlende Qualitätssicherung bei Softwareänderungen

### 1.2 Lösung
- Java-Anwendung zur **strukturierten Verwaltung von Studenten und Noten**
- **Validierte Dateneingabe**: Noten 1,0–5,0, kein Nullwert, keine leeren Felder
- **Automatische Durchschnittsberechnung** je Student
- **Spring Boot** als Laufzeitcontainer mit `CommandLineRunner`-Integration
- **Dockerisierung** für reproduzierbare Deployments

### 1.3 Zielgruppe
- Studierende und Lehrende, die Notendaten verwalten
- Entwickler, die ein Beispielprojekt für Maven-Qualitätsprozesse suchen
- Teams, die einen automatisierten Build-Agenten evaluieren

### 1.4 Features & Use Cases

**Kernfunktionen**

| Feature | Beschreibung |
|---|---|
| **Student anlegen** | Name und ID werden gespeichert, Duplikate werden erkannt |
| **Note hinzufügen** | Fach und Notenwert (1,0–5,0) werden validiert und gespeichert |
| **Durchschnitt berechnen** | Arithmetisches Mittel aller Noten je Student |
| **Interaktive Eingabe** | Scanner-gesteuerter Eingabemodus für manuelle Erfassung |
| **Tabellarischer Vergleich** | Zwei Studenten nebeneinander mit allen Noten und Durchschnitt |

**Qualitätssicherung**

| Feature | Beschreibung |
|---|---|
| **Compile-Gate** | 0 Kompilierfehler — Pflicht vor jedem weiteren Gate |
| **Test-Gate** | JUnit 4 mit Mockito — 100 % der Tests müssen grün sein |
| **Checkstyle** | Google Style Guide — 2 Leerzeichen, max. 100 Zeichen |
| **PMD** | Statische Analyse — ungenutzte Variablen, leere Blöcke |
| **SpotBugs** | Bug-Erkennung — NullPointer, Ressourcen-Leaks |
| **JaCoCo** | Testabdeckungsbericht je Methode und Branch |

**Build-Agent**

| Feature | Beschreibung |
|---|---|
| **Automatische Fehlerbehebung** | `BuildAgent.java` erkennt fehlgeschlagenes Gate und ruft Claude CLI auf |
| **Gate-Erkennung** | Compile / Surefire / Checkstyle / PMD / SpotBugs aus Maven-Output |
| **Retry-Logik** | Bis zu 3 Versuche, konfigurierbar per Argument |
| **Shell-Hook** | `activate.sh` überschreibt `mvn` — bei Fehler greift Agent automatisch ein |
| **CI-Integration** | GitHub Actions-Job `Auto-Fix` läuft bei `failure()` der Gates |

### 1.5 Mehrwert

| Ohne System | Mit System |
|---|---|
| Manuelle Notenpflege | Validierte, strukturierte Datenhaltung |
| Kein Feedback bei Fehleingaben | Sofortige Exception mit Fehlerbeschreibung |
| Kein Qualitätscheck | 5 automatisierte Gates bei jedem Build |
| Build-Fehler blockieren Entwickler | BuildAgent repariert Gates automatisch |
| Manuelle Docker-Builds | Automatischer Build und Push via GitHub Actions |
| Keine Testabdeckungsmetrik | JaCoCo-Report lokal und im CI verfügbar |

---

## 2. Architektur

### 2.1 Systemübersicht
```
App (CommandLineRunner, @SpringBootApplication)
  └── GradeService (Interface)
        └── GradeServiceImpl (@Service)
              ├── Student (model — final, validiert)
              └── Grade (model — final, Wertebereich 1,0–5,0)
```

### 2.2 Paketstruktur
```
de.student.grademanager/
  ├── App.java                  Einstiegspunkt, Spring Boot, interaktive Schnittstelle
  ├── model/
  │     ├── Student.java        Entität: Name, StudentId
  │     └── Grade.java          Entität: Fach, Note (1,0–5,0)
  └── service/
        ├── GradeService.java   Interface: addStudent, addGrade, calculateAverage, …
        └── GradeServiceImpl.java  Implementierung mit HashMap + Assertions
```

### 2.3 Schnittstellen

| Methode | Signatur | Beschreibung |
|---|---|---|
| `addStudent` | `(String name, int studentId)` | Student anlegen, maps synchron halten |
| `addGrade` | `(int studentId, String subject, double value)` | Note anhängen, assert auf Map-Konsistenz |
| `calculateAverage` | `(int studentId) → double` | Arithmetisches Mittel, 0,0 bei leerer Liste |
| `getStudent` | `(int studentId) → Student` | Student-Objekt abrufen |
| `getGrades` | `(int studentId) → List<Grade>` | Unveränderliche Noten-Liste |
| `hasStudent` | `(int studentId) → boolean` | Existenzprüfung |

### 2.4 Invarianten & Assertions
```
addStudent()
  assert students.containsKey(id) && grades.containsKey(id)
    : "addStudent left maps inconsistent for id " + id

addGrade()
  assert gradeList != null
    : "grades map out of sync with students map for id " + id

calculateAverage()
  assert result >= 0.0 && result <= 5.0
    : "calculateAverage produced out-of-range result: " + result
```
Tests laufen mit `-ea` — `@BeforeClass` prüft `desiredAssertionStatus()`.

---

## 3. Qualitätssicherung

### 3.1 Fitness Gates

| Gate | Befehl | Ziel |
|---|---|---|
| Kompilierung | `mvn compile -pl student-grade-manager-core -q` | 0 Fehler |
| Tests | `mvn test -pl student-grade-manager-core` | 100 % grün |
| Checkstyle | `mvn checkstyle:check -pl student-grade-manager-core` | 0 Verstöße |
| PMD | `mvn pmd:check -pl student-grade-manager-core` | 0 Regelverletzungen |
| SpotBugs | `mvn spotbugs:check -pl student-grade-manager-core` | 0 Bugs |
| Alle Gates | `mvn verify -pl student-grade-manager-core` | BUILD SUCCESS |

### 3.2 Testabdeckung (JaCoCo)

| Klasse | Methoden | Abdeckung |
|---|---|---|
| `Grade` | Konstruktor, getSubject, getValue | 100 % |
| `Student` | Konstruktor, getStudentId, getName | 100 % |
| `GradeServiceImpl` | Alle 8 Methoden | > 90 % |
| `App` | run, runInteractive, printComparison, printComparisonTable, printStudentRows | > 80 % |

Report lokal öffnen:
```bash
mvn test -pl student-grade-manager-core
open student-grade-manager-core/target/site/jacoco/index.html
```

### 3.3 Enforcer — keine Test-Bypässe
```xml
<evaluateBeanshell>
  <condition>!"${skipTests}".equals("true")</condition>
  <message>-DskipTests ist in diesem Projekt nicht erlaubt.</message>
</evaluateBeanshell>
```

---

## 4. Build-Agent

### 4.1 Problemstellung
- Build-Fehler durch Style-Verletzungen, fehlende Interface-Methoden oder kaputte Tests blockieren den Entwickler
- Manuelle Fehlersuche bei 5 Gates kostet Zeit

### 4.2 Lösung — `scripts/BuildAgent.java`
```
mvn verify schlägt fehl
  → BuildAgent erkennt fehlgeschlagenes Gate (compile / surefire / checkstyle / pmd / spotbugs)
  → Fehlerausgabe wird extrahiert
  → Betroffene Java-Dateien werden lokalisiert
  → Claude CLI wird mit Fehlerkontext aufgerufen:
      claude -p "$PROMPT" --allowedTools Edit,Read,Bash
  → mvn verify läuft erneut
  → Bei Erfolg: Build grün — sonst nächster Versuch (max. 3)
```

### 4.3 Ausführung

| Aufruf | Beschreibung |
|---|---|
| `java scripts/BuildAgent.java` | Standard (3 Versuche) |
| `java scripts/BuildAgent.java 5` | 5 Versuche |
| `source scripts/activate.sh` + `mvn compile` | Automatischer Hook — Agent startet bei Fehler |

### 4.4 CI-Integration
```
push → Gates (compile / test / checkstyle / pmd / spotbugs)
            │
       failure()?
            │ JA
       Auto-Fix Job:
         npm install -g @anthropic-ai/claude-code
         java scripts/BuildAgent.java
         git commit "fix(auto): resolve build gate violations [skip ci]"
         git push
```

---

## 5. Spring Boot & Docker

### 5.1 Spring Boot Integration

| Komponente | Änderung |
|---|---|
| `App` | `@SpringBootApplication`, `implements CommandLineRunner` |
| `GradeServiceImpl` | `@Service` — IoC-gesteuerte Instanziierung |
| `main()` | `SpringApplication.run(App.class, args)` |
| `application.properties` | `web-application-type=none`, `banner-mode=off` |
| `pom.xml` | `spring-boot-starter` + `spring-boot-maven-plugin` (Layered JAR) |

### 5.2 Dockerfile — Layered Multi-Stage Build
```dockerfile
FROM eclipse-temurin:17-jre-alpine AS builder
WORKDIR /workspace
COPY target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/dependencies/ ./
COPY --from=builder /workspace/spring-boot-loader/ ./
COPY --from=builder /workspace/snapshot-dependencies/ ./
COPY --from=builder /workspace/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
```

### 5.3 Vorteile Layered Build

| Schicht | Inhalt | Ändert sich |
|---|---|---|
| `dependencies` | Alle Maven-Abhängigkeiten | Selten |
| `spring-boot-loader` | Spring Boot Launcher-Klassen | Selten |
| `snapshot-dependencies` | SNAPSHOT-Abhängigkeiten | Manchmal |
| `application` | Eigener Quellcode | Häufig |

Docker cached die unteren Schichten — nur die `application`-Schicht wird bei Code-Änderungen neu übertragen.

---

## 6. CI/CD-Pipeline

### 6.1 Workflow-Übersicht
```
push auf main / feat/** / dev/**
  │
  ├── compile ──────────────────────────────────────────────┐
  │     ├── test (JUnit + JaCoCo-Report)                   │
  │     ├── checkstyle                                      │
  │     ├── pmd                       alle grün?            │
  │     └── spotbugs ──────────────────────────────► docker (nur dev/**)
  │                       fehlgeschlagen?
  │                              │
  │                         build-fixer (Auto-Fix)
  │
  └── docker (nur bei dev/**):
        mvn package → Fat JAR
        docker build (Layered Multi-Stage)
        docker push → ghcr.io/jkolzau-coder/student-grade-manager
```

### 6.2 Branches und Zwecke

| Branch | Zweck | Docker Push |
|---|---|---|
| `main` | Stabiler Stand | Nein |
| `feat/**` | Feature-Entwicklung | Nein |
| `dev/sb-docker` | Spring Boot + Docker-Integration | Ja |
| `dev/test-report` | JaCoCo + Testabdeckung | Ja |
| `test-setup/junit-lifecycle` | JUnit-Lifecycle-Baseline | Nein |
| `feat/build-agent` | BuildAgent-Implementierung | Nein |

### 6.3 Image-Registry

| Tag | Wann erstellt |
|---|---|
| `latest` | Bei jedem Push auf `dev/**` |
| `<commit-sha>` | Bei jedem Push auf `dev/**` |

```
ghcr.io/jkolzau-coder/student-grade-manager:latest
ghcr.io/jkolzau-coder/student-grade-manager:<sha>
```

---

## 7. Betrieb

### 7.1 Lokaler Workflow

```bash
# 1. Kompilieren
mvn compile -pl student-grade-manager-core -q

# 2. Tests + Coverage
mvn test -pl student-grade-manager-core

# 3. Alle Gates
mvn verify -pl student-grade-manager-core

# 4. Docker Image bauen
mvn package -pl student-grade-manager-core
docker build -t student-grade-manager student-grade-manager-core/

# 5. Container starten
docker run student-grade-manager

# 6. BuildAgent manuell
java scripts/BuildAgent.java
```

### 7.2 Shell-Hook aktivieren
```bash
source scripts/activate.sh
# ab jetzt: jeder mvn-Fehler → BuildAgent startet automatisch
mvn compile   # schlägt fehl → BuildAgent repariert → mvn compile läuft erneut
```

### 7.3 JaCoCo-Report lokal
```bash
mvn test -pl student-grade-manager-core
open student-grade-manager-core/target/site/jacoco/index.html
```

---

## 8. Fehlerquellen

### 8.1 Build-Probleme

| Problem | Ursache | Lösung |
|---|---|---|
| `getSubject()` nicht gefunden | Methode als `GetSubject()` (Großbuchstabe) — Checkstyle-Verletzung | Lowercase-Konvention, BuildAgent repariert automatisch |
| Interface-Methode fehlt | `GradeServiceImpl` implementiert `GradeService` nicht vollständig | Abstract-Fehler bei Compile — Methoden ergänzen |
| JaCoCo-Agent fehlt | Surefire `argLine` war statisch gesetzt | `@{argLine}` Late-Binding für JaCoCo-Agentpfad |
| `exec:java` kein stdin | Maven-JVM erbt kein Terminal-stdin | `exec:exec`-Goal forkt eigenen Prozess |

### 8.2 BugAgent-Artefakte

| Problem | Ursache | Lösung |
|---|---|---|
| Absichtlich lange Zeile in `Grade.java` | BugAgent-Test-Überrest vom Checkstyle-Demo | Manuell entfernt + `fix`-Commit |
| Unnötige Methoden-Kommentare | BugAgent fügte Validierungskommentare ein | CLAUDE.md: Kommentare nur wenn WHY nicht offensichtlich |
| `getGrades()` mutable List | Regression von `unmodifiableList` auf `new ArrayList<>()` | `Collections.unmodifiableList` wiederhergestellt |

### 8.3 CI/CD

| Problem | Ursache | Lösung |
|---|---|---|
| Checkstyle nicht im Lifecycle | Nur in `pluginManagement` — nicht in `<plugins>` | Execution in child-pom ergänzt |
| Duplikate Testklassen | BugAgent legte Tests in falsche Pakete | Nicht-committete Duplikate gelöscht |
| `git reset --hard` geblockt | Hook verhindert destruktive Operationen | `git rm` + Commit statt Reset |

---

## 9. Learnings

### 9.1 Architektur
- **Interface-first**: `GradeService` vor `GradeServiceImpl` — Mockito-Tests möglich ohne konkrete Implementierung
- **`pluginManagement` vs. `<plugins>`**: `pluginManagement` ist nur Template — Execution muss im Child-POM deklariert sein
- **`final`-Klassen**: verhindert SpotBugs `EI_EXPOSE_REP2` bei Referenzfeldern — `@BeforeClass` prüft Modifier in Tests
- **Spring Boot + Varargs**: `CommandLineRunner.run(String... args)` ist rückwärtskompatibel mit `run()` — bestehende Tests brauchen keine Anpassung

### 9.2 Qualitätssicherung
- **Assertions mit `-ea`**: JVM-Flag muss explizit gesetzt sein — `desiredAssertionStatus()` im Test verifiziert es
- **JaCoCo + Surefire**: `@{argLine}` Late-Binding notwendig — statische `argLine` überschreibt JaCoCo-Agentpfad
- **BugAgent-Artefakte**: Automatisch generierter Code enthält Relikte (Kommentare, Style-Verletzungen) — immer Review nach automatischer Reparatur
- **Testabdeckung `main()`**: Spring-Boot-`main()` bleibt in Unit-Tests unabgedeckt — ist akzeptabel, kein Handlungsbedarf

### 9.3 CI/CD & Docker
- **Layered JAR**: Trennung von Dependencies und Application-Code reduziert Docker-Push-Größe bei Code-Änderungen signifikant
- **`[skip ci]`-Tag**: verhindert endlose CI-Schleife nach Auto-Fix-Commit des BuildAgents
- **Cherry-Pick mit Kontext**: Commits können nur auf Branches übertragen werden, die denselben Code-Stand als Basis haben — abweichende Interfaces führen zu Konflikten
- **BuildAgent Rekursion**: Shell-Funktion in `activate.sh` vererbt sich nicht auf Subprozesse — kein rekursiver Aufruf möglich

---

## 10. Next Steps

### 10.1 Kurzfristig
- [ ] **`getAllStudents()`** in `GradeService`-Interface aufnehmen und in CI-Branches cherry-picken
- [ ] **JaCoCo-Schwellenwert**: Minimum-Coverage (z.B. 80 %) als Build-Gate definieren (`jacoco:check`)
- [ ] **`main()`-Abdeckung**: Spring Boot Integration-Test mit `@SpringBootTest` für vollständige Coverage
- [ ] **`activate.sh` als `.envrc`**: direnv-Integration für automatische Aktivierung beim Betreten des Verzeichnisses

### 10.2 Mittelfristig
- [ ] **Persistenz**: JSON- oder SQLite-Export der Notendaten über Session-Grenzen hinaus
- [ ] **REST-API**: Spring Boot Web Starter + Controller — Noten per HTTP verwalten
- [ ] **PR-Coverage-Kommentar**: `madrapps/jacoco-report` Action postet Coverage-Delta direkt in den PR
- [ ] **BuildAgent Lernschleife**: Fehlerbehebungen zurück ins Commit-Log schreiben — Muster für häufige Fehler erkennen

### 10.3 Langfristig
- [ ] **Multi-Modul**: separates `student-grade-manager-api`-Modul mit OpenAPI-Spec
- [ ] **Datenbankanbindung**: JPA + H2 (Test) / PostgreSQL (Produktion)
- [ ] **Authentifizierung**: Spring Security — Lehrende vs. Studierende
- [ ] **Docker Compose**: Anwendung + Datenbank als gemeinsarter Stack
- [ ] **Kubernetes-Deployment**: Helm Chart für reproduzierbare Deployments auf mehreren Umgebungen

---

## 11. Usability — Entwicklerperspektive

### 11.1 Nielsen Heuristiken auf das Projekt übertragen

| Heuristik | Anwendung im Projekt | Befund |
|---|---|---|
| 1 Systemstatus | Maven-Output zeigt Gate-Ergebnis sofort | Erfüllt |
| 2 Reale Welt | Deutsche Fehlermeldungen im BuildAgent (`Versuch`, `Fehlgeschlagenes Gate`) | Weitgehend erfüllt |
| 3 Nutzerkontrolle | `--abort` bei cherry-pick, `[skip ci]` verhindert Endlosschleife | Erfüllt |
| 4 Konsistenz | Konventionelle Commit-Messages durchgängig (`type(scope): subject`) | Erfüllt |
| 5 Fehlervermeidung | Enforcer blockiert `-DskipTests`, Assertions prüfen Map-Invariante | Erfüllt |
| 6 Wiedererkennung | `activate.sh` gibt klare Statusmeldung — kein versteckter Zustand | Erfüllt |
| 7 Flexibilität | `java scripts/BuildAgent.java 5` — Retry-Anzahl konfigurierbar | Erfüllt |
| 8 Minimalismus | Kein unnötiger Code — CLAUDE.md: Kommentare nur wenn WHY nicht offensichtlich | Weitgehend erfüllt |
| 9 Fehlermeldungen | Gate-Name + betroffene Dateien im BuildAgent-Prompt — Claude bekommt Kontext | Erfüllt |
| 10 Hilfe | CLAUDE.md, ARCHETYPE.md, README.md als Referenz | Erfüllt |
