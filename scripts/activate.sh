#!/usr/bin/env bash
# Einmalig pro Session sourcen: source scripts/activate.sh
# Danach greift der build-fixer automatisch bei jedem mvn-Fehler.

if [ -n "$ZSH_VERSION" ]; then
  _ACTIVATE_DIR="$(cd "$(dirname "${(%):-%x}")" && pwd)"
else
  _ACTIVATE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fi

mvn() {
  command mvn "$@"
  local exit_code=$?
  if [ $exit_code -ne 0 ]; then
    echo ""
    echo "=== mvn fehlgeschlagen — build-wrapper wird gestartet ==="
    java "$_ACTIVATE_DIR/BuildWrapper.java"
    local wrapper_exit=$?
    if [ $wrapper_exit -eq 0 ]; then
      echo ""
      echo "=== build-wrapper erfolgreich — Wiederhole: mvn $* ==="
      command mvn "$@"
      return $?
    fi
    return $wrapper_exit
  fi
  return $exit_code
}

echo "build-wrapper aktiviert. Alle mvn-Fehler werden automatisch behoben."
