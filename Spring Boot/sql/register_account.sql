SET NAMES utf8mb4;

-- 注册用户默认角色（如已存在则保持现状）
INSERT INTO roles(code, name, description, status)
SELECT 'CUSTOMER', '普通用户', '新用户注册默认角色', 1
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'CUSTOMER');

-- 注册以手机号登录，清洗旧数据后为手机号建立唯一约束。
-- 若提示重复键，先处理已有重复手机号再重新执行。
UPDATE employees
SET phone = REPLACE(REPLACE(REPLACE(phone, '-', ''), ' ', ''), '+86', '')
WHERE phone IS NOT NULL AND phone <> '';

ALTER TABLE employees
  ADD UNIQUE KEY uk_employees_phone (phone);
