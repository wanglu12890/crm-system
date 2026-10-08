#!/usr/bin/env python3
"""Offline safety and integrity gate for the generated CRM AI demo SQL."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

from generate import DEFAULT_CONFIG, DEFAULT_OUTPUT, build_dataset, load_config, validate_dataset

FORBIDDEN = re.compile(
    r"\b(DROP|TRUNCATE|DELETE|UPDATE|ALTER|REPLACE|INSERT\s+IGNORE|CREATE|LOAD\s+DATA|PREPARE|EXECUTE)\b",
    re.IGNORECASE,
)


def strip_line_comments(sql: str) -> str:
    return "\n".join(line for line in sql.splitlines() if not line.lstrip().startswith("--"))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    errors = validate_dataset(build_dataset(load_config(args.config)))
    manifest_path = args.output / "ai_demo_manifest.json"
    sql_path = args.output / "ai_demo_seed.sql"
    if not manifest_path.exists() or not sql_path.exists():
        errors.append("manifest or SQL artifact is missing")
        manifest: dict = {}
        sql = ""
    else:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        sql = sql_path.read_text(encoding="utf-8")

    config = load_config(args.config)
    expected_counts = {
        "customers": config["counts"]["customers"],
        "contacts": config["counts"]["contacts"],
        "followRecords": config["counts"]["follow_records"],
    }
    if manifest.get("dataset") != config.get("dataset"):
        errors.append("config and manifest dataset metadata differ")
    if manifest.get("counts") != expected_counts:
        errors.append("config and manifest counts differ")

    recorded_hash = manifest.get("sha256", {}).get("ai_demo_seed.sql")
    actual_hash = hashlib.sha256(sql.encode("utf-8")).hexdigest() if sql else None
    if actual_hash != recorded_hash:
        errors.append("SQL SHA-256 differs from manifest")

    cleaned = strip_line_comments(sql)
    if FORBIDDEN.search(cleaned):
        errors.append("SQL contains a forbidden write/schema/client operation")
    if re.search(r"FOREIGN_KEY_CHECKS", cleaned, re.IGNORECASE):
        errors.append("SQL changes FOREIGN_KEY_CHECKS")
    statements = [part.strip() for part in cleaned.split(";") if part.strip()]
    allowed_statement = re.compile(
        r"^(USE\s+|SET\s+NAMES\s+|SELECT\b|START\s+TRANSACTION\b|"
        r"INSERT\s+INTO\s+(CUSTOMER|CONTACT|FOLLOW_RECORD)\b|COMMIT\b)",
        re.IGNORECASE,
    )
    unexpected = [statement[:80] for statement in statements if not allowed_statement.match(statement)]
    if unexpected:
        errors.append(f"unexpected SQL statement(s): {unexpected}")
    insert_targets = re.findall(r"\bINSERT\s+INTO\s+([a-z_]+)", cleaned, re.IGNORECASE)
    if insert_targets != ["customer", "contact", "follow_record"]:
        errors.append(f"unexpected INSERT order/targets: {insert_targets}")

    result = {
        "result": "PASS" if not errors else "FAIL",
        "sqlSha256": actual_hash,
        "counts": expected_counts,
        "statementCount": len(statements),
        "insertTargets": insert_targets,
        "errors": errors,
    }
    print(json.dumps(result, ensure_ascii=False, indent=2) if args.json else result["result"])
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
