#!/usr/bin/env python3
"""Automated build-fixer using the Anthropic API with tool use."""

import os
import re
import subprocess
import sys
from pathlib import Path

try:
    import anthropic
except ImportError:
    print("ERROR: anthropic package not installed. Run: pip install anthropic")
    sys.exit(1)

PROJECT_ROOT = Path(__file__).parent.parent.resolve()
MODULE = "student-grade-manager-core"
MAX_RETRIES = int(sys.argv[1]) if len(sys.argv) > 1 else 3
MODEL = "claude-sonnet-4-6"

TOOLS = [
    {
        "name": "read_file",
        "description": "Read the full content of a source file.",
        "input_schema": {
            "type": "object",
            "properties": {
                "path": {"type": "string", "description": "Absolute path to the file."}
            },
            "required": ["path"],
        },
    },
    {
        "name": "write_file",
        "description": "Overwrite a source file with corrected content. "
                       "Only src/main/java and src/test/java paths are allowed.",
        "input_schema": {
            "type": "object",
            "properties": {
                "path": {"type": "string", "description": "Absolute path to the file."},
                "content": {"type": "string", "description": "Complete new file content."},
            },
            "required": ["path", "content"],
        },
    },
    {
        "name": "run_command",
        "description": "Execute a shell command in the project root and return stdout+stderr.",
        "input_schema": {
            "type": "object",
            "properties": {
                "command": {"type": "string", "description": "Shell command to run."}
            },
            "required": ["command"],
        },
    },
]


def execute_tool(name: str, inputs: dict) -> str:
    if name == "read_file":
        path = Path(inputs["path"])
        if not path.exists():
            return f"ERROR: file not found: {path}"
        return path.read_text(encoding="utf-8")

    if name == "write_file":
        path = Path(inputs["path"])
        allowed = ("src/main/java", "src/test/java")
        if not any(a in str(path) for a in allowed):
            return f"ERROR: writes are only allowed in {allowed}"
        path.write_text(inputs["content"], encoding="utf-8")
        return f"OK: written {path}"

    if name == "run_command":
        result = subprocess.run(
            inputs["command"],
            shell=True,
            capture_output=True,
            text=True,
            cwd=str(PROJECT_ROOT),
        )
        output = (result.stdout + result.stderr).strip()
        return output[:5000]

    return f"ERROR: unknown tool '{name}'"


def run_build() -> tuple[bool, str]:
    """Run mvn verify and checkstyle. Returns (success, combined output)."""
    for cmd in [f"mvn verify -pl {MODULE}", f"mvn checkstyle:check -pl {MODULE}"]:
        result = subprocess.run(
            cmd, shell=True, capture_output=True, text=True, cwd=str(PROJECT_ROOT)
        )
        output = result.stdout + result.stderr
        if result.returncode != 0:
            return False, output
    return True, ""


def detect_gate(output: str) -> str:
    if "COMPILATION ERROR" in output:
        return "compile"
    if "maven-surefire-plugin" in output:
        return "surefire"
    if "maven-checkstyle-plugin" in output:
        return "checkstyle"
    if "maven-pmd-plugin" in output:
        return "pmd"
    if "spotbugs-maven-plugin" in output:
        return "spotbugs"
    return "unknown"


def extract_errors(output: str) -> str:
    lines = [
        line for line in output.splitlines()
        if "[ERROR]" in line or (
            "[WARNING]" in line and ".java" in line
        ) or "Tests run" in line
    ]
    return "\n".join(lines[:60])


def find_affected_files(output: str) -> list[str]:
    names = re.findall(r"[\w/]+\.java", output)
    found = set()
    for name in set(names):
        basename = Path(name).name
        for match in PROJECT_ROOT.rglob(basename):
            found.add(str(match))
    return sorted(found)


def run_agent_loop(gate: str, errors: str, files: list[str]) -> None:
    if not os.environ.get("ANTHROPIC_API_KEY"):
        print("ERROR: ANTHROPIC_API_KEY environment variable not set.")
        sys.exit(1)

    client = anthropic.Anthropic()

    system = (
        "You are an automated Java build-fixer. Use the provided tools to read "
        "affected source files, diagnose the build failure, and write corrected files. "
        "Only write files inside src/main/java or src/test/java. "
        "Never modify pom.xml or build configuration. "
        "After writing fixes, run the failed gate command to confirm the fix works."
    )

    user_content = (
        f"Fix this Maven build failure.\n\n"
        f"Failed gate : {gate}\n"
        f"Project root: {PROJECT_ROOT}\n\n"
        f"Error output:\n{errors}\n\n"
        f"Affected files:\n"
        + ("\n".join(files) if files else "None detected — search src/ tree")
        + "\n\nFix all violations so the next full build passes."
    )

    messages = [{"role": "user", "content": user_content}]

    while True:
        response = client.messages.create(
            model=MODEL,
            max_tokens=4096,
            system=system,
            tools=TOOLS,
            messages=messages,
        )

        messages.append({"role": "assistant", "content": response.content})

        if response.stop_reason == "end_turn":
            for block in response.content:
                if hasattr(block, "text") and block.text.strip():
                    print(block.text)
            break

        if response.stop_reason == "tool_use":
            tool_results = []
            for block in response.content:
                if block.type == "tool_use":
                    print(f"  tool: {block.name}  args: {str(block.input)[:120]}")
                    result = execute_tool(block.name, block.input)
                    tool_results.append({
                        "type": "tool_result",
                        "tool_use_id": block.id,
                        "content": result,
                    })
            messages.append({"role": "user", "content": tool_results})
            continue

        break


def main() -> None:
    print(f"=== build-fixer-api.py  model={MODEL}  max_retries={MAX_RETRIES} ===")

    for attempt in range(1, MAX_RETRIES + 1):
        print(f"\n--- Versuch {attempt} / {MAX_RETRIES} ---")

        success, output = run_build()
        if success:
            print(f"\nBuild erfolgreich nach {attempt} Versuch(en).")
            sys.exit(0)

        gate = detect_gate(output)
        errors = extract_errors(output)
        files = find_affected_files(output)

        print(f"Gate fehlgeschlagen : {gate}")
        print(f"Betroffene Dateien  : {len(files)}")
        print("Rufe Anthropic API auf ...")

        run_agent_loop(gate, errors, files)

    print(f"\nBuild nach {MAX_RETRIES} Versuchen immer noch fehlerhaft.")
    sys.exit(1)


if __name__ == "__main__":
    main()
