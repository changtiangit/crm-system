USE crm;

CREATE TABLE IF NOT EXISTS crm_clues (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  contact VARCHAR(80) NOT NULL,
  phone VARCHAR(30) NOT NULL,
  source VARCHAR(50) NOT NULL,
  `status` VARCHAR(30) NOT NULL DEFAULT '',
  owner VARCHAR(80) NOT NULL DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_clue_status (`status`), KEY idx_clue_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_customers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  industry VARCHAR(50) NOT NULL,
  level VARCHAR(10) NOT NULL DEFAULT 'B',
  contact VARCHAR(80) NOT NULL,
  phone VARCHAR(30) NOT NULL,
  owner VARCHAR(80) NOT NULL DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_customer_name (name), KEY idx_customer_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_opportunities (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  customer VARCHAR(150) NOT NULL,
  amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  stage VARCHAR(30) NOT NULL DEFAULT '',
  expect_date DATE NOT NULL,
  owner VARCHAR(80) NOT NULL DEFAULT '',
  PRIMARY KEY (id), KEY idx_opportunity_customer (customer), KEY idx_opportunity_stage (stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS crm_orders (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL,
  customer VARCHAR(150) NOT NULL,
  contact VARCHAR(80) NOT NULL,
  phone VARCHAR(30) NOT NULL DEFAULT '',
  amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  status VARCHAR(30) NOT NULL DEFAULT '',
  order_date DATE NOT NULL,
  owner VARCHAR(80) NOT NULL DEFAULT '',
  PRIMARY KEY (id), UNIQUE KEY uk_crm_order_no (order_no), KEY idx_order_customer (customer), KEY idx_order_date (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
