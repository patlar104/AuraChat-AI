#!/usr/bin/env python3

import json
import re
import sys


PROMPT_RULES = [
    {
        "skill": "$android-failure-state-design",
        "why": "failure-state modeling for loading, empty, error, retry, offline, and optimistic UI gaps",
        "patterns": [
            r"\bretry\b",
            r"\berror state\b",
            r"\bempty state\b",
            r"\bloading state\b",
            r"\bfailure state\b",
            r"\boffline\b",
            r"\boptimistic\b",
            r"\brollback\b",
            r"\bviewmodel state\b",
        ],
    },
    {
        "skill": "$kotlin-flow-orchestration",
        "why": "Flow and coroutine ownership, races, streaming handoff, and cancellation",
        "patterns": [
            r"\bstateflow\b",
            r"\bsharedflow\b",
            r"\bflow\b",
            r"\bcoroutine\b",
            r"\bstreaming\b",
            r"\brace\b",
            r"\bcancel(?:lation)?\b",
            r"\bemit\b",
            r"\bcollect(?:or)?\b",
            r"\broom\b",
        ],
    },
    {
        "skill": "$android-edge-case-testing",
        "why": "high-value regression tests for retries, navigation, streaming, and observer timing",
        "patterns": [
            r"\btest(?:ing|s)?\b",
            r"\bregression\b",
            r"\bedge case\b",
            r"\bunit test\b",
            r"\bcompose test\b",
            r"\bviewmodel test\b",
            r"\brobolectric\b",
        ],
    },
    {
        "skill": "$navigation-compose-correctness",
        "why": "Navigation Compose route contracts, one-shot events, and back stack control",
        "patterns": [
            r"\bnavigation compose\b",
            r"\bnavcontroller\b",
            r"\bback stack\b",
            r"\bsavedstatehandle\b",
            r"\broute\b",
            r"\bnav argument\b",
            r"\bdrawer\b",
            r"\btab(?:s)?\b",
        ],
    },
    {
        "skill": "$compose-streaming-performance",
        "why": "Compose recomposition, effect churn, auto-scroll jitter, and hot update paths",
        "patterns": [
            r"\bcompose\b",
            r"\brecomposition\b",
            r"\bjitter\b",
            r"\bscroll\b",
            r"\bperformance\b",
            r"\blag\b",
            r"\blaunchedeffect\b",
            r"\banimat(?:e|ion)\b",
        ],
    },
    {
        "skill": "$github-actions-permissions",
        "why": "GitHub Actions token scopes, workflow events, and least-privilege fixes",
        "patterns": [
            r"\bgithub actions\b",
            r"\bworkflow\b",
            r"\bgithub_token\b",
            r"\bpermissions\b",
            r"\bpull_request(?:_target)?\b",
            r"\bissue_comment\b",
            r"\bci\b",
            r"\bactions yml\b",
            r"\bworkflow yml\b",
        ],
    },
]


def json_out(event_name: str, context: str) -> None:
    payload = {
        "hookSpecificOutput": {
            "hookEventName": event_name,
            "additionalContext": context,
        }
    }
    json.dump(payload, sys.stdout)


def collect_prompt_matches(prompt: str):
    lowered = prompt.lower()
    matches = []
    for rule in PROMPT_RULES:
        if any(re.search(pattern, lowered) for pattern in rule["patterns"]):
            matches.append(rule)
    return matches[:3]


def prompt_context(prompt: str) -> str:
    matches = collect_prompt_matches(prompt)
    if not matches:
        return ""

    suggestions = "; ".join(
        f"{rule['skill']} for {rule['why']}" for rule in matches
    )
    return (
        "Relevant local skills for this request: "
        f"{suggestions}. Use only if directly relevant."
    )


def failure_context(tool_name: str, tool_input: dict, error: str) -> str:
    if tool_name != "Bash":
        return ""

    command = (tool_input or {}).get("command", "").lower()
    error_text = (error or "").lower()
    suggestions = []

    if any(token in command for token in ("gradlew", "./gradlew", "test", "connected", "robolectric")):
        suggestions.append(
            "$android-edge-case-testing for turning this failure mode into a focused regression test"
        )

    if any(re.search(pattern, command) for pattern in (r"\bflow\b", r"\bcoroutine\b", r"\broom\b")) or any(
        token in error_text for token in ("timeout", "cancellation", "race", "concurrent")
    ):
        suggestions.append(
            "$kotlin-flow-orchestration for analyzing timing, cancellation, and producer ownership"
        )

    if any(token in command for token in ("github", "gh ", ".github/workflows", "workflow")) or any(
        token in error_text for token in ("resource not accessible by integration", "permission", "forbidden")
    ):
        suggestions.append(
            "$github-actions-permissions for workflow token scope and event-model fixes"
        )

    if not suggestions:
        return ""

    return "Relevant local skill after this failure: " + "; ".join(suggestions[:2]) + "."


def main() -> int:
    data = json.load(sys.stdin)
    event_name = data.get("hook_event_name", "")

    if event_name == "UserPromptSubmit":
        context = prompt_context(data.get("prompt", ""))
        if context:
            json_out(event_name, context)
        return 0

    if event_name == "PostToolUseFailure":
        context = failure_context(
            data.get("tool_name", ""),
            data.get("tool_input", {}),
            data.get("error", ""),
        )
        if context:
            json_out(event_name, context)
        return 0

    return 0


if __name__ == "__main__":
    sys.exit(main())
