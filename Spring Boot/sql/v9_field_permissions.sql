-- 字段级权限：成本 / 毛利对销售不可见
-- 功能权限决定「能进哪个页面」，数据权限决定「能看到谁的数据」，
-- 字段权限决定「同一条记录里哪些字段不能看」。本次补的是第三层：
--   product:cost:view  商品进价（成本价）
--   order:profit:view  订单成本、毛利额、毛利率
-- 销售组长与销售顾问只对成交负责，看不到进价与毛利；销售总监、财务、管理员保留。
-- 幂等：权限点与授权都只在缺失时插入，重复执行不会报错，也不会覆盖角色管理里的手工调整。

USE crm;

INSERT INTO permissions(code, name, module, type, sort_order)
SELECT 'product:cost:view', '查看商品进价', '销售管理', 'action', 221
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'product:cost:view');

INSERT INTO permissions(code, name, module, type, sort_order)
SELECT 'order:profit:view', '查看成本与毛利', '销售管理', 'action', 222
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'order:profit:view');

-- 管理员、财务、销售总监可见；组长 / 顾问不可见
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('product:cost:view', 'order:profit:view')
WHERE r.code IN ('ADMIN', 'FINANCE', 'SALES')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = r.id AND x.permission_id = p.id
  );

-- 兜底：确保销售组的两个角色没有被误授权（例如之前手工勾过）
DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code IN ('SALES_G1', 'SALES_G2')
  AND p.code IN ('product:cost:view', 'order:profit:view');

SELECT p.code, GROUP_CONCAT(r.code ORDER BY r.code) AS roles
FROM permissions p
LEFT JOIN role_permissions rp ON rp.permission_id = p.id
LEFT JOIN roles r ON r.id = rp.role_id
WHERE p.code IN ('product:cost:view', 'order:profit:view')
GROUP BY p.code;

-- 生产角色维护商品资料（新增 / 改价 / 库存），进价是他们录入的字段，保留可见；
-- 售后只维护技术资料，销售只报价成交，两者都不需要看到成本。
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'product:cost:view'
WHERE r.code = 'PRODUCTION'
  AND NOT EXISTS (SELECT 1 FROM role_permissions x WHERE x.role_id = r.id AND x.permission_id = p.id);
