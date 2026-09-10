#!/usr/bin/env bash

set -u
set -o pipefail

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

checkpoint="${1:-}"
[[ -n "$checkpoint" ]] || fail "Usage: restore-checkpoint.sh <checkpoint-id>"
python_command="${FOCUS_LAB_PYTHON:-python3}"
command -v "$python_command" >/dev/null 2>&1 || fail "python3 is required to read the checkpoint manifest."

script_dir="$(cd -- "$(dirname -- "$0")" && pwd)" || fail "Cannot resolve script directory."
repo_root="$(cd -- "$script_dir/../.." && pwd)" || fail "Cannot resolve repository root."
checkpoints_root="$repo_root/masterclass/checkpoints"
manifest_path="$checkpoints_root/manifest.json"
[[ -f "$manifest_path" ]] || fail "Checkpoint manifest not found: $manifest_path"

metadata_output="$("$python_command" - "$manifest_path" "$checkpoint" <<'PY'
import json
import pathlib
import sys

manifest_path = pathlib.Path(sys.argv[1]).resolve()
checkpoint_id = sys.argv[2]
repo_root = manifest_path.parents[2]
checkpoints_root = manifest_path.parent

try:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
except Exception as error:
    raise SystemExit(f"Cannot read checkpoint manifest: {error}")

matches = [item for item in manifest.get("checkpoints", []) if item.get("id") == checkpoint_id]
if len(matches) != 1:
    available = [item.get("id") for item in manifest.get("checkpoints", []) if item.get("status") == "available"]
    raise SystemExit(f"Unknown checkpoint '{checkpoint_id}'. Available: {', '.join(available)}")

entry = matches[0]
directory = entry.get("directory")
if entry.get("status") != "available" or not directory:
    raise SystemExit(f"Checkpoint '{checkpoint_id}' is planned but not available yet.")

snapshot_root = (checkpoints_root / directory).resolve()
try:
    snapshot_root.relative_to(checkpoints_root)
except ValueError:
    raise SystemExit("Checkpoint directory escapes the checkpoints root.")

student_files = manifest.get("studentFiles", [])
if not student_files:
    raise SystemExit("Checkpoint manifest contains no student files.")

for relative in student_files:
    if not isinstance(relative, str) or not relative:
        raise SystemExit("Checkpoint manifest contains an invalid student path.")
    for root in (repo_root, snapshot_root):
        try:
            (root / relative).resolve().relative_to(root)
        except ValueError:
            raise SystemExit(f"Path escapes its expected root: {relative}")

print(directory)
for relative in student_files:
    print(relative)
PY
)" || fail "Checkpoint metadata validation failed."

metadata=()
while IFS= read -r line; do
  line=${line%$'\r'}
  metadata+=("$line")
done <<< "$metadata_output"

[[ ${#metadata[@]} -gt 1 ]] || fail "Checkpoint manifest contains no restorable files."
checkpoint_directory="${metadata[0]}"
student_files=("${metadata[@]:1}")
snapshot_root="$checkpoints_root/$checkpoint_directory"

for relative in "${student_files[@]}"; do
  [[ -f "$snapshot_root/$relative" ]] || fail "Snapshot is incomplete; missing file: $snapshot_root/$relative"
done

timestamp="$(date '+%Y%m%d-%H%M%S')"
backup_root="$repo_root/.recovery/$timestamp-$checkpoint-$$"
mkdir -p -- "$backup_root" || fail "Cannot create backup directory: $backup_root"

for relative in "${student_files[@]}"; do
  destination="$repo_root/$relative"
  if [[ -f "$destination" ]]; then
    backup_path="$backup_root/$relative"
    mkdir -p -- "$(dirname -- "$backup_path")" || fail "Cannot create backup path for $relative"
    cp -p -- "$destination" "$backup_path" || fail "Cannot back up $relative"
  fi
done

for relative in "${student_files[@]}"; do
  source_path="$snapshot_root/$relative"
  destination="$repo_root/$relative"
  mkdir -p -- "$(dirname -- "$destination")" || fail "Cannot create destination path for $relative"
  cp -p -- "$source_path" "$destination" || fail "Cannot restore $relative"
done

printf "Restored checkpoint '%s'.\n" "$checkpoint"
printf 'Backup: %s\n' "$backup_root"

if ! (cd -- "$repo_root" && "$repo_root/gradlew" assembleDebug --offline); then
  fail "Offline debug build failed. Backup remains at $backup_root"
fi

printf 'Offline debug build passed.\n'
