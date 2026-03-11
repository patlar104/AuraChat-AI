#!/bin/sh
set -eu

HOOK_NAME="${1:-}"
if [ -z "$HOOK_NAME" ]; then
  echo "Hook runner error: missing hook script name." >&2
  exit 1
fi

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
PROJECT_ROOT="${CLAUDE_PROJECT_DIR:-${PWD:-}}"

if [ -z "$PROJECT_ROOT" ] || [ ! -d "$PROJECT_ROOT/.claude/hooks" ]; then
  PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)"
fi

TARGET="$PROJECT_ROOT/.claude/hooks/$HOOK_NAME"
if [ ! -f "$TARGET" ]; then
  echo "Hook runner error: could not resolve $HOOK_NAME. CLAUDE_PROJECT_DIR was not set and the resolved project root does not contain $TARGET." >&2
  exit 1
fi

export CLAUDE_PROJECT_DIR="$PROJECT_ROOT"
shift
exec python3 "$TARGET" "$@"
