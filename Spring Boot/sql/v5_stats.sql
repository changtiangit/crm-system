-- 数据统计模块（销售统计 / 客户采购分析 / 产品销售分析）
-- 同时把「退换货单」从销售管理归口到售后管理（模块名与排序一起改）
-- 幂等：重复执行只更新名称、模块与排序，不会产生重复权限

USE crm;

-- ---------------- 数据统计三项菜单权限 ----------------
INSERT INTO permissions (code, name, module, type, sort_order) VALUES
  ('stats:sales:view',    '查看销售统计',     '数据统计', 'menu', 0),
  ('stats:customer:view', '查看客户采购分析', '数据统计', 'menu', 0),
  ('stats:product:view',  '查看产品销售分析', '数据统计', 'menu', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type);

-- ---------------- 退换货归口到售后管理 ----------------
UPDATE permissions SET module = '售后管理' WHERE code LIKE 'return:%';

-- ---------------- 重新排组内顺序 ----------------
-- 销售管理：商机 → 报价单 → 销售订单 → 商品
-- 售后管理：售后工单 → 退换货单 → 客户回访
UPDATE permissions p
JOIN (
  SELECT code,
         ROW_NUMBER() OVER (
           PARTITION BY module
           ORDER BY CASE
             WHEN code LIKE 'dashboard:%'    THEN 10
             WHEN code LIKE 'todo:%'         THEN 20
             WHEN code LIKE 'stats:sales:%'  THEN 10
             WHEN code LIKE 'stats:customer:%' THEN 20
             WHEN code LIKE 'stats:product:%'  THEN 30
             WHEN code LIKE 'clue:%'         THEN 10
             WHEN code LIKE 'pool:%'         THEN 20
             WHEN code LIKE 'customer:%'     THEN 30
             WHEN code LIKE 'contact:%'      THEN 40
             WHEN code LIKE 'followup:%'     THEN 50
             WHEN code LIKE 'opportunity:%'  THEN 10
             WHEN code LIKE 'quote:%'        THEN 20
             WHEN code LIKE 'order:%'        THEN 30
             WHEN code LIKE 'product:%'      THEN 40
             WHEN code LIKE 'delivery:%'     THEN 10
             WHEN code LIKE 'receipt:%'      THEN 20
             WHEN code LIKE 'receivable:%'   THEN 30
             WHEN code LIKE 'ticket:%'       THEN 10
             WHEN code LIKE 'return:%'       THEN 20
             WHEN code LIKE 'revisit:%'      THEN 30
             WHEN code LIKE 'system:%'       THEN 10
             ELSE 90 END,
           sort_order, id
         ) AS rn
  FROM permissions
) t ON t.code = p.code
SET p.sort_order = CASE p.module
    WHEN '工作台'     THEN 10
    WHEN '数据统计'   THEN 50
    WHEN '客户管理'   THEN 100
    WHEN '销售管理'   THEN 200
    WHEN '交付与回款' THEN 300
    WHEN '售后管理'   THEN 400
    WHEN '设置'       THEN 500
    ELSE 900 END + t.rn;

-- ---------------- 按角色授权 ----------------
-- 销售部（含一组、二组）与营销：三张报表都看，销售自己复盘、营销看产品与客户结构
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'stats:sales:view', 'stats:customer:view', 'stats:product:view'
) WHERE r.code IN ('SALES', 'SALES_G1', 'SALES_G2', 'MARKET');

-- 财务：销售统计 + 客户采购分析（关心客户欠款与贡献结构），产品分析不涉及
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'stats:sales:view', 'stats:customer:view'
) WHERE r.code = 'FINANCE';

-- 售后：产品销售分析（看故障与退货集中在哪些品）
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'stats:product:view'
WHERE r.code = 'SERVICE';

-- 生产：产品销售分析（按销量与库存安排排产）
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'stats:product:view'
WHERE r.code = 'PRODUCTION';

-- 生产还要发货，需要看客户与收货联系人（只读，不含任何增删改）
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('customer:view', 'contact:view')
WHERE r.code = 'PRODUCTION';

-- ---------------- 补齐销售一组 / 二组的开单能力 ----------------
-- 组员要能自己开订单、录商机、建客户，否则只能在商机里看、下不了单；
-- 「删除客户 / 删除订单」这类高危操作仍只留给销售部与管理员。
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'order:view', 'order:add', 'order:edit',
  'opportunity:add', 'opportunity:edit',
  'customer:add', 'customer:edit'
) WHERE r.code IN ('SALES', 'SALES_G1', 'SALES_G2');

-- 管理员：全部
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';
