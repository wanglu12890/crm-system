#!/usr/bin/env python3
"""Generate deterministic synthetic CRM AI Agent V1 data without touching MySQL."""

from __future__ import annotations

import argparse
import hashlib
import json
import random
from collections import Counter, defaultdict
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parent
DEFAULT_CONFIG = ROOT / "config.yaml"
DEFAULT_OUTPUT = ROOT / "output"

LEVEL_COUNTS = {"A": 45, "B": 90, "C": 105, "D": 45, None: 15}
TYPE_COUNTS = {"ENTERPRISE": 255, "INDIVIDUAL": 45}
STATUS_COUNTS = {"POTENTIAL": 150, "ACTIVE": 105, "INACTIVE": 45}
INDUSTRY_COUNTS = {"制造业": 90, "信息技术": 65, "零售": 40, "建筑": 30, "物流": 25, "医疗": 20, "教育": 15, "金融": 10, "其他": 5}
SOURCE_COUNTS = {"销售开发": 90, "客户推荐": 60, "官网咨询": 45, "线上推广": 45, "电话营销": 30, "线下活动": 20, "其他": 10}
REGIONS = {
    ("广东省", "深圳市"): 20, ("广东省", "广州市"): 15, ("广东省", "佛山市"): 10, ("广东省", "东莞市"): 10,
    ("江苏省", "苏州市"): 20, ("江苏省", "南京市"): 15, ("江苏省", "无锡市"): 10,
    ("浙江省", "杭州市"): 20, ("浙江省", "宁波市"): 12, ("浙江省", "嘉兴市"): 8,
    ("湖南省", "长沙市"): 20, ("湖南省", "株洲市"): 8, ("湖南省", "岳阳市"): 7,
    ("上海市", "上海市"): 25, ("北京市", "北京市"): 25,
    ("四川省", "成都市"): 18, ("四川省", "绵阳市"): 7,
    ("湖北省", "武汉市"): 15, ("湖北省", "宜昌市"): 5,
    ("山东省", "青岛市"): 12, ("山东省", "济南市"): 8,
    ("福建省", "厦门市"): 6, ("福建省", "福州市"): 4,
}
COMPANY_WORDS = ["云帆", "启辰", "星瀚", "清越", "远拓", "新岭", "明川", "青禾", "博睿", "嘉木", "凌峰", "万象"]
COMPANY_SUFFIXES = ["科技", "智造", "商贸", "供应链", "实业", "信息服务", "产业发展", "企业服务"]
SURNAMES = ["赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈", "褚", "卫"]
GIVEN_NAMES = ["晨", "宁", "安", "言", "清", "禾", "远", "岚", "知夏", "星河", "嘉树", "明月"]


def load_config(path: Path) -> dict[str, Any]:
    # JSON is valid YAML 1.2 and keeps this utility dependency-free.
    return json.loads(path.read_text(encoding="utf-8"))


def expanded(counts: dict[Any, int]) -> list[Any]:
    return [value for value, count in counts.items() for _ in range(count)]


def shuffled(values: Iterable[Any], rng: random.Random) -> list[Any]:
    result = list(values)
    rng.shuffle(result)
    return result


def dt_text(value: datetime) -> str:
    return value.strftime("%Y-%m-%d %H:%M:%S.000")


def random_dt(rng: random.Random, start: datetime, end: datetime) -> datetime:
    if end <= start:
        return start
    return start + timedelta(seconds=rng.randrange(int((end - start).total_seconds())))


def sql_value(value: Any) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def make_industries(groups: list[str], rng: random.Random) -> list[str]:
    result: list[str | None] = [None] * len(groups)
    special = {
        "DEPT1": {"制造业": 55, "信息技术": 20},
        "DEPT2": {"制造业": 25, "信息技术": 40},
        "PUBLIC": {"制造业": 10, "信息技术": 5},
    }
    for group, plan in special.items():
        slots = shuffled([i for i, value in enumerate(groups) if value == group], rng)
        cursor = 0
        for industry, count in plan.items():
            for index in slots[cursor:cursor + count]:
                result[index] = industry
            cursor += count
    remaining = shuffled(expanded({k: v for k, v in INDUSTRY_COUNTS.items() if k not in ("制造业", "信息技术")}), rng)
    empty = [i for i, value in enumerate(result) if value is None]
    for index, industry in zip(empty, remaining, strict=True):
        result[index] = industry
    return [str(value) for value in result]


def build_dataset(config: dict[str, Any]) -> dict[str, Any]:
    rng = random.Random(config["dataset"]["seed"])
    as_of = datetime.fromisoformat(config["dataset"]["analysis_as_of"])
    users = config["users"]
    owner_names = shuffled([name for name, data in users.items() for _ in range(data["count"])] + [None] * config["public_pool_count"], rng)
    groups = ["PUBLIC" if name is None else ("DEPT1" if users[name]["dept_code"] == "SALES_DEPT_01" else "DEPT2") for name in owner_names]
    activities = [None] * 300
    private_indices = shuffled([i for i, name in enumerate(owner_names) if name], rng)
    cursor = 0
    for activity, count in (("HIGH", 60), ("NORMAL", 120), ("LOW", 60), ("LONG", 30)):
        for index in private_indices[cursor:cursor + count]:
            activities[index] = activity
        cursor += count
    for i, name in enumerate(owner_names):
        if name is None:
            activities[i] = "NEVER"

    # Guarantee at least twelve A-level private customers with stale follow-up while preserving exact totals.
    long_indices = [i for i, value in enumerate(activities) if value == "LONG"]
    levels: list[str | None] = [None] * 300
    for index in long_indices[:12]:
        levels[index] = "A"
    remaining_levels = shuffled(expanded({"A": 33, "B": 90, "C": 105, "D": 45, None: 15}), rng)
    for index, value in zip([i for i, value in enumerate(levels) if value is None], remaining_levels, strict=True):
        levels[index] = value

    types = shuffled(expanded(TYPE_COUNTS), rng)
    statuses = shuffled(expanded(STATUS_COUNTS), rng)
    industries = make_industries(groups, rng)
    sources = shuffled(expanded(SOURCE_COUNTS), rng)
    regions = shuffled(expanded(REGIONS), rng)

    # Exactly 100 customers are created in the last 90 days; stale categories remain old enough for their follow windows.
    recent_candidates = shuffled([i for i, value in enumerate(activities) if value in ("HIGH", "NORMAL")], rng)
    recent_set = set(recent_candidates[:100])
    created_times: list[datetime] = []
    for i, activity in enumerate(activities):
        if i in recent_set:
            created_times.append(random_dt(rng, datetime(2026, 7, 3), datetime(2026, 9, 25)))
        elif activity == "LONG":
            created_times.append(random_dt(rng, datetime(2025, 10, 1), datetime(2026, 3, 1)))
        elif activity == "LOW":
            created_times.append(random_dt(rng, datetime(2025, 10, 1), datetime(2026, 6, 15)))
        else:
            created_times.append(random_dt(rng, datetime(2025, 10, 1), datetime(2026, 7, 2)))

    customer_start = config["id_ranges"]["customer_start"]
    user_ids = [data["id"] for data in users.values()]
    customers: list[dict[str, Any]] = []
    for i in range(300):
        customer_id = customer_start + i
        owner_name = owner_names[i]
        owner_id = users[owner_name]["id"] if owner_name else None
        actor_id = owner_id or user_ids[i % len(user_ids)]
        province, city = regions[i]
        if types[i] == "ENTERPRISE":
            name = f"{city[:2]}{COMPANY_WORDS[i % len(COMPANY_WORDS)]}{COMPANY_SUFFIXES[(i // len(COMPANY_WORDS)) % len(COMPANY_SUFFIXES)]}演示有限公司"
        else:
            name = f"{SURNAMES[i % len(SURNAMES)]}{GIVEN_NAMES[(i * 5) % len(GIVEN_NAMES)]}（AI演示客户）"
        customers.append({
            "id": customer_id, "customer_no": f"KH{customer_id}", "customer_name": name,
            "customer_type": types[i], "customer_level": levels[i], "industry": industries[i], "source": sources[i],
            "phone": None if i % 11 == 0 else f"000-{i + 1:08d}",
            "email": None if i % 7 == 0 else f"customer{i + 1:03d}@example.com",
            "province": province, "city": city, "address": f"虚构示范路{i + 1}号（合成数据）",
            "owner_id": owner_id, "owner_name": owner_name, "group": groups[i], "activity": activities[i],
            "status": statuses[i], "remark": "CRM AI Agent V1 合成客户" if i % 3 == 0 else None,
            "created_by": actor_id, "updated_by": actor_id, "created_at": created_times[i], "updated_at": created_times[i],
            "deleted": 0, "version": 0,
        })
    enterprise_indices = [i for i, row in enumerate(customers) if row["customer_type"] == "ENTERPRISE"]
    customers[enterprise_indices[1]]["customer_name"] = customers[enterprise_indices[0]]["customer_name"]

    contact_counts = shuffled([0] * 30 + [1] * 135 + [2] * 90 + [3] * 45, rng)
    contact_start = config["id_ranges"]["contact_start"]
    contacts: list[dict[str, Any]] = []
    by_customer: dict[int, list[dict[str, Any]]] = defaultdict(list)
    for i, customer in enumerate(customers):
        for j in range(contact_counts[i]):
            contact_id = contact_start + len(contacts)
            created_at = customer["created_at"]
            contact = {
                "id": contact_id, "customer_id": customer["id"], "contact_name": f"联系人{i + 1:03d}-{j + 1}",
                "gender": (i + j) % 3, "position": ["负责人", "采购经理", "业务专员"][j],
                "mobile": f"000-1{i + 1:07d}", "telephone": None if j else f"000-2{i + 1:07d}",
                "email": f"contact{i + 1:03d}-{j + 1}@example.org", "is_primary": 1 if j == 0 else 0,
                "decision_role": ["DECISION", "INFLUENCER", "USER"][j], "birthday": None,
                "remark": "合成联系人", "created_by": customer["created_by"], "updated_by": customer["updated_by"],
                "created_at": created_at, "updated_at": created_at, "deleted": 0, "version": 0,
            }
            contacts.append(contact)
            by_customer[customer["id"]].append(contact)

    follow_start = config["id_ranges"]["follow_record_start"]
    follows: list[dict[str, Any]] = []
    follow_counts = {"HIGH": 8, "NORMAL": 4, "LOW": 3, "LONG": 2, "NEVER": 0}
    follow_windows = {
        "HIGH": (as_of - timedelta(days=29), as_of), "NORMAL": (as_of - timedelta(days=29), as_of),
        "LOW": (as_of - timedelta(days=89), as_of - timedelta(days=31)),
        "LONG": (datetime(2025, 10, 1), as_of - timedelta(days=91)),
    }
    follow_types = ["PHONE", "VISIT", "EMAIL", "IM", "OTHER"]
    contents = ["沟通当前采购计划", "确认业务需求和决策流程", "演示解决方案并收集反馈", "回访使用情况", "约定下一阶段沟通事项"]
    for customer_index, customer in enumerate(customers):
        activity = customer["activity"]
        if activity == "NEVER":
            continue
        start, end = follow_windows[activity]
        start = max(start, customer["created_at"])
        for j in range(follow_counts[activity]):
            follow_at = random_dt(rng, start, end)
            candidates = by_customer[customer["id"]]
            contact_id = candidates[j % len(candidates)]["id"] if candidates and j % 3 != 2 else None
            follows.append({
                "id": follow_start + len(follows), "target_type": "CUSTOMER", "target_id": customer["id"],
                "contact_id": contact_id, "follow_type": follow_types[(len(follows) + j) % len(follow_types)],
                "content": contents[(customer_index + j) % len(contents)] + "（合成记录）",
                "follow_at": follow_at, "next_follow_at": follow_at + timedelta(days=7) if j == follow_counts[activity] - 1 and (customer["id"] % 2 == 0) else None,
                "owner_id": customer["owner_id"], "created_by": customer["owner_id"], "updated_by": customer["owner_id"],
                "created_at": follow_at, "updated_at": follow_at, "deleted": 0, "version": 0,
            })
        customer["updated_at"] = max(item["follow_at"] for item in follows if item["target_id"] == customer["id"])
    return {"customers": customers, "contacts": contacts, "follows": follows, "config": config}


def validate_dataset(data: dict[str, Any]) -> list[str]:
    config = data["config"]
    customers, contacts, follows = data["customers"], data["contacts"], data["follows"]
    errors: list[str] = []
    expect = config["counts"]
    for label, actual, expected in (("customers", len(customers), expect["customers"]), ("contacts", len(contacts), expect["contacts"]), ("follow_records", len(follows), expect["follow_records"])):
        if actual != expected: errors.append(f"{label}: expected {expected}, got {actual}")
    for field, expected in (("customer_level", LEVEL_COUNTS), ("customer_type", TYPE_COUNTS), ("status", STATUS_COUNTS), ("industry", INDUSTRY_COUNTS), ("source", SOURCE_COUNTS)):
        actual = Counter(row[field] for row in customers)
        if actual != Counter(expected): errors.append(f"customer {field} distribution mismatch: {actual}")
    customer_ids = {row["id"] for row in customers}
    contact_ids = {row["id"] for row in contacts}
    if len(customer_ids) != len(customers) or len({row["customer_no"] for row in customers}) != len(customers): errors.append("customer IDs or numbers are not unique")
    if len(contact_ids) != len(contacts) or len({row["id"] for row in follows}) != len(follows): errors.append("child IDs are not unique")
    if any(row["customer_id"] not in customer_ids for row in contacts): errors.append("contact references missing customer")
    contact_customer = {row["id"]: row["customer_id"] for row in contacts}
    if any(row["target_id"] not in customer_ids or (row["contact_id"] is not None and contact_customer.get(row["contact_id"]) != row["target_id"]) for row in follows): errors.append("follow relationship mismatch")
    primary_counts = Counter(row["customer_id"] for row in contacts if row["is_primary"] == 1)
    if any(count > 1 for count in primary_counts.values()): errors.append("more than one primary contact")
    as_of = datetime.fromisoformat(config["dataset"]["analysis_as_of"])
    created = {row["id"]: row["created_at"] for row in customers}
    if any(row["follow_at"] < created[row["target_id"]] or row["follow_at"] >= as_of for row in follows): errors.append("invalid follow time")
    if Counter(row["owner_name"] for row in customers) != Counter({**{name: value["count"] for name, value in config["users"].items()}, None: config["public_pool_count"]}): errors.append("ownership distribution mismatch")
    if Counter((row["province"], row["city"]) for row in customers) != Counter(REGIONS): errors.append("region distribution mismatch")
    if sum(1 for row in follows if next(c for c in customers if c["id"] == row["target_id"])["owner_id"] is None): errors.append("public customer has follow records")
    if Counter(row["activity"] for row in customers) != Counter({"HIGH": 60, "NORMAL": 120, "LOW": 60, "LONG": 30, "NEVER": 30}): errors.append("follow activity distribution mismatch")
    if Counter(sum(1 for contact in contacts if contact["customer_id"] == row["id"]) for row in customers) != Counter({0: 30, 1: 135, 2: 90, 3: 45}): errors.append("contact count distribution mismatch")
    dept1_industry = Counter(row["industry"] for row in customers if row["group"] == "DEPT1")
    dept2_industry = Counter(row["industry"] for row in customers if row["group"] == "DEPT2")
    if dept1_industry["制造业"] <= dept2_industry["制造业"] or dept2_industry["信息技术"] <= dept1_industry["信息技术"]: errors.append("department industry scenario mismatch")
    if len({row["customer_name"] for row in customers}) == len(customers): errors.append("duplicate-name boundary scenario is missing")
    if not any(row["phone"] is None for row in customers) or not any(row["email"] is None for row in customers): errors.append("nullable contact field scenarios are missing")
    if any(not datetime(2025, 10, 1) <= row["created_at"] < as_of for row in customers): errors.append("customer creation time out of range")
    return errors


def rows_sql(table: str, columns: list[str], rows: list[dict[str, Any]]) -> str:
    values = []
    for row in rows:
        rendered = []
        for column in columns:
            value = row[column]
            if isinstance(value, datetime): value = dt_text(value)
            rendered.append(sql_value(value))
        values.append("(" + ", ".join(rendered) + ")")
    return f"INSERT INTO {table} (\n    " + ", ".join(columns) + "\n) VALUES\n" + ",\n".join(values) + ";\n"


def build_sql(data: dict[str, Any]) -> str:
    config = data["config"]
    users = config["users"]
    c_start, ct_start, f_start = (config["id_ranges"][key] for key in ("customer_start", "contact_start", "follow_record_start"))
    precheck = " UNION ALL\n".join(f"SELECT {v['id']} AS user_id, {sql_value(k)} AS username, {sql_value(v['role_code'])} AS role_code, {v['dept_id']} AS dept_id, {sql_value(v['dept_code'])} AS dept_code" for k, v in users.items())
    header = f"""-- CRM AI Agent V1 synthetic dataset. DEVELOPMENT/DEMO ONLY. Never use in production.
-- Dataset: {config['dataset']['name']}; seed={config['dataset']['seed']}; analysis_as_of={config['dataset']['analysis_as_of']}
-- Generated offline. This file never creates users or departments and never deletes existing data.
USE {config['dataset']['database']};
SET NAMES utf8mb4;

-- PRE-CHECK: must return five fully matching enabled users/departments. STOP on any mismatch.
SELECT e.*, u.id AS actual_user_id, u.username AS actual_username, u.dept_id AS actual_dept_id,
       u.status AS user_status, u.deleted AS user_deleted, d.dept_code AS actual_dept_code,
       CASE WHEN ur.user_id IS NOT NULL THEN r.role_code END AS actual_role_code,
       CASE WHEN ur.user_id IS NOT NULL THEN r.data_scope END AS actual_data_scope
FROM ({precheck}) e
LEFT JOIN sys_user u ON u.id=e.user_id AND u.username=e.username
LEFT JOIN sys_department d ON d.id=u.dept_id AND d.dept_code=e.dept_code AND d.status=1 AND d.deleted=0
LEFT JOIN sys_role r ON r.role_code=e.role_code AND r.status=1 AND r.deleted=0
LEFT JOIN sys_user_role ur ON ur.user_id=u.id AND ur.role_id=r.id
ORDER BY e.username;

-- Expected role scopes: SALES_MANAGER=DEPT, SALES_STAFF=SELF. STOP on mismatch.
SELECT role_code, data_scope, status, deleted FROM sys_role
WHERE role_code IN ('SALES_MANAGER', 'SALES_STAFF') ORDER BY role_code;

-- COLLISION CHECK: all counts must be zero before import. Duplicate imports also fail unique keys.
SELECT
 (SELECT COUNT(*) FROM customer WHERE id BETWEEN {c_start} AND {c_start + 299} OR customer_no LIKE 'KH230810%') AS customer_collisions,
 (SELECT COUNT(*) FROM contact WHERE id BETWEEN {ct_start} AND {ct_start + 449}) AS contact_collisions,
 (SELECT COUNT(*) FROM follow_record WHERE id BETWEEN {f_start} AND {f_start + 1199}) AS follow_collisions;

START TRANSACTION;
"""
    customer_cols = ["id", "customer_no", "customer_name", "customer_type", "customer_level", "industry", "source", "phone", "email", "province", "city", "address", "owner_id", "status", "remark", "created_by", "updated_by", "created_at", "updated_at", "deleted", "version"]
    contact_cols = ["id", "customer_id", "contact_name", "gender", "position", "mobile", "telephone", "email", "is_primary", "decision_role", "birthday", "remark", "created_by", "updated_by", "created_at", "updated_at", "deleted", "version"]
    follow_cols = ["id", "target_type", "target_id", "contact_id", "follow_type", "content", "follow_at", "next_follow_at", "owner_id", "created_by", "updated_by", "created_at", "updated_at", "deleted", "version"]
    footer = f"""COMMIT;

-- Verification counts for this batch.
SELECT COUNT(*) AS customers FROM customer WHERE id BETWEEN {c_start} AND {c_start + 299};
SELECT COUNT(*) AS contacts FROM contact WHERE id BETWEEN {ct_start} AND {ct_start + 449};
SELECT COUNT(*) AS follow_records FROM follow_record WHERE id BETWEEN {f_start} AND {f_start + 1199};
-- Cleanup is intentionally NOT included. Use manifest IDs and delete child-to-parent only after explicit review.
"""
    return header + rows_sql("customer", customer_cols, data["customers"]) + "\n" + rows_sql("contact", contact_cols, data["contacts"]) + "\n" + rows_sql("follow_record", follow_cols, data["follows"]) + "\n" + footer


def build_answers(data: dict[str, Any]) -> dict[str, Any]:
    customers, follows = data["customers"], data["follows"]
    as_of = datetime.fromisoformat(data["config"]["dataset"]["analysis_as_of"])
    latest: dict[int, datetime] = {}
    for row in follows: latest[row["target_id"]] = max(latest.get(row["target_id"], datetime.min), row["follow_at"])
    stale = [row for row in customers if row["id"] not in latest or latest[row["id"]] < as_of - timedelta(days=30)]
    province_counts = Counter(row["province"] for row in customers)
    south_china = {"广东省", "广西壮族自治区", "海南省", "香港特别行政区", "澳门特别行政区"}
    important_stale_south = [row for row in stale if row["province"] in south_china and row["customer_level"] in ("A", "B")]
    manufacturing_cities = Counter(row["city"] for row in customers if row["industry"] == "制造业")
    dept_regions = {
        dept: dict(sorted(Counter(row["province"] for row in customers if row["group"] == group).items()))
        for dept, group in (("SALES_DEPT_01", "DEPT1"), ("SALES_DEPT_02", "DEPT2"))
    }
    return {
        "dataset": data["config"]["dataset"]["name"], "analysisAsOf": data["config"]["dataset"]["analysis_as_of"],
        "timeBoundary": "left-closed/right-open; follow_at < analysis_as_of",
        "answers": {
            "customerTotal": len(customers), "publicPoolCustomers": sum(row["owner_id"] is None for row in customers),
            "levels": {str(k) if k is not None else "UNRATED": v for k, v in Counter(row["customer_level"] for row in customers).items()},
            "privateCustomersByDepartment": {"SALES_DEPT_01": sum(row["group"] == "DEPT1" for row in customers), "SALES_DEPT_02": sum(row["group"] == "DEPT2" for row in customers)},
            "customersByOwner": dict(sorted(Counter(row["owner_name"] for row in customers if row["owner_name"]).items())),
            "largestIndustry": Counter(row["industry"] for row in customers).most_common(1)[0],
            "customersWithoutFollowInLast30Days": len(stale),
            "aLevelWithoutFollowInLast30Days": sum(row["customer_level"] == "A" for row in stale),
            "customersCreatedInLast90Days": sum(as_of - timedelta(days=90) <= row["created_at"] < as_of for row in customers),
            "neverFollowedCustomerNos": [row["customer_no"] for row in customers if row["id"] not in latest],
            "visiblePrivateCounts": {"SUPER_ADMIN": 270, "SYSTEM_ADMIN": 270, "sales_manager01": 135, "sales_manager02": 135, "sales_staff01": 55, "sales_staff02": 45, "sales_staff03": 85},
            "visiblePublicPoolCounts": {name: 30 for name in ["SUPER_ADMIN", "SYSTEM_ADMIN", "sales_manager01", "sales_manager02", "sales_staff01", "sales_staff02", "sales_staff03"]},
            "topProvince": province_counts.most_common(1)[0],
            "guangdongCities": dict(sorted(Counter(row["city"] for row in customers if row["province"] == "广东省").items())),
            "hunanALevelCustomers": [{"customerNo": row["customer_no"], "customerName": row["customer_name"]} for row in customers if row["province"] == "湖南省" and row["customer_level"] == "A"],
            "manufacturingCustomersByCity": dict(sorted(manufacturing_cities.items(), key=lambda item: (-item[1], item[0]))),
            "departmentProvinceCoverage": dept_regions,
            "southChinaImportantCustomersWithoutFollowInLast30Days": [{"customerNo": row["customer_no"], "customerName": row["customer_name"], "level": row["customer_level"], "province": row["province"], "city": row["city"]} for row in important_stale_south],
        },
        "definitions": {"southChina": sorted(south_china), "importantCustomerLevels": ["A", "B"], "staleThreshold": "latest follow_at < 2026-09-01T00:00:00 or never followed"},
        "scopeNote": "Counts cover this synthetic batch only. Private list excludes public-pool customers; public-pool counts require the dedicated pool permission.",
    }


def report(data: dict[str, Any], answers: dict[str, Any]) -> str:
    customers = data["customers"]
    def table(title: str, counter: Counter[Any]) -> str:
        lines = [f"## {title}", "", "| Value | Count |", "|---|---:|"]
        lines += [f"| {('UNRATED' if key is None else key)} | {value} |" for key, value in sorted(counter.items(), key=lambda item: str(item[0]))]
        return "\n".join(lines)
    return "\n\n".join([
        "# CRM AI Agent V1 Synthetic Data Report",
        f"Dataset: `{data['config']['dataset']['name']}`  \nSeed: `{data['config']['dataset']['seed']}`  \nAnalysis as of: `{data['config']['dataset']['analysis_as_of']}`  \nAll names, addresses and contact details are synthetic.",
        table("Ownership", Counter(row["owner_name"] or "PUBLIC_POOL" for row in customers)),
        table("Level", Counter(row["customer_level"] for row in customers)),
        table("Type", Counter(row["customer_type"] for row in customers)),
        table("Status", Counter(row["status"] for row in customers)),
        table("Industry", Counter(row["industry"] for row in customers)),
        table("Source", Counter(row["source"] for row in customers)),
        table("Province", Counter(row["province"] for row in customers)),
        table("Industry / Province cross distribution", Counter(f"{row['industry']} / {row['province']}" for row in customers)),
        table("Sales department / Province coverage", Counter(f"{row['group']} / {row['province']}" for row in customers if row["group"] != "PUBLIC")),
        "## Key deterministic answers\n\n```json\n" + json.dumps(answers["answers"], ensure_ascii=False, indent=2) + "\n```",
    ]) + "\n"


def write_outputs(data: dict[str, Any], output: Path) -> None:
    output.mkdir(parents=True, exist_ok=True)
    sql = build_sql(data)
    answers = build_answers(data)
    report_text = report(data, answers)
    files = {"ai_demo_seed.sql": sql, "data_report.md": report_text, "agent_reference_answers.json": json.dumps(answers, ensure_ascii=False, indent=2) + "\n"}
    for name, content in files.items(): (output / name).write_text(content, encoding="utf-8", newline="\n")
    manifest = {
        "dataset": data["config"]["dataset"],
        "generatedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
        "counts": {"customers": len(data["customers"]), "contacts": len(data["contacts"]), "followRecords": len(data["follows"])},
        "ids": {"customer": [row["id"] for row in data["customers"]], "contact": [row["id"] for row in data["contacts"]], "followRecord": [row["id"] for row in data["follows"]]},
        "sha256": {name: hashlib.sha256(content.encode("utf-8")).hexdigest() for name, content in files.items()},
    }
    (output / "ai_demo_manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()
    data = build_dataset(load_config(args.config))
    errors = validate_dataset(data)
    if errors:
        print("Validation failed; no importable output was written:")
        print("\n".join(f"- {error}" for error in errors))
        return 1
    write_outputs(data, args.output)
    print(f"Generated validated dataset in {args.output}: 300 customers, 450 contacts, 1200 follow records")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
