-- Read-only acceptance queries for crm_ai_agent_v1. Run only after an authorized import.
-- Fixed batch IDs are contiguous and exactly match ai_demo_manifest.json.
USE crm_system;

SELECT COUNT(*) AS batch_customers
FROM customer WHERE id BETWEEN 2308100000000000001 AND 2308100000000000300;
SELECT COUNT(*) AS batch_contacts
FROM contact WHERE id BETWEEN 2308101000000000001 AND 2308101000000000450;
SELECT COUNT(*) AS batch_follow_records
FROM follow_record WHERE id BETWEEN 2308102000000000001 AND 2308102000000001200;

SELECT customer_level, COUNT(*) count
FROM customer WHERE id BETWEEN 2308100000000000001 AND 2308100000000000300
GROUP BY customer_level ORDER BY customer_level;
SELECT customer_type, COUNT(*) count
FROM customer WHERE id BETWEEN 2308100000000000001 AND 2308100000000000300
GROUP BY customer_type ORDER BY customer_type;
SELECT status, COUNT(*) count
FROM customer WHERE id BETWEEN 2308100000000000001 AND 2308100000000000300
GROUP BY status ORDER BY status;
SELECT industry, COUNT(*) count
FROM customer WHERE id BETWEEN 2308100000000000001 AND 2308100000000000300
GROUP BY industry ORDER BY count DESC, industry;
SELECT source, COUNT(*) count
FROM customer WHERE id BETWEEN 2308100000000000001 AND 2308100000000000300
GROUP BY source ORDER BY count DESC, source;
SELECT province, city, COUNT(*) count
FROM customer WHERE id BETWEEN 2308100000000000001 AND 2308100000000000300
GROUP BY province, city ORDER BY province, city;

SELECT u.username, d.dept_code, COUNT(*) count
FROM customer c
LEFT JOIN sys_user u ON u.id=c.owner_id
LEFT JOIN sys_department d ON d.id=u.dept_id
WHERE c.id BETWEEN 2308100000000000001 AND 2308100000000000300
GROUP BY u.username, d.dept_code ORDER BY u.username;

SELECT COUNT(*) AS invalid_contact_customer
FROM contact ct LEFT JOIN customer c ON c.id=ct.customer_id
WHERE ct.id BETWEEN 2308101000000000001 AND 2308101000000000450 AND c.id IS NULL;
SELECT COUNT(*) AS multiple_primary_customer
FROM (
  SELECT customer_id FROM contact
  WHERE id BETWEEN 2308101000000000001 AND 2308101000000000450 AND is_primary=1
  GROUP BY customer_id HAVING COUNT(*) > 1
) invalid;
SELECT COUNT(*) AS invalid_follow_relation
FROM follow_record f
LEFT JOIN customer c ON c.id=f.target_id
LEFT JOIN contact ct ON ct.id=f.contact_id
WHERE f.id BETWEEN 2308102000000000001 AND 2308102000000001200
  AND (f.target_type <> 'CUSTOMER' OR c.id IS NULL OR (f.contact_id IS NOT NULL AND ct.customer_id <> f.target_id));
SELECT COUNT(*) AS future_or_pre_customer_follow
FROM follow_record f JOIN customer c ON c.id=f.target_id
WHERE f.id BETWEEN 2308102000000000001 AND 2308102000000001200
  AND (f.follow_at < c.created_at OR f.follow_at >= '2026-10-01 00:00:00');

SELECT COUNT(*) AS no_follow_last_30_days
FROM customer c LEFT JOIN (
  SELECT target_id, MAX(follow_at) last_follow_at FROM follow_record
  WHERE id BETWEEN 2308102000000000001 AND 2308102000000001200 GROUP BY target_id
) f ON f.target_id=c.id
WHERE c.id BETWEEN 2308100000000000001 AND 2308100000000000300
  AND (f.last_follow_at IS NULL OR f.last_follow_at < '2026-09-01 00:00:00');
SELECT COUNT(*) AS a_level_no_follow_last_30_days
FROM customer c LEFT JOIN (
  SELECT target_id, MAX(follow_at) last_follow_at FROM follow_record
  WHERE id BETWEEN 2308102000000000001 AND 2308102000000001200 GROUP BY target_id
) f ON f.target_id=c.id
WHERE c.id BETWEEN 2308100000000000001 AND 2308100000000000300
  AND c.customer_level='A'
  AND (f.last_follow_at IS NULL OR f.last_follow_at < '2026-09-01 00:00:00');
SELECT COUNT(*) AS created_last_90_days
FROM customer
WHERE id BETWEEN 2308100000000000001 AND 2308100000000000300
  AND created_at >= '2026-07-03 00:00:00' AND created_at < '2026-10-01 00:00:00';
