#!/usr/bin/env bash
set -uo pipefail

MAX_RETRIES=${1:-3}
MODULE="student-grade-manager-core"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP_OUT=$(mktemp)
trap 'rm -f "$TMP_OUT"' EXIT

detect_gate() {
  local out="$1"
  if grep -q "COMPILATION ERROR" "$out"; then
    echo "compile"
  elif grep -q "maven-surefire-plugin" "$out"; then
    echo "surefire"
  elif grep -q "maven-checkstyle-plugin" "$out"; then
    echo "checkstyle"
  elif grep -q "maven-pmd-plugin" "$out"; then
    echo "pmd"
  elif grep -q "spotbugs-maven-plugin" "$out"; then
    echo "spotbugs"
  else
    echo "unknown"
  fi
}

extract_errors() {
  local out="$1"
  grep -E "^\[ERROR\]|^\[WARNING\].*\.java|Tests run.*Failures|Tests run.*Error|COMPILATION ERROR" "$out" | head -60
}

find_affected_files() {
  local out="$1"
  grep -oE '[A-Za-z_/]+\.java' "$out" \
    | sort -u \
    | while IFS= read -r f; do
        find "$PROJECT_ROOT" -name "$(basename "$f")" -type f 2>/dev/null | head -1
      done \
    | sort -u \
    | grep -v "^$"
}

run_gates() {
  mvn verify -pl "$MODULE" 2>&1 | tee "$TMP_OUT"
  local exit1=${PIPESTATUS[0]}
  [ $exit1 -ne 0 ] && return $exit1

  mvn checkstyle:check -pl "$MODULE" 2>&1 | tee "$TMP_OUT"
  return ${PIPESTATUS[0]}
}

echo "=== build-fixer.sh — max $MAX_RETRIES Versuche ==="

for attempt in $(seq 1 "$MAX_RETRIES"); do
  echo ""
  echo "--- Versuch $attempt / $MAX_RETRIES ---"

  cd "$PROJECT_ROOT"

  if run_gates; then
    echo ""
    echo "Build erfolgreich nach $attempt Versuch(en)."
    exit 0
  fi

  GATE=$(detect_gate "$TMP_OUT")
  ERRORS=$(extract_errors "$TMP_OUT")
  FILES=$(find_affected_files "$TMP_OUT")

  echo "Fehlgeschlagenes Gate: $GATE"
  echo "Rufe Claude CLI auf..."

  PROMPT="Fix a Maven build failure in the student-grade-manager Java project.

Failed gate : $GATE
Attempt     : $attempt of $MAX_RETRIES
Project root: $PROJECT_ROOT

--- Maven error output ---
$ERRORS

--- Affected source files ---
$FILES

Instructions:
- Read the affected files, identify the root cause, and edit only source files
  under src/main/java or src/test/java.
- Do NOT modify pom.xml or any build configuration.
- After editing, verify with: mvn verify -pl $MODULE (and mvn checkstyle:check if gate was checkstyle).
- Fix all violations in one pass so the next build attempt succeeds."

  claude -p "$PROMPT" --allowedTools Edit,Read,Bash

done

echo ""
echo "Build nach $MAX_RETRIES Versuchen immer noch fehlerhaft."
exit 1
