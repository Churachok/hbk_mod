#!/usr/bin/env bash
# Restore supplied skins and make unused head-overlay pixels transparent.
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
python3 "$project_root/scripts/prepare_entity_skin_overlays.py" "$@"
