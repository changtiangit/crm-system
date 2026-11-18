-- 数据权限（行级可见范围）
-- 功能权限决定「能进哪个页面」，数据权限决定「页面里能看到谁的数据」。
--   ALL   全公司   总监 / 管理员 / 财务 / 运营 / 售后等职能岗
--   GROUP 本组     销售组长：本人 + 直属组员负责的客户、线索、订单
--   SELF  仅本人   销售顾问：只看到自己负责的数据
-- 幂等：列只在缺失时添加，已有手工配置不会被覆盖

USE crm;

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

CALL crm_add_column('employees', 'data_scope',
  'data_scope VARCHAR(16) NOT NULL DEFAULT '''' COMMENT ''数据范围：ALL 全公司 / GROUP 本组 / SELF 仅本人，空=按岗位推导'' AFTER sales_group');

DROP PROCEDURE IF EXISTS crm_add_column;

-- 按岗位补默认范围；只填空值，避免覆盖管理员在员工管理里手工调整过的配置
UPDATE employees SET data_scope = 'ALL'   WHERE data_scope = '' AND position IN ('系统管理员', '销售总监');
UPDATE employees SET data_scope = 'GROUP' WHERE data_scope = '' AND position = '销售组长';
UPDATE employees SET data_scope = 'SELF'  WHERE data_scope = '' AND position LIKE '销售%' AND position <> '销售总监';
UPDATE employees SET data_scope = 'ALL'   WHERE data_scope = ''
  AND position IN ('市场运营', '财务', '技术', '售后', '生产', '人事', '行政');

-- 兜底：仍未配置的账号给「全公司」，保持原有可用性（管理员可再收紧）
UPDATE employees SET data_scope = 'ALL' WHERE data_scope = '';

-- 演示数据校准：销售顾问只看到自己的客户；组长看本组；总监与职能岗看全部
UPDATE employees SET data_scope = 'ALL'   WHERE employee_no IN ('ADMIN', 'EMP001', 'EMP004', 'EMP005', 'EMP006', 'EMP007', 'EMP008', 'EMP009');
UPDATE employees SET data_scope = 'GROUP' WHERE employee_no IN ('EMP002', 'EMP003');
UPDATE employees SET data_scope = 'SELF'  WHERE employee_no IN ('EMP010', 'EMP011', 'EMP012', 'EMP013', 'EMP014', 'EMP015');

SELECT employee_no, name, position, sales_group, data_scope FROM employees ORDER BY id;

-- ---------------- 数据统计只给管理层与职能岗 ----------------
-- 销售统计 / 客户采购分析 / 产品销售分析是公司级经营数据（含全体销售业绩、全部客户采购额），
-- 销售组长与销售顾问不再拥有入口；如某位组长需要，可在「角色管理」里单独授权。
DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code IN ('SALES', 'SALES_G1', 'SALES_G2')
  AND p.code IN ('stats:sales:view', 'stats:customer:view', 'stats:product:view');

SELECT r.code, GROUP_CONCAT(p.code ORDER BY p.code) AS stats_perms
FROM roles r
LEFT JOIN role_permissions rp ON rp.role_id = r.id
LEFT JOIN permissions p ON p.id = rp.permission_id AND p.code LIKE 'stats:%'
GROUP BY r.id ORDER BY r.id;

-- ---------------- 交付与回款只给财务 / 仓储与管理层 ----------------
-- 销售只负责签单与客户关系，发货、收款、应收由财务与交付岗维护，销售角色不再保留入口。
DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code IN ('SALES', 'SALES_G1', 'SALES_G2')
  AND p.code IN ('delivery:view', 'receipt:view', 'receivable:view');

-- 销售组长需要把待分配线索拉进本组（只能分给本组成员，后端已按数据范围校验）
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'clue:assign'
WHERE r.code = 'SALES'
  AND NOT EXISTS (SELECT 1 FROM role_permissions x WHERE x.role_id = r.id AND x.permission_id = p.id);
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code = 'clue:assign'
WHERE r.code IN ('SALES_G1', 'SALES_G2')
  AND NOT EXISTS (SELECT 1 FROM role_permissions x WHERE x.role_id = r.id AND x.permission_id = p.id);
-- 注意：销售顾问与组长同属 SALES_G1/G2 角色，功能权限无法区分两者，
-- 因此「能不能分配」由后端按数据范围再判一次：SELF 范围（销售顾问）调用分配接口直接 403。

-- ---------------- 经营概览是公司级经营大盘，销售角色不再保留 ----------------
-- 销售只看自己的待办与本人/本组数据；经营大盘（签单额、目标、销售额趋势、线索来源）
-- 属于管理层视角，由管理员、销售总监以外的职能岗与总经办查看。
-- 如某位销售总监确实需要，可在「角色管理」里单独授予 dashboard:view。
DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code IN ('SALES', 'SALES_G1', 'SALES_G2')
  AND p.code = 'dashboard:view';
