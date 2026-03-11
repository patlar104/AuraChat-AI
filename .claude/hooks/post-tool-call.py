import hashlib
import json
import os
import sys
import tempfile
from datetime import datetime, timezone
from http.client import HTTPConnection, HTTPException
from pathlib import Path
import traceback
from contextlib import closing
from typing import Optional

WEBSERVER_HOST = "localhost"
WEBSERVER_ENDPOINT = "/api/provenance/call"
PORT_FILE_SUFFIX = "-provenance-port.txt"

class ProvenanceHookError(RuntimeError):
    pass


def get_project_root() -> str:
    claude_root = os.getenv("CLAUDE_PROJECT_DIR")
    if claude_root:
        return claude_root

    cwd = os.getenv("PWD")
    if cwd and (Path(cwd) / ".claude" / "hooks").is_dir():
        return cwd

    return str(Path(__file__).resolve().parents[2])

def http_request(method, host, port, location, *, body: Optional[bytes] = None, headers={}, timeout=None) -> bytes:
    with closing(HTTPConnection(host, port, timeout=timeout)) as connection:
        connection.request(method, location, body=body, headers=headers)

def get_server_port():
    claude_root = get_project_root()
    path_hash = hashlib.md5(claude_root.encode('utf-8')).hexdigest()
    port_file = Path(tempfile.gettempdir()) / (path_hash + PORT_FILE_SUFFIX)

    return int(port_file.read_text("utf-8").strip())


def send_diff_to_webserver(file_path, timestamp_ms):
    try:
        port = get_server_port()
    except FileNotFoundError as e:
        raise ProvenanceHookError(
            f"Could not determine API port: {e.filename} does not exist") from e
    except Exception as e:
        raise ProvenanceHookError("Could not determine API port") from e

    url = f"http://{WEBSERVER_HOST}:{port}{WEBSERVER_ENDPOINT}"

    try:
        payload = {"file_path": file_path, "timestamp": timestamp_ms}
        return http_request(
            "POST",
            WEBSERVER_HOST,
            port=port,
            location=WEBSERVER_ENDPOINT,
            body=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            headers={'Content-Type': 'application/json'},
            timeout=0.5
        )

    except (HTTPException, OSError, ConnectionError) as e:
        raise ProvenanceHookError(
            f"Network error while sending diff to {url}") from e
    except Exception as e:
        raise ProvenanceHookError(
            f"Unknown error while sending diff to {url}") from e


def normalize_path(value):
    if not isinstance(value, str) or not value.strip():
        return None

    path = Path(value)
    if not path.is_absolute():
        path = Path(get_project_root()) / path

    return str(path.resolve())


def extract_file_paths(tool_name, tool_input):
    candidate_paths = []

    if tool_name in ["Write", "Edit", "MultiEdit"]:
        candidate_paths.append(tool_input.get("file_path"))
    elif tool_name == "NotebookEdit":
        candidate_paths.append(tool_input.get("notebook_path"))
    else:
        candidate_paths.extend(tool_input.get("files", []))
        candidate_paths.append(tool_input.get("path"))
        candidate_paths.append(tool_input.get("filePath"))

    unique_paths = []
    seen = set()
    for value in candidate_paths:
        normalized = normalize_path(value)
        if normalized and normalized not in seen:
            unique_paths.append(normalized)
            seen.add(normalized)

    return unique_paths


def excepthook(type, value, traceback_):
    traceback.print_exception(type, value, traceback_, file=sys.stderr)
    sys.exit(1)


def main():
    data = json.load(sys.stdin)
    tool_name = data.get('tool_name', 'unknown')

    modification_tools = [
        "Write",
        "Edit",
        "MultiEdit",
        "NotebookEdit",
        "createFile",
        "editFiles",
        "replaceStringInFile",
    ]

    if tool_name in modification_tools:
        tool_input = data.get('tool_input', {})
        file_paths = extract_file_paths(tool_name, tool_input)
        timestamp_ms = int(datetime.now(timezone.utc).timestamp() * 1000)
        for file_path in file_paths:
            try:
                send_diff_to_webserver(file_path, timestamp_ms)
            except ProvenanceHookError:
                # Provenance server not running — skip gracefully
                pass

if __name__ == "__main__":
    sys.excepthook = excepthook
    sys.exit(main())
