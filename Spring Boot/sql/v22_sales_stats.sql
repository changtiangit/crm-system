-- ============================================================
-- v22 销售总监补统计权限
-- 现象：张明（SALES / 全公司范围）能看经营概览的业绩排行，点进去却是 403，
--       因为 SALES 角色没被授予 stats:sales:view / stats:customer:view。
-- 范围：只影响 SALES（当前仅销售总监），组长与顾问是 SALES_G1 / SALES_G2，不受影响。
-- ============================================================

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('stats:sales:view', 'stats:customer:view')
WHERE r.code = 'SALES';

SELECT r.code AS 角色, GROUP_CONCAT(p.code ORDER BY p.code) AS 统计权限
FROM roles r
JOIN role_permissions rp ON rp.role_id = r.id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code = 'SALES' AND p.code LIKE 'stats:%'
GROUP BY r.code;
