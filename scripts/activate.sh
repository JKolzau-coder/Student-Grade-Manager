#!/usr/bin/env bash
# Einmalig pro Session sourcen: source scripts/activate.sh
# Danach greift der build-fixer automatisch bei jedem mvn-Fehler.

_ACTIVATE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mvn() {
  command mvn "$@"
  local exit_code=$?
  if [ $exit_code -ne 0 ]; then
    echo ""
    echo "=== mvn fehlgeschlagen — build-wrapper wird gestartet ==="
    java "$_ACTIVATE_DIR/BuildWrapper.java"
    return $?
  fi
  return $exit_code
}

echo "build-wrapper aktiviert. Alle mvn-Fehler werden automatisch behoben."
