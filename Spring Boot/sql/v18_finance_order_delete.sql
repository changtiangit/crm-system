-- =====================================================================
-- v18 收回财务的「删除订单」权限
--
-- 财务需要改单（金额修正、状态跟进），但删除订单等于销毁合同凭证：
-- 订单一删，收款、应收、发货、工单全都挂空，账也对不上。
-- 作废请走「订单状态」，删除只留给管理员与销售总监。
--
-- 脚本可重复执行。
-- =====================================================================

USE crm;

DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code = 'FINANCE' AND p.code = 'order:delete';

-- 校验：各角色对订单的写权限
SELECT r.code, r.name,
       MAX(CASE WHEN p.code = 'order:add' THEN 1 ELSE 0 END)    AS can_add,
       MAX(CASE WHEN p.code = 'order:edit' THEN 1 ELSE 0 END)   AS can_edit,
       MAX(CASE WHEN p.code = 'order:delete' THEN 1 ELSE 0 END) AS can_delete
FROM roles r
LEFT JOIN role_permissions rp ON rp.role_id = r.id
LEFT JOIN permissions p ON p.id = rp.permission_id
WHERE r.code IN ('ADMIN', 'SALES', 'SALES_G1', 'SALES_G2', 'FINANCE', 'SERVICE', 'PRODUCTION')
GROUP BY r.id
ORDER BY r.id;
