#!/usr/bin/env bash
# Single entry point: ./mvnw [deploy|install|package]
# Runs BuildWrapper (auto-fix all gates), then executes the requested Maven goal.
GOAL="${1:-deploy}"
java "$(dirname "$0")/scripts/BuildWrapper.java" 3 "$GOAL"
