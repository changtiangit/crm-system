-- =====================================================================
-- v16 销售顾问的起始客户 + 清掉历史通用待办
--
-- 1) 历史待办：v15 之前所有账号共用一份通用待办（客户跟进 / 合同回款 /
--    报价审批 / 售后回访），营销、财务等岗位也在看这些内容。这里把
--    「没有岗位标记（payload 里没有 relatedType）」的历史待办清掉，
--    各账号下次登录会按岗位画像重新生成。
-- 2) 销售顾问：演示账号（陈嘉伟等）是 SELF 数据范围，原来名下没有任何
--    客户与订单，工作台全是 0。这里给陈嘉伟补一套真实的起步客户：
--    2 家客户 / 3 张订单 / 2 个商机 / 2 条线索 / 1 份报价 / 1 笔应收。
--
-- 脚本可重复执行：全部用「不存在才插入」保护。
-- =====================================================================

USE crm;

-- ---------- 1. 清掉历史通用待办 ----------
DELETE FROM crm_module_records
WHERE module_name = 'todos'
  AND JSON_EXTRACT(payload, '$.relatedType') IS NULL;

-- ---------- 2. 销售顾问陈嘉伟的起步客户 ----------
SET @rep := '陈嘉伟';
SET @custA := '宁波甬城汽车服务有限公司';
SET @custB := '合肥皖通汽配贸易行';

INSERT INTO crm_customers (name, industry, level, contact, phone, owner, create_time)
SELECT @custA, '汽车维修', 'B', '张伟', '138-5741-2088', @rep, '2026-09-20 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM crm_customers WHERE name = @custA);

INSERT INTO crm_customers (name, industry, level, contact, phone, owner, create_time)
SELECT @custB, '汽配经销', 'C', '刘敏', '139-5512-7788', @rep, '2026-10-18 14:30:00'
WHERE NOT EXISTS (SELECT 1 FROM crm_customers WHERE name = @custB);

-- 订单（单号唯一，重复执行会被忽略）
INSERT IGNORE INTO crm_orders (order_no, customer, contact, phone, amount, status, order_date, owner) VALUES
('SO20260928043', @custA, '张伟', '138-5741-2088', 15400.00, '已完成',   '2026-09-28', @rep),
('SO20261104042', @custB, '刘敏', '139-5512-7788',  6450.00, '已付款',   '2026-11-04', @rep),
('SO20261110041', @custA, '张伟', '138-5741-2088',  7080.00, '部分回款', '2026-11-10', @rep);

INSERT IGNORE INTO crm_order_items (order_no, product, category, qty, price, cost, amount, sort_no) VALUES
('SO20260928043', '全合成机油', '油液类',   40, 260.00, 180.00, 10400.00, 0),
('SO20260928043', '机油滤芯',   '保养滤芯类', 200,  25.00,  12.00,  5000.00, 1),
('SO20261104042', '刹车片',     '制动底盘类', 60,  85.00,  52.00,  5100.00, 0),
('SO20261104042', '刹车油',     '油液类',   30,  45.00,  28.00,  1350.00, 1),
('SO20261110041', '机油滤芯',   '保养滤芯类', 120,  25.00,  12.00,  3000.00, 0),
('SO20261110041', '空气滤芯',   '保养滤芯类',  60,  38.00,  19.00,  2280.00, 1),
('SO20261110041', '空调滤芯',   '保养滤芯类',  45,  40.00,  21.00,  1800.00, 2);

-- 商机
INSERT INTO crm_opportunities (name, customer, amount, stage, expect_date, owner)
SELECT '甬城汽车-门店耗材年度框架', @custA, 48000.00, '方案报价', '2026-12-20', @rep
WHERE NOT EXISTS (SELECT 1 FROM crm_opportunities WHERE name = '甬城汽车-门店耗材年度框架');

INSERT INTO crm_opportunities (name, customer, amount, stage, expect_date, owner)
SELECT '皖通汽配-制动件分销', @custB, 22000.00, '商务谈判', '2026-12-08', @rep
WHERE NOT EXISTS (SELECT 1 FROM crm_opportunities WHERE name = '皖通汽配-制动件分销');

-- 线索：顾问自己开发的两条
INSERT INTO crm_clues (name, contact, phone, source, intent, status, owner, assign_time, assign_by, create_time)
SELECT '宁波甬城二网门店耗材询价', '张伟', '138-5741-2088', '老客户推荐', '高', '跟进中', @rep,
       '2026-11-06 10:20:00', '李思', '2026-11-06 09:10:00'
WHERE NOT EXISTS (SELECT 1 FROM crm_clues WHERE name = '宁波甬城二网门店耗材询价');

INSERT INTO crm_clues (name, contact, phone, source, intent, status, owner, assign_time, assign_by, create_time)
SELECT '合肥皖通制动件批量采购', '刘敏', '139-5512-7788', '展会', '中', '待跟进', @rep,
       '2026-11-11 15:40:00', '陈默', '2026-11-11 11:05:00'
WHERE NOT EXISTS (SELECT 1 FROM crm_clues WHERE name = '合肥皖通制动件批量采购');

-- 报价单 / 跟进记录 / 应收账款（模块表存 JSON，按业务单号去重）
INSERT INTO crm_module_records (module_name, owner, scope_owner, payload)
SELECT 'quotes', @rep, @rep,
       JSON_OBJECT('quoteNo', 'QT20261112041', 'customer', @custA, 'contact', '张伟',
                   'amount', 48000, 'status', '待审批', 'quoteDate', '2026-11-12',
                   'validTo', '2026-11-27', 'owner', @rep)
WHERE NOT EXISTS (SELECT 1 FROM crm_module_records
                  WHERE module_name = 'quotes' AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.quoteNo')) = 'QT20261112041');

INSERT INTO crm_module_records (module_name, owner, scope_owner, payload)
SELECT 'followups', @rep, @rep,
       JSON_OBJECT('time', '2026-11-12 14:20', 'customer', @custA, 'contact', '张伟',
                   'type', '上门拜访', 'content', '带滤芯样件上门，确认二网门店的月度用量与配送频次。',
                   'next', '11-16 前把年度框架报价改成两档方案', 'owner', @rep)
WHERE NOT EXISTS (SELECT 1 FROM crm_module_records
                  WHERE module_name = 'followups' AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.time')) = '2026-11-12 14:20'
                    AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.customer')) = '宁波甬城汽车服务有限公司');

INSERT INTO crm_module_records (module_name, owner, scope_owner, payload)
SELECT 'followups', @rep, @rep,
       JSON_OBJECT('time', '2026-11-11 16:00', 'customer', @custB, 'contact', '刘敏',
                   'type', '电话', 'content', '客户对比了竞品刹车片价格，要求按 60 套阶梯价再让 3 个点。',
                   'next', '11-15 申请阶梯价后回电', 'owner', @rep)
WHERE NOT EXISTS (SELECT 1 FROM crm_module_records
                  WHERE module_name = 'followups' AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.time')) = '2026-11-11 16:00'
                    AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.customer')) = '合肥皖通汽配贸易行');

INSERT INTO crm_module_records (module_name, owner, scope_owner, payload)
SELECT 'receivables', @rep, @rep,
       JSON_OBJECT('customer', @custA, 'orderNo', 'SO20261110041', 'total', 7080,
                   'received', 3000, 'dueDate', '2026-11-28', 'owner', @rep)
WHERE NOT EXISTS (SELECT 1 FROM crm_module_records
                  WHERE module_name = 'receivables' AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.orderNo')) = 'SO20261110041');

-- 校验：陈嘉伟名下的数据量
SELECT @rep AS owner,
       (SELECT COUNT(*) FROM crm_customers WHERE owner = @rep)     AS customers,
       (SELECT COUNT(*) FROM crm_orders WHERE owner = @rep)        AS orders,
       (SELECT COUNT(*) FROM crm_opportunities WHERE owner = @rep) AS opportunities,
       (SELECT COUNT(*) FROM crm_clues WHERE owner = @rep)         AS clues,
       (SELECT COUNT(*) FROM crm_module_records WHERE module_name = 'quotes' AND owner = @rep) AS quotes;
