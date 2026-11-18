-- ============================================================
-- v2：角色/部门结构精简 + 商品权限按岗位拆分
--   1) 部门绑定「对应角色」：注册时选部门 → 自动获得该部门角色
--   2) 销售中心更名销售部，销售一部/二部更名销售一组/二组（与角色树一致）
--   3) 删除角色：技术（并入售后）、普通用户、商品管理员
--   4) 商品权限拆分：新增 product:offline(下架) / product:stock(库存增减) / product:tech(技术维修资料)
--   5) 商品表增加 tech_note（技术/维修资料，售后维护）
-- 执行：mysql -uroot -p crm < v2_role_dept_adjust.sql
-- ============================================================
SET NAMES utf8mb4;

-- ---------- 1) 部门绑定角色 ----------
ALTER TABLE departments
  ADD COLUMN role_id BIGINT UNSIGNED NULL COMMENT '该部门注册时默认授予的角色' AFTER status,
  ADD INDEX idx_departments_role (role_id);

-- ---------- 2) 部门更名：销售部 > 销售一组 / 销售二组 ----------
UPDATE departments SET name = '销售部'   WHERE code = 'DEPT_SALES_CENTER';
UPDATE departments SET name = '销售一组' WHERE code = 'DEPT_SALES_1';
UPDATE departments SET name = '销售二组' WHERE code = 'DEPT_SALES_2';

-- ---------- 3) 角色更名：销售 → 销售部 ----------
UPDATE roles
   SET name = '销售部',
       description = '销售部：负责线索跟进、客户维护与商机推进，下设销售一组、销售二组'
 WHERE code = 'SALES';

-- ---------- 4) 技术并入售后 ----------
-- 原「技术」角色的员工先补授售后，再清理技术角色
INSERT IGNORE INTO employee_roles (employee_id, role_id)
SELECT er.employee_id, s.id
  FROM employee_roles er
  JOIN roles t ON t.id = er.role_id AND t.code = 'TECH'
  JOIN roles s ON s.code = 'SERVICE';

-- 售后接管技术的业务范围（商机查看）
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'opportunity:view'
 WHERE r.code = 'SERVICE';

DELETE er FROM employee_roles er JOIN roles r ON r.id = er.role_id
 WHERE r.code IN ('TECH', 'CUSTOMER', 'PRODUCT_ADMIN');
DELETE rp FROM role_permissions rp JOIN roles r ON r.id = rp.role_id
 WHERE r.code IN ('TECH', 'CUSTOMER', 'PRODUCT_ADMIN');
DELETE FROM roles WHERE code IN ('TECH', 'CUSTOMER', 'PRODUCT_ADMIN');

-- ---------- 5) 部门 → 角色 绑定 ----------
UPDATE departments d JOIN roles r ON r.code = 'ADMIN'      SET d.role_id = r.id WHERE d.code = 'DEPT_GENERAL';
UPDATE departments d JOIN roles r ON r.code = 'SALES'      SET d.role_id = r.id WHERE d.code = 'DEPT_SALES_CENTER';
UPDATE departments d JOIN roles r ON r.code = 'SALES_G1'   SET d.role_id = r.id WHERE d.code = 'DEPT_SALES_1';
UPDATE departments d JOIN roles r ON r.code = 'SALES_G2'   SET d.role_id = r.id WHERE d.code = 'DEPT_SALES_2';
UPDATE departments d JOIN roles r ON r.code = 'MARKET'     SET d.role_id = r.id WHERE d.code IN ('DEPT_MARKET_CENTER', 'DEPT_MARKET');
UPDATE departments d JOIN roles r ON r.code = 'FINANCE'    SET d.role_id = r.id WHERE d.code = 'DEPT_FINANCE';
UPDATE departments d JOIN roles r ON r.code = 'SERVICE'    SET d.role_id = r.id WHERE d.code IN ('DEPT_TECH', 'DEPT_SERVICE');
UPDATE departments d JOIN roles r ON r.code = 'PRODUCTION' SET d.role_id = r.id WHERE d.code = 'DEPT_PRODUCTION';

-- ---------- 6) 商品权限拆分 ----------
INSERT INTO permissions (code, name, module, type, sort_order) VALUES
  ('product:offline', '商品下架',      '商品管理', 'action', 440),
  ('product:stock',   '库存增减',      '商品管理', 'action', 450),
  ('product:tech',    '技术/维修资料', '商品管理', 'action', 460);

-- 生产：商品新增、编辑、下架、库存增减
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
  ON p.code IN ('product:view', 'product:add', 'product:edit', 'product:offline', 'product:stock')
 WHERE r.code = 'PRODUCTION';

-- 售后：商品查看 + 技术/维修资料维护
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('product:view', 'product:tech')
 WHERE r.code = 'SERVICE';

-- ---------- 7) 商品表：技术/维修资料 ----------
ALTER TABLE products
  ADD COLUMN tech_note VARCHAR(500) NULL COMMENT '技术参数与维修说明，售后角色维护' AFTER remark;

UPDATE products SET tech_note = '保养周期 5000-8000 公里；更换前需熄火泄压，安装时注意密封圈方向，装好后怠速检查有无渗漏。'
 WHERE code IN ('SP0001', 'SP0002');
UPDATE products SET tech_note = '按车型选用对应粘度等级；加注量以油尺上下限之间为准，过量会导致曲轴箱压力异常。'
 WHERE code IN ('SP0005', 'SP0006');
UPDATE products SET tech_note = '更换时成套更换，避免新旧混用；扭矩按 25-30N·m 执行，装车后需路试确认无异常噪音。'
 WHERE code IN ('SP0015', 'SP0016');
