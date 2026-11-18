-- =====================================================================
-- v17 销售岗位补「应收账款 · 查看」权限
--
-- 销售对回款是有责任的：总监要看全公司应收、组长要看本组、顾问要看自己那几单。
-- 原来应收只在财务手里，销售只能问财务要数，工作台上的「回款风险」面板
-- 也只能显示 ¥0（前端按权限把数据清了）。
--
-- 只加「查看」(receivable:view)：
--   · 核销 / 确认回款仍归财务（receivable:confirm）
--   · 收款记录（receipt:view）、开票仍归财务
-- 数据范围照旧生效：SALES → 全公司，SALES_G1/G2 + GROUP → 本组，SELF → 仅本人。
--
-- 脚本可重复执行。
-- =====================================================================

USE crm;

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'receivable:view'
WHERE r.code IN ('SALES', 'SALES_G1', 'SALES_G2');

-- 校验：各角色是否已具备应收查看权限
SELECT r.code, r.name,
       MAX(CASE WHEN p.code = 'receivable:view' THEN 1 ELSE 0 END) AS can_view_receivable,
       MAX(CASE WHEN p.code = 'receivable:confirm' THEN 1 ELSE 0 END) AS can_confirm
FROM roles r
LEFT JOIN role_permissions rp ON rp.role_id = r.id
LEFT JOIN permissions p ON p.id = rp.permission_id
WHERE r.code IN ('ADMIN', 'SALES', 'SALES_G1', 'SALES_G2', 'FINANCE', 'MARKET')
GROUP BY r.id
ORDER BY r.id;
