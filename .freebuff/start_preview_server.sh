#!/bin/bash
# Start a static file server for app/src/main/assets, fully detached (double-fork via Python).
# Usage: start_preview_server.sh <port> <logfile>
set -euo pipefail

PORT="${1:-8000}"
LOGFILE="${2:-/Users/chen/Desktop/XMBOX/.freebuff/preview.log}"
ROOT="/Users/chen/Desktop/XMBOX/app/src/main/assets"

python3 - "$PORT" "$LOGFILE" <<'PYEOF'
import os
import sys

port = int(sys.argv[1])
logfile = sys.argv[2]

# First fork: detach from the controlling shell.
if os.fork() > 0:
    os._exit(0)
os.setsid()
# Second fork: ensure we are not a session leader (so we never regain a tty).
if os.fork() > 0:
    os._exit(0)

# Redirect stdin/stdout/stderr away from the tool's pipes.
fd = os.open(os.devnull, os.O_RDONLY)
os.dup2(fd, 0)
log = os.open(logfile, os.O_WRONLY | os.O_CREAT | os.O_APPEND, 0o644)
os.dup2(log, 1)
os.dup2(log, 2)

os.execvp("python3", ["python3", "-m", "http.server", str(port), "--bind", "127.0.0.1", "--directory", "/Users/chen/Desktop/XMBOX/app/src/main/assets"])
PYEOF

echo "server launched"
