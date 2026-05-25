# Student Grade Manager — Projektregeln

## Projektübersicht

Multi-Modul-Maven-Projekt zur Verwaltung von Studenten und Noten.

| Modul | Zweck |
|---|---|
| `student-grade-manager-core` | Kernlogik als ausführbares JAR |

Technische Details zu Struktur, Plugins und Build-Befehlen: siehe `ARCHETYPE.md`.

---

## Quick Reference

| Item | Value |
|------|-------|
| **Project** | student_grade_manager |
| **Language** | java |
| **Architecture** | layered |
| **Database** | none |
| **Main Branch** | main |
| **Test Command** | `mvn test -pl student-grade-manager-core` |
| **Lint Command** | `mvn checkstyle:check -pl student-grade-manager-core` |

---

## Fitness Gates — Pflichtprüfungen

Bevor eine Aufgabe als **erledigt** gilt, müssen alle Gates grün sein.
Reihenfolge einhalten — jedes Gate ist Voraussetzung für das nächste:

| Gate | Befehl | Ziel |
|---|---|---|
| Kompilierung | `mvn compile -pl student-grade-manager-core -q` | 0 Fehler — kein Code landet in der Codebasis, der nicht kompiliert |
| Tests | `mvn test -pl student-grade-manager-core` | 100 % der bestehenden Tests grün — neue Funktionalität braucht mindestens 1 neuen Test |
| Style | `mvn checkstyle:check -pl student-grade-manager-core` | 0 Verstöße gegen den definierten Coding-Standard |
| Statische Analyse | `mvn pmd:check -pl student-grade-manager-core` | 0 Fehler — ungenutzte Variablen, leere Blöcke, unnötige Casts |
| Bug-Erkennung | `mvn spotbugs:check -pl student-grade-manager-core` | 0 Bugs — NullPointer-Risiken, Ressourcen-Leaks, fehlerhafte Vergleiche |

Alle Gates auf einmal: `mvn verify -pl student-grade-manager-core`

---

## Hooks — Automatisierte Qualitätschecks

| Ereignis | Auslöser | Aktion |
|---|---|---|
| `PostToolUse` | Jede Quelldatei-Änderung | Sofortige Kompilierung — Syntaxfehler werden unmittelbar gemeldet |
| `Stop` | Claude beendet seinen Arbeitsdurchlauf | Vollständige Testsuite — verhindert, dass eine Aufgabe als fertig gilt, solange Tests rot sind |

---

## Skills — Wann welchen einsetzen

| Skill | Wann einsetzen |
|---|---|
| `/code-review` | Nach dem Implementieren neuer Methoden — prüft auf Duplikate, unnötige Komplexität |
| `/security-baseline` | Vor jedem Commit auf `main` — prüft auf Sicherheitslücken |
| `/skill-sync` | Regelmäßig — hält das cognitive-core Framework aktuell |
| `/session-resume` | Beim Start einer neuen Session — lädt Kontext der letzten Arbeit |

**Pflicht-Skills pro Workflow:**
- Neues Feature → `/code-review` nach Implementierung → `/security-baseline` vor Commit
- Bugfix → `/code-review` zur Verifikation
- Refactoring → `/code-review`

---

## Code Standards

- Google Style Guide: 2-Leerzeichen Einrückung, max. 100 Zeichen Zeilenlänge, öffnende Klammer gleiche Zeile
- Kein `System.out.println` — ausschließlich SLF4J + Logback
- Alle Klassen im Paket `de.student.grademanager`
- Git commits: `type(scope): subject` (conventional format, scopes: model service app test config)
- KEINE KI-/Tool-Referenzen in Commit-Messages

---

## Agenten-Profile

### Profil: Solution Architect

**Fokus:** Modulschnitt, Schnittstellendesign und Technologieentscheidungen

**Aufgaben:**
- Neue Funktionalität der richtigen Schicht zuordnen
- Abhängigkeiten zwischen Modulen bewerten und zirkuläre Abhängigkeiten verhindern
- Externe Schnittstellen hinter Interfaces kapseln, bevor Implementierungen entstehen
- Versionierungsstrategie und Release-Prozess pflegen

**Leitfragen:**
1. Welcher Schicht gehört diese Logik an?
2. Kann diese externe Abhängigkeit hinter einem Interface versteckt werden?
3. Ist die Entscheidung in 6 Monaten noch nachvollziehbar dokumentiert?

**Qualitätskriterien:**
- Kein Modul nutzt Klassen, die es nicht explizit als Abhängigkeit deklariert
- Interfaces sind vor Implementierungen definiert
- Architekturentscheidungen sind dokumentiert

**Typische Outputs:**
- Moduldiagramm mit Abhängigkeiten
- Interface-Definitionen vor der Implementierung
- Entscheidungsprotokoll mit Begründung

---

### Profil: Test Specialist

**Fokus:** Testabdeckung, Isolation und Grenzwertanalyse

**Aufgaben:**
- Für jede neue öffentliche Methode Testfälle definieren (Happy Path, Grenzwerte, Fehlerfälle)
- Mock-Strategie festlegen: externe Abhängigkeiten mocken, interne Logik direkt testen
- Sicherstellen dass Tests voneinander unabhängig laufen
- Integrationstests von Unit-Tests trennen

**Leitfragen:**
1. Braucht dieser Test externe Ressourcen (Netzwerk, Datenbank)? → Mock verwenden
2. Sind untere und obere Grenzwerte explizit getestet?
3. Ist der Testname ohne Kommentar verständlich?

**Qualitätskriterien:**
- Jede öffentliche Methode hat: 1 Happy-Path + 1 Grenzwert + 1 Fehlerfall
- Kein Test gibt Ausgaben auf der Konsole aus
- Die gesamte Testsuite läuft in unter 30 Sekunden
- Mocks nur für externe Abhängigkeiten, nicht für eigene Klassen

**Testfall-Schema:**

| Kategorie | Beschreibung |
|---|---|
| Happy Path | Normalfall mit erwarteten Eingaben |
| Grenzwert unten | Kleinstmöglicher gültiger Wert |
| Grenzwert oben | Größtmöglicher gültiger Wert |
| Ungültige Eingabe | null, leer, außerhalb des Wertebereichs |
| Fehlerfall | Netzwerkfehler, Timeout, leere Antwort |

---

## Erlaubte Ausnahmen

Wenn ein Fitness Gate aus einem triftigen Grund nicht erfüllt werden kann:

1. Begründung direkt im Code als Kommentar
2. Unterdrückungsannotation mit Begründung
3. Dokumentation im Commit-Message

Niemals ein Gate dauerhaft deaktivieren ohne Rücksprache.

## Agents

See `.claude/AGENTS_README.md` for the agent team documentation.

## Imported Rules

@import .claude/rules/java-conventions.md
@import .claude/rules/testing.md
