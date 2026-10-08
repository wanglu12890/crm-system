# CRM AI Agent V1 synthetic dataset

This utility generates deterministic development/demo data for Customer, Contact and FollowRecord. It never connects to MySQL, never deletes data and does not create users or departments.

`config.yaml` is JSON-formatted YAML 1.2 so the generator needs only Python's standard library. The configured five users and two departments must already match the development database.

## Generate and validate

From the repository root:

```powershell
python scripts/seed-ai-demo/generate.py
python scripts/seed-ai-demo/validate.py
```

Custom paths are supported:

```powershell
python scripts/seed-ai-demo/generate.py --config scripts/seed-ai-demo/config.yaml --output scripts/seed-ai-demo/output
```

Generation first validates exact counts, distributions, foreign-key relationships, primary contacts, ownership, regions and time boundaries. If validation fails, it does not write an importable SQL file.

## Import manually

1. Review `output/ai_demo_seed.sql` and run its user/department pre-check separately.
2. Confirm all five users are enabled, undeleted and assigned to the expected department.
3. Confirm the collision query returns three zeroes.
4. Only then execute the transaction in a development database:

```powershell
cmd /c "mysql -h localhost -P 3306 -u root -p crm_system < scripts\seed-ai-demo\output\ai_demo_seed.sql"
```

Do not put the password in the command, configuration or repository. The SQL is intentionally not idempotent: a repeated import fails on primary/unique keys instead of silently duplicating a dataset.

## Cleanup policy

There is no automatic cleanup command. After explicit review, use the exact IDs in `ai_demo_manifest.json` and delete in this order: `follow_record`, `contact`, `customer`. Never use broad ranges without first comparing them with the manifest.

All generated names, addresses, phone values and emails are synthetic. This dataset must not be used in production.
