-- ============================================================
-- v12：销售总监开放「经营概览」
-- 背景：v8 按「销售部不看公司大盘」把 dashboard:view 从销售角色收回，
--       但销售总监（SALES）要担销售目标，必须能看签单进度与团队业绩。
-- 处理：只给 SALES（总监/销售部负责人）补 dashboard:view；
--       销售组长（SALES_G1/G2）与销售顾问（SALES_REP）仍然只到「我的待办」。
-- 幂等：INSERT ... WHERE NOT EXISTS，可重复执行。
-- ============================================================

USE crm;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'dashboard:view'
WHERE r.code = 'SALES'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions x
    WHERE x.role_id = r.id AND x.permission_id = p.id
  );

-- 核对：dashboard:view 的授权角色
SELECT p.code AS permission, GROUP_CONCAT(r.code ORDER BY r.id) AS roles
FROM permissions p
LEFT JOIN role_permissions rp ON rp.permission_id = p.id
LEFT JOIN roles r ON r.id = rp.role_id
WHERE p.code = 'dashboard:view'
GROUP BY p.id;

-- 核对：各角色权限数量
SELECT r.code, COUNT(rp.permission_id) AS perms
FROM roles r
LEFT JOIN role_permissions rp ON rp.role_id = r.id
GROUP BY r.id
ORDER BY r.id;
