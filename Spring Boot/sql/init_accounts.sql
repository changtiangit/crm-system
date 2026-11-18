SET NAMES utf8mb4;

-- 1. 结构补充：角色层级 + 功能分组
ALTER TABLE roles ADD COLUMN parent_id BIGINT UNSIGNED NULL COMMENT '上级角色' AFTER code;
ALTER TABLE roles ADD KEY idx_roles_parent (parent_id);
ALTER TABLE roles ADD CONSTRAINT fk_roles_parent FOREIGN KEY (parent_id) REFERENCES roles(id) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE permissions ADD COLUMN module VARCHAR(50) NOT NULL DEFAULT '其他' COMMENT '功能分组' AFTER name;

-- 2. 清空后重新初始化
DELETE FROM role_permissions;
DELETE FROM employee_roles;
DELETE FROM employees;
DELETE FROM roles;
DELETE FROM permissions;

-- 3. 功能点
INSERT INTO permissions(module, name, code, type, sort_order) VALUES
  ('线索管理', '查看线索', 'clue:view', 'menu', 10),
  ('线索管理', '新增线索', 'clue:add', 'action', 20),
  ('线索管理', '编辑线索', 'clue:edit', 'action', 30),
  ('线索管理', '删除线索', 'clue:delete', 'action', 40),
  ('线索管理', '分配线索', 'clue:assign', 'action', 50),
  ('客户管理', '查看客户', 'customer:view', 'menu', 60),
  ('客户管理', '新增客户', 'customer:add', 'action', 70),
  ('客户管理', '编辑客户', 'customer:edit', 'action', 80),
  ('客户管理', '删除客户', 'customer:delete', 'action', 90),
  ('客户管理', '转移客户', 'customer:transfer', 'action', 100),
  ('商机管理', '查看商机', 'opportunity:view', 'menu', 110),
  ('商机管理', '新增商机', 'opportunity:add', 'action', 120),
  ('商机管理', '编辑商机', 'opportunity:edit', 'action', 130),
  ('商机管理', '删除商机', 'opportunity:delete', 'action', 140),
  ('订单管理', '查看订单', 'order:view', 'menu', 150),
  ('订单管理', '新增订单', 'order:add', 'action', 160),
  ('订单管理', '编辑订单', 'order:edit', 'action', 170),
  ('订单管理', '删除订单', 'order:delete', 'action', 180),
  ('订单管理', '导出订单', 'order:export', 'action', 190),
  ('员工管理', '查看员工', 'system:employee:view', 'menu', 200),
  ('员工管理', '新增员工', 'system:employee:add', 'action', 210),
  ('员工管理', '编辑员工', 'system:employee:edit', 'action', 220),
  ('员工管理', '删除员工', 'system:employee:delete', 'action', 230),
  ('角色管理', '查看角色', 'system:role:view', 'menu', 240),
  ('角色管理', '新增角色', 'system:role:add', 'action', 250),
  ('角色管理', '编辑角色', 'system:role:edit', 'action', 260),
  ('角色管理', '删除角色', 'system:role:delete', 'action', 270),
  ('角色管理', '分配功能', 'system:role:assign', 'action', 280),
  ('部门管理', '查看部门', 'system:dept:view', 'menu', 290),
  ('部门管理', '新增部门', 'system:dept:add', 'action', 300),
  ('部门管理', '编辑部门', 'system:dept:edit', 'action', 310),
  ('部门管理', '删除部门', 'system:dept:delete', 'action', 320),
  ('权限授权', '查看权限', 'system:permission:view', 'menu', 330),
  ('权限授权', '角色授权', 'system:permission:assign', 'action', 340);

-- 4. 角色
INSERT INTO roles(code, name, description, status) VALUES
  ('ADMIN', '管理员', '系统管理员，拥有全部功能，可维护所有员工账号、角色与部门', 1),
  ('CUSTOMER', '普通用户', '新用户注册默认角色', 1),
  ('SALES', '销售', '销售部门角色，负责线索跟进、客户维护与商机推进（可设下级小组）', 1),
  ('FINANCE', '财务', '负责订单回款、开票与财务对账', 1),
  ('MARKET', '营销', '负责线索获取、分配与市场活动', 1),
  ('SERVICE', '售后', '负责客户售后服务与回访', 1),
  ('PRODUCTION', '生产', '负责订单生产排期与交付', 1),
  ('TECH', '技术', '负责售前技术支持与实施交付', 1),
  ('SALES_G1', '销售一组', '销售部下属小组：华东区域', 1),
  ('SALES_G2', '销售二组', '销售部下属小组：华南区域', 1);

-- 5. 角色层级：销售一组、销售二组挂在销售下
UPDATE roles c JOIN roles p ON p.code = 'SALES' SET c.parent_id = p.id WHERE c.code IN ('SALES_G1', 'SALES_G2');

-- 6. 角色授权
INSERT INTO role_permissions(role_id, permission_id) SELECT r.id, p.id FROM roles r JOIN permissions p WHERE r.code = 'ADMIN' AND p.code IN ('clue:view', 'clue:add', 'clue:edit', 'clue:delete', 'clue:assign', 'customer:view', 'customer:add', 'customer:edit', 'customer:delete', 'customer:transfer', 'opportunity:view', 'opportunity:add', 'opportunity:edit', 'opportunity:delete', 'order:view', 'order:add', 'order:edit', 'order:delete', 'order:export', 'system:employee:view', 'system:employee:add', 'system:employee:edit', 'system:employee:delete', 'system:role:view', 'system:role:add', 'system:role:edit', 'system:role:delete', 'system:role:assign', 'system:dept:view', 'system:dept:add', 'system:dept:edit', 'system:dept:delete', 'system:permission:view', 'system:permission:assign');
INSERT INTO role_permissions(role_id, permission_id) SELECT r.id, p.id FROM roles r JOIN permissions p WHERE r.code = 'SALES' AND p.code IN ('clue:view', 'clue:add', 'clue:edit', 'customer:view', 'customer:add', 'customer:edit', 'opportunity:view', 'opportunity:add', 'opportunity:edit', 'order:view', 'order:add');
INSERT INTO role_permissions(role_id, permission_id) SELECT r.id, p.id FROM roles r JOIN permissions p WHERE r.code = 'SALES_G1' AND p.code IN ('clue:view', 'clue:add', 'clue:edit', 'customer:view', 'opportunity:view');
INSERT INTO role_permissions(role_id, permission_id) SELECT r.id, p.id FROM roles r JOIN permissions p WHERE r.code = 'SALES_G2' AND p.code IN ('clue:view', 'clue:add', 'clue:edit', 'customer:view', 'opportunity:view');
INSERT INTO role_permissions(role_id, permission_id) SELECT r.id, p.id FROM roles r JOIN permissions p WHERE r.code = 'FINANCE' AND p.code IN ('order:view', 'order:add', 'order:edit', 'order:delete', 'order:export', 'customer:view');
INSERT INTO role_permissions(role_id, permission_id) SELECT r.id, p.id FROM roles r JOIN permissions p WHERE r.code = 'MARKET' AND p.code IN ('clue:view', 'clue:add', 'clue:edit', 'clue:assign', 'customer:view', 'customer:add', 'opportunity:view');
INSERT INTO role_permissions(role_id, permission_id) SELECT r.id, p.id FROM roles r JOIN permissions p WHERE r.code = 'SERVICE' AND p.code IN ('customer:view', 'customer:edit', 'order:view');
INSERT INTO role_permissions(role_id, permission_id) SELECT r.id, p.id FROM roles r JOIN permissions p WHERE r.code = 'PRODUCTION' AND p.code IN ('order:view');
INSERT INTO role_permissions(role_id, permission_id) SELECT r.id, p.id FROM roles r JOIN permissions p WHERE r.code = 'TECH' AND p.code IN ('customer:view', 'opportunity:view', 'order:view');

-- 7. 员工账号（每个部门一名，初始密码 123456）
INSERT INTO employees(employee_no, username, password_hash, name, department_id, phone, status) VALUES
  ('ADMIN', 'ADMIN', 'pbkdf2$120000$Mv8ConLdOpbbkJdVUJI+dg$jDf3Ffvythylxag8hDImU5QYvEidy0dC31WxwYQEy5s', '系统管理员', (SELECT id FROM departments WHERE name = '总经办'), '188-0000-0001', 1),
  ('EMP001', 'EMP001', 'pbkdf2$120000$QCCtPpgxAsHvL0AUtX7/MA$94Y5i/TyzBPTWF32I06+OiCjbtJdX5PJpK1qHC61A54', '张明', (SELECT id FROM departments WHERE name = '销售中心'), '138-1024-5566', 1),
  ('EMP002', 'EMP002', 'pbkdf2$120000$kMUsaavKtm+8x3Kg2kPqog$1lnQvaW0gIwPlX/hFomW6WF3cubOBlsM9kmXzBWPS60', '李思', (SELECT id FROM departments WHERE name = '销售一部'), '139-2288-7711', 1),
  ('EMP003', 'EMP003', 'pbkdf2$120000$KnYbjN2XqyTFqupF1buFog$3gIprytDt5nE5gO8H2VSLaoA4ZJ8/JsS4/J4sDiylwc', '周婷', (SELECT id FROM departments WHERE name = '销售二部'), '137-6600-3322', 1),
  ('EMP004', 'EMP004', 'pbkdf2$120000$O2Qz/D6zJ7nMQlYUSUMcxQ$8Ezk5qOOchlKrTDund2TqvvIICI+jolXe9+lM8rgHcc', '陈默', (SELECT id FROM departments WHERE name = '市场中心'), '135-8899-1200', 1),
  ('EMP005', 'EMP005', 'pbkdf2$120000$wgZZ/NuI77+DBP7PgzbvnA$BoprgKC8LVODoGWMhVeF9tS+Vlb6Ocw9dgp1o9RpXAg', '王芳', (SELECT id FROM departments WHERE name = '市场部'), '136-7788-4455', 1),
  ('EMP006', 'EMP006', 'pbkdf2$120000$ITvXN+PhN7esyVgCKTJ6qQ$Po/vU5PGhKeuVbqYj7lxJuxJOLgFdMHCi9zo9zImVF4', '刘洋', (SELECT id FROM departments WHERE name = '财务部'), '150-6677-2233', 1),
  ('EMP007', 'EMP007', 'pbkdf2$120000$WT8QFGGa/cvbuuCV240/dA$OLrb/b/a4ct8eSHjgsrx3JvpJUhfavpZQ3JT9cNNihI', '吴磊', (SELECT id FROM departments WHERE name = '技术部'), '131-2233-9900', 1),
  ('EMP008', 'EMP008', 'pbkdf2$120000$wd9XYS5/GJn7kBgie36Glw$06kM9vqbL+zPjidIHjIefqZzALEeQzM0O+epxC8PAKk', '孙浩', (SELECT id FROM departments WHERE name = '售后部'), '188-5566-7788', 1),
  ('EMP009', 'EMP009', 'pbkdf2$120000$/KiDEVChQYR4qy/DcgmYMg$RQAYWJqEjKBPAAFiZg8hOFwmBvpSkVEB9yjcGjOO9QQ', '赵敏', (SELECT id FROM departments WHERE name = '生产部'), '130-9988-7766', 1);

-- 8. 员工角色
INSERT INTO employee_roles(employee_id, role_id) SELECT e.id, r.id FROM employees e JOIN roles r WHERE e.employee_no = 'ADMIN' AND r.code IN ('ADMIN');
INSERT INTO employee_roles(employee_id, role_id) SELECT e.id, r.id FROM employees e JOIN roles r WHERE e.employee_no = 'EMP001' AND r.code IN ('ADMIN', 'SALES');
INSERT INTO employee_roles(employee_id, role_id) SELECT e.id, r.id FROM employees e JOIN roles r WHERE e.employee_no = 'EMP002' AND r.code IN ('SALES_G1');
INSERT INTO employee_roles(employee_id, role_id) SELECT e.id, r.id FROM employees e JOIN roles r WHERE e.employee_no = 'EMP003' AND r.code IN ('SALES_G2');
INSERT INTO employee_roles(employee_id, role_id) SELECT e.id, r.id FROM employees e JOIN roles r WHERE e.employee_no = 'EMP004' AND r.code IN ('MARKET');
INSERT INTO employee_roles(employee_id, role_id) SELECT e.id, r.id FROM employees e JOIN roles r WHERE e.employee_no = 'EMP005' AND r.code IN ('MARKET');
INSERT INTO employee_roles(employee_id, role_id) SELECT e.id, r.id FROM employees e JOIN roles r WHERE e.employee_no = 'EMP006' AND r.code IN ('FINANCE');
INSERT INTO employee_roles(employee_id, role_id) SELECT e.id, r.id FROM employees e JOIN roles r WHERE e.employee_no = 'EMP007' AND r.code IN ('TECH');
INSERT INTO employee_roles(employee_id, role_id) SELECT e.id, r.id FROM employees e JOIN roles r WHERE e.employee_no = 'EMP008' AND r.code IN ('SERVICE');
INSERT INTO employee_roles(employee_id, role_id) SELECT e.id, r.id FROM employees e JOIN roles r WHERE e.employee_no = 'EMP009' AND r.code IN ('PRODUCTION');

