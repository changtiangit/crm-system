SET NAMES utf8mb4;

-- ============================================================
-- 手机号 + 短信验证码登录 所需的结构调整
-- 可重复执行
-- ============================================================

-- 1. 手机号统一为 11 位数字（原初始化数据带 - 分隔符）
UPDATE employees
SET phone = REPLACE(REPLACE(REPLACE(phone, '-', ''), ' ', ''), '+86', '')
WHERE phone IS NOT NULL AND phone <> '';

-- 2. 手机号将作为登录账号，加索引
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'employees' AND index_name = 'idx_employees_phone'
);
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE employees ADD INDEX idx_employees_phone (phone)',
  'SELECT ''idx_employees_phone 已存在'' AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 短信验证码表
CREATE TABLE IF NOT EXISTS sms_codes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  phone VARCHAR(20) NOT NULL COMMENT '接收手机号',
  scene VARCHAR(20) NOT NULL DEFAULT 'login' COMMENT '使用场景：login=登录',
  code_hash CHAR(64) NOT NULL COMMENT '验证码 SHA-256，不存明文',
  attempts TINYINT NOT NULL DEFAULT 0 COMMENT '已错误次数',
  used TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用',
  expire_at DATETIME NOT NULL COMMENT '过期时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sms_phone_scene (phone, scene, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信验证码';
