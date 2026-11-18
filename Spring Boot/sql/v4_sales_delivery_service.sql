-- 销售管理 / 交付与回款 / 售后管理 三个模块（报价单、退换货单、发货、收款、应收、售后工单、客户回访）
-- 幂等：重复执行只更新名称与排序，不会产生重复权限
-- 同时把权限页的分组名称与左侧菜单对齐：线索→客户管理，商机/订单/商品→销售管理，系统四项→设置

USE crm;

INSERT INTO permissions (code, name, module, type, sort_order) VALUES
  ('quote:view',        '查看报价单',       '销售管理',     'menu',   0),
  ('quote:add',         '新建报价单',       '销售管理',     'action', 0),
  ('quote:edit',        '编辑/审批报价单',  '销售管理',     'action', 0),
  ('quote:delete',      '删除报价单',       '销售管理',     'action', 0),
  ('return:view',       '查看退换货单',     '销售管理',     'menu',   0),
  ('return:add',        '申请退换货',       '销售管理',     'action', 0),
  ('return:edit',       '处理退换货',       '销售管理',     'action', 0),
  ('delivery:view',     '查看发货记录',     '交付与回款',   'menu',   0),
  ('delivery:add',      '登记发货',         '交付与回款',   'action', 0),
  ('receipt:view',      '查看收款记录',     '交付与回款',   'menu',   0),
  ('receipt:add',       '登记收款',         '交付与回款',   'action', 0),
  ('receivable:view',   '查看应收账款',     '交付与回款',   'menu',   0),
  ('receivable:confirm','核销应收账款',     '交付与回款',   'action', 0),
  ('ticket:view',       '查看售后工单',     '售后管理',     'menu',   0),
  ('ticket:add',        '新建售后工单',     '售后管理',     'action', 0),
  ('ticket:edit',       '处理售后工单',     '售后管理',     'action', 0),
  ('ticket:close',      '关闭售后工单',     '售后管理',     'action', 0),
  ('revisit:view',      '查看客户回访',     '售后管理',     'menu',   0),
  ('revisit:add',       '新增回访记录',     '售后管理',     'action', 0),
  ('revisit:edit',      '编辑回访记录',     '售后管理',     'action', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type);

-- 分组名称与菜单对齐
UPDATE permissions SET module = '客户管理' WHERE module = '线索管理';
UPDATE permissions SET module = '销售管理' WHERE module IN ('商机管理', '订单管理', '商品管理');
UPDATE permissions SET module = '设置'     WHERE module IN ('员工管理', '角色管理', '部门管理', '权限授权');

-- 组内顺序 = 菜单顺序（商机 → 报价单 → 销售订单 → 退换货单 → 商品）
UPDATE permissions p
JOIN (
  SELECT code,
         ROW_NUMBER() OVER (
           PARTITION BY module
           ORDER BY CASE
             WHEN code LIKE 'dashboard:%'  THEN 10
             WHEN code LIKE 'todo:%'       THEN 20
             WHEN code LIKE 'clue:%'       THEN 10
             WHEN code LIKE 'pool:%'       THEN 20
             WHEN code LIKE 'customer:%'   THEN 30
             WHEN code LIKE 'contact:%'    THEN 40
             WHEN code LIKE 'followup:%'   THEN 50
             WHEN code LIKE 'opportunity:%' THEN 10
             WHEN code LIKE 'quote:%'      THEN 20
             WHEN code LIKE 'order:%'      THEN 30
             WHEN code LIKE 'return:%'     THEN 40
             WHEN code LIKE 'product:%'    THEN 50
             WHEN code LIKE 'delivery:%'   THEN 10
             WHEN code LIKE 'receipt:%'    THEN 20
             WHEN code LIKE 'receivable:%' THEN 30
             WHEN code LIKE 'ticket:%'     THEN 10
             WHEN code LIKE 'revisit:%'    THEN 20
             WHEN code LIKE 'system:%'     THEN 10
             ELSE 90 END,
           sort_order, id
         ) AS rn
  FROM permissions
) t ON t.code = p.code
SET p.sort_order = CASE p.module
    WHEN '工作台'     THEN 10
    WHEN '客户管理'   THEN 100
    WHEN '销售管理'   THEN 200
    WHEN '交付与回款' THEN 300
    WHEN '售后管理'   THEN 400
    WHEN '设置'       THEN 500
    ELSE 900 END + t.rn;

-- ---------------- 按角色授权 ----------------
-- 销售部（含一组、二组）：报价单全流程 + 退换货申请 + 交付回款只读 + 售后工单查看 + 客户回访
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'quote:view','quote:add','quote:edit','quote:delete',
  'return:view','return:add','return:edit',
  'delivery:view','receipt:view','receivable:view',
  'ticket:view','revisit:view','revisit:add'
) WHERE r.code IN ('SALES','SALES_G1','SALES_G2');

-- 营销：报价单与退换货单只读（看得到销售进度就行）
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'quote:view','return:view','ticket:view','revisit:view'
) WHERE r.code = 'MARKET';

-- 财务：收款、应收核销，报价/退换货/发货只读
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'quote:view','return:view','delivery:view',
  'receipt:view','receipt:add',
  'receivable:view','receivable:confirm'
) WHERE r.code = 'FINANCE';

-- 售后：售后工单全流程 + 客户回访 + 发货记录只读（判断是否已到货）
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'ticket:view','ticket:add','ticket:edit','ticket:close',
  'revisit:view','revisit:add','revisit:edit',
  'delivery:view'
) WHERE r.code = 'SERVICE';

-- 生产：登记发货 + 退换货单只读（退货要回库）
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'delivery:view','delivery:add','return:view'
) WHERE r.code = 'PRODUCTION';

-- 管理员：全部
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';
