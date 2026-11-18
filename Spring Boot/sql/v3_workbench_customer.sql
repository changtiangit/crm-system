-- 工作台 + 客户管理 二级模块（销售线索 / 客户公海 / 客户档案 / 联系人 / 跟进记录）
-- 幂等：重复执行只更新名称与排序，不会产生重复权限

USE crm;

INSERT INTO permissions (code, name, module, type, sort_order) VALUES
  ('dashboard:view',   '查看经营概览',   '工作台',   'menu',   1),
  ('todo:view',        '查看我的待办',   '工作台',   'menu',   2),
  ('todo:add',         '新增待办',       '工作台',   'action', 3),
  ('todo:done',        '办结待办',       '工作台',   'action', 4),
  ('pool:view',        '查看客户公海',   '客户管理', 'menu',   101),
  ('pool:claim',       '领取公海客户',   '客户管理', 'action', 102),
  ('contact:view',     '查看联系人',     '客户管理', 'menu',   103),
  ('contact:add',      '新增联系人',     '客户管理', 'action', 104),
  ('contact:edit',     '编辑联系人',     '客户管理', 'action', 105),
  ('contact:delete',   '删除联系人',     '客户管理', 'action', 106),
  ('followup:view',    '查看跟进记录',   '客户管理', 'menu',   107),
  ('followup:add',     '新增跟进记录',   '客户管理', 'action', 108),
  ('followup:edit',    '编辑跟进记录',   '客户管理', 'action', 109),
  ('followup:delete',  '删除跟进记录',   '客户管理', 'action', 110)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), sort_order = VALUES(sort_order);

-- 销售部（含一组、二组）：工作台 + 公海领取 + 联系人 + 跟进
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'dashboard:view','todo:view','todo:add','todo:done','pool:view','pool:claim',
  'contact:view','contact:add','contact:edit','contact:delete',
  'followup:view','followup:add','followup:edit'
) WHERE r.code IN ('SALES','SALES_G1','SALES_G2');

-- 营销：工作台 + 公海领取 + 联系人（可增改）+ 跟进
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'dashboard:view','todo:view','todo:add','todo:done','pool:view','pool:claim',
  'contact:view','contact:add','contact:edit','followup:view','followup:add'
) WHERE r.code = 'MARKET';

-- 售后：工作台 + 联系人只读 + 跟进（售后回访也记跟进）
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'dashboard:view','todo:view','todo:add','todo:done','contact:view','followup:view','followup:add'
) WHERE r.code = 'SERVICE';

-- 财务 / 生产：只看工作台与待办
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('dashboard:view','todo:view','todo:add','todo:done')
WHERE r.code IN ('FINANCE','PRODUCTION');
