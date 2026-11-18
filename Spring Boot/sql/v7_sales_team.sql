-- 销售团队与线索分配
-- 1) 员工增加「职位 / 能力等级 / 线索容量 / 直属组长 / 所属销售组」
-- 2) 线索增加「意向等级 / 分配时间 / 分配人」，并引入「待分配」状态
-- 3) 新增分配日志表，团队页可以看到最近的分配动态
-- 幂等：列只在缺失时添加，人员与线索按工号 / 手机号反连接插入

USE crm;

-- ---------------- 1. 员工表补充销售属性 ----------------
DROP PROCEDURE IF EXISTS crm_add_column;
DELIMITER $$
CREATE PROCEDURE crm_add_column(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(500))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN ', ddl);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL crm_add_column('employees', 'position',      'position VARCHAR(30) NOT NULL DEFAULT '''' COMMENT ''职位'' AFTER name');
CALL crm_add_column('employees', 'sales_level',   'sales_level VARCHAR(10) NOT NULL DEFAULT '''' COMMENT ''销售能力等级 S/A/B/C'' AFTER position');
CALL crm_add_column('employees', 'clue_capacity', 'clue_capacity INT NOT NULL DEFAULT 0 COMMENT ''在手线索上限'' AFTER sales_level');
CALL crm_add_column('employees', 'leader_id',     'leader_id BIGINT UNSIGNED NULL COMMENT ''直属组长'' AFTER clue_capacity');
CALL crm_add_column('employees', 'sales_group',   'sales_group VARCHAR(30) NOT NULL DEFAULT '''' COMMENT ''所属销售组'' AFTER leader_id');

CALL crm_add_column('crm_clues', 'intent',      'intent VARCHAR(10) NOT NULL DEFAULT ''中'' COMMENT ''意向等级：高/中/低'' AFTER source');
CALL crm_add_column('crm_clues', 'assign_time', 'assign_time DATETIME NULL COMMENT ''最近分配时间'' AFTER owner');
CALL crm_add_column('crm_clues', 'assign_by',   'assign_by VARCHAR(80) NOT NULL DEFAULT '''' COMMENT ''分配人'' AFTER assign_time');

DROP PROCEDURE IF EXISTS crm_add_column;

-- ---------------- 2. 补齐现有员工的职位 ----------------
UPDATE employees SET position = '系统管理员' WHERE employee_no = 'ADMIN' AND position = '';
UPDATE employees SET position = '销售总监', sales_level = 'S', clue_capacity = 8, sales_group = '销售部'
  WHERE employee_no = 'EMP001' AND position = '';
UPDATE employees SET position = '销售组长', sales_level = 'A', clue_capacity = 6, sales_group = '销售一组'
  WHERE employee_no = 'EMP002' AND position = '';
UPDATE employees SET position = '销售组长', sales_level = 'A', clue_capacity = 6, sales_group = '销售二组'
  WHERE employee_no = 'EMP003' AND position = '';
UPDATE employees SET position = '市场运营' WHERE employee_no IN ('EMP004', 'EMP005') AND position = '';
UPDATE employees SET position = '财务' WHERE employee_no = 'EMP006' AND position = '';
UPDATE employees SET position = '技术' WHERE employee_no = 'EMP007' AND position = '';
UPDATE employees SET position = '售后' WHERE employee_no = 'EMP008' AND position = '';
UPDATE employees SET position = '生产' WHERE employee_no = 'EMP009' AND position = '';

-- 组长关系：李思、周婷 向张明汇报
UPDATE employees SET leader_id = (SELECT id FROM (SELECT id FROM employees WHERE employee_no = 'EMP001') t)
  WHERE employee_no IN ('EMP002', 'EMP003') AND leader_id IS NULL;

-- ---------------- 3. 新增 6 名销售员工（默认密码 123456） ----------------
SET @default_pwd = (SELECT password_hash FROM employees WHERE employee_no = 'EMP001');

DROP TEMPORARY TABLE IF EXISTS tmp_sales_members;
CREATE TEMPORARY TABLE tmp_sales_members (
  no VARCHAR(20), name VARCHAR(100), phone VARCHAR(30), grp VARCHAR(30),
  position VARCHAR(30), level VARCHAR(10), capacity INT, leader_no VARCHAR(20)
);
INSERT INTO tmp_sales_members VALUES
  ('EMP010', '陈嘉伟', '13800110010', '销售一组', '销售顾问', 'S', 8, 'EMP002'),
  ('EMP011', '林晓',   '13800110011', '销售一组', '销售顾问', 'B', 5, 'EMP002'),
  ('EMP012', '方宇',   '13800110012', '销售一组', '销售顾问', 'C', 3, 'EMP002'),
  ('EMP013', '黄志远', '13800110013', '销售二组', '销售顾问', 'A', 7, 'EMP003'),
  ('EMP014', '苏晴',   '13800110014', '销售二组', '销售顾问', 'B', 4, 'EMP003'),
  ('EMP015', '何俊',   '13800110015', '销售二组', '销售顾问', 'C', 2, 'EMP003');

INSERT INTO employees(employee_no, username, password_hash, name, position, sales_level,
                      clue_capacity, leader_id, sales_group, department_id, phone, status)
SELECT t.no, t.no, @default_pwd, t.name, t.position, t.level,
       t.capacity, l.id, t.grp, d.id, t.phone, 1
FROM tmp_sales_members t
JOIN departments d ON d.name = t.grp
LEFT JOIN employees l ON l.employee_no = t.leader_no
LEFT JOIN employees e ON e.employee_no = t.no
WHERE e.id IS NULL;

-- 销售组角色：一组 / 二组
INSERT IGNORE INTO employee_roles(employee_id, role_id)
SELECT e.id, r.id FROM employees e JOIN roles r ON r.code = 'SALES_G1'
WHERE e.employee_no IN ('EMP010', 'EMP011', 'EMP012');
INSERT IGNORE INTO employee_roles(employee_id, role_id)
SELECT e.id, r.id FROM employees e JOIN roles r ON r.code = 'SALES_G2'
WHERE e.employee_no IN ('EMP013', 'EMP014', 'EMP015');

-- ---------------- 4. 线索：状态、意向与初始分配时间 ----------------
UPDATE crm_clues SET assign_time = create_time, assign_by = '历史导入'
WHERE owner <> '' AND assign_time IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_pending_clues;
CREATE TEMPORARY TABLE tmp_pending_clues (
  name VARCHAR(150), contact VARCHAR(80), phone VARCHAR(30), source VARCHAR(50),
  intent VARCHAR(10), create_time DATETIME
);
INSERT INTO tmp_pending_clues VALUES
  ('城东汽修连锁-机油三滤年度采购', '徐建国', '13800112233', '官网表单',   '高', '2026-11-13 09:12:00'),
  ('星辰汽车服务-刹车片集采询价',   '罗敏',   '13900223344', '电话咨询',   '中', '2026-11-13 08:40:00'),
  ('嘉禾 4S 店-原厂替代件试供',     '高翔',   '13700334455', '展会',       '高', '2026-11-12 17:25:00'),
  ('蓝盾车队-防冻液冬季备货',       '马涛',   '13600445566', '老客户推荐', '中', '2026-11-12 15:10:00'),
  ('易修快保-洗车耗材月度补货',     '曹雪',   '13500556677', '广告投放',   '低', '2026-11-12 11:32:00'),
  ('驰加养护-变速箱油招标报名',     '范志强', '13400667788', '官网表单',   '高', '2026-11-12 10:05:00'),
  ('鹏程汽配-火花塞分销合作',       '唐磊',   '13300778899', '展会',       '中', '2026-11-11 16:48:00'),
  ('顺达洗车-镀晶耗材体验申请',     '邱倩',   '13200889900', '广告投放',   '低', '2026-11-11 14:20:00'),
  ('恒信维修厂-底盘件长期供货',     '贺强',   '13100990011', '电话咨询',   '高', '2026-11-11 09:35:00'),
  ('安途汽车-轮胎与轮毂采购',       '潘月',   '13001001122', '老客户推荐', '中', '2026-11-10 17:02:00'),
  ('车管家连锁-雨刮片年采',         '汪洋',   '18901112233', '官网表单',   '中', '2026-11-10 15:26:00'),
  ('新干线快修-空调滤芯补货',       '傅琳',   '18801223344', '电话咨询',   '低', '2026-11-10 10:14:00'),
  ('中鑫汽贸-美容养护品配套',       '钟诚',   '18701334455', '展会',       '高', '2026-11-09 16:40:00'),
  ('路航车队-柴油机油集中采购',     '白露',   '18601445566', '老客户推荐', '中', '2026-11-09 11:08:00'),
  ('优卡汽车-贴膜工具询价',         '顾凯',   '18501556677', '广告投放',   '低', '2026-11-09 09:50:00'),
  ('众诚汽修-点火线圈试单',         '宁静',   '18401667788', '官网表单',   '中', '2026-11-08 17:30:00'),
  ('万里车队-润滑脂季度采购',       '章浩',   '18301778899', '电话咨询',   '高', '2026-11-08 14:12:00'),
  ('车之翼-免拆清洗设备配套',       '施雨',   '18201889900', '展会',       '低', '2026-11-08 09:05:00');

INSERT INTO crm_clues(name, contact, phone, source, intent, status, owner, create_time)
SELECT t.name, t.contact, t.phone, t.source, t.intent, '待分配', '', t.create_time
FROM tmp_pending_clues t
LEFT JOIN crm_clues c ON c.phone = t.phone
WHERE c.id IS NULL;

-- ---------------- 5. 分配日志 ----------------
CREATE TABLE IF NOT EXISTS crm_clue_assign_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  clue_id BIGINT UNSIGNED NOT NULL,
  clue_name VARCHAR(150) NOT NULL DEFAULT '',
  from_owner VARCHAR(80) NOT NULL DEFAULT '',
  to_owner VARCHAR(80) NOT NULL DEFAULT '',
  operator VARCHAR(80) NOT NULL DEFAULT '',
  mode VARCHAR(20) NOT NULL DEFAULT '手动',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_log_clue (clue_id),
  KEY idx_log_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 历史已分配的线索补一条初始记录，团队页不至于空着
INSERT INTO crm_clue_assign_log(clue_id, clue_name, from_owner, to_owner, operator, mode, created_at)
SELECT c.id, c.name, '', c.owner, '历史导入', '初始分配', c.create_time
FROM crm_clues c
LEFT JOIN crm_clue_assign_log l ON l.clue_id = c.id
WHERE c.owner <> '' AND l.id IS NULL;

-- ---------------- 6. 权限：销售团队 ----------------
INSERT INTO permissions(code, name, module, type, sort_order) VALUES
  ('sales:team:view', '查看销售团队', '客户管理', 'menu',   0),
  ('sales:team:edit', '维护团队与配额', '客户管理', 'action', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type);

-- 客户管理模块重新排序：线索 → 销售团队 → 公海 → 客户 → 联系人 → 跟进
UPDATE permissions p
JOIN (
  SELECT code, ROW_NUMBER() OVER (ORDER BY CASE
      WHEN code LIKE 'clue:%'       THEN 10
      WHEN code LIKE 'sales:team:%' THEN 20
      WHEN code LIKE 'pool:%'       THEN 30
      WHEN code LIKE 'customer:%'   THEN 40
      WHEN code LIKE 'contact:%'    THEN 50
      WHEN code LIKE 'followup:%'   THEN 60
      ELSE 90 END, sort_order, id) AS rn
  FROM permissions WHERE module = '客户管理'
) t ON t.code = p.code
SET p.sort_order = 100 + t.rn;

-- 授权：管理员全给；销售条线、营销（运营）只读
INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
WHERE p.code IN ('sales:team:view', 'sales:team:edit') AND r.code = 'ADMIN';

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
WHERE p.code = 'sales:team:edit' AND r.code = 'SALES';

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
WHERE p.code = 'sales:team:view' AND r.code IN ('SALES', 'SALES_G1', 'SALES_G2', 'MARKET');

-- 营销（运营）也要能把线索转到公海 / 改负责人，已有 clue:assign，无需再加

-- ---------------- 7. 校验 ----------------
SELECT '销售团队' AS item, COUNT(*) AS cnt FROM employees WHERE sales_group <> ''
UNION ALL SELECT '待分配线索', COUNT(*) FROM crm_clues WHERE status = '待分配'
UNION ALL SELECT '分配日志', COUNT(*) FROM crm_clue_assign_log;
