#!/usr/bin/env python3
"""Regenerate in memory and verify checked-in AI demo artifacts and hashes."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from generate import DEFAULT_CONFIG, DEFAULT_OUTPUT, build_dataset, load_config, validate_dataset


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()
    errors = validate_dataset(build_dataset(load_config(args.config)))
    manifest_path = args.output / "ai_demo_manifest.json"
    if not manifest_path.exists():
        errors.append("manifest is missing; run generate.py first")
    else:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        for name, expected in manifest.get("sha256", {}).items():
            path = args.output / name
            actual = hashlib.sha256(path.read_bytes()).hexdigest() if path.exists() else None
            if actual != expected: errors.append(f"artifact hash mismatch: {name}")
    if errors:
        print("Validation failed:\n" + "\n".join(f"- {error}" for error in errors))
        return 1
    print("Validation passed: distributions, relations, times and artifact hashes are valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
