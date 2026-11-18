-- ============================================================
-- v21 财务链路补齐：订单 ↔ 应收 ↔ 收款 三方对账
--   1) 没有应收单的历史订单补一张（金额 = 订单金额，已收按订单状态推）
--   2) 应收已收金额 > 0 但没有任何收款单的，补一张收款单
--   3) 最后统一按「收款单合计」回写应收已收金额（不超过应收总额）
-- 幂等：crm_seed_batches 记录 v21_finance_reconcile
-- ============================================================

CREATE TABLE IF NOT EXISTS crm_seed_batches (
  name varchar(60) NOT NULL PRIMARY KEY,
  applied_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note varchar(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS reconcile_v21_finance;
DELIMITER $$
CREATE PROCEDURE reconcile_v21_finance()
BEGIN
  IF EXISTS (SELECT 1 FROM crm_seed_batches WHERE name = 'v21_finance_reconcile') THEN
    SELECT 'skip: v21_finance_reconcile 已执行过' AS result;
  ELSE
    -- 1) 历史订单补应收单
    INSERT INTO crm_module_records(module_name, owner, scope_owner, payload)
    SELECT 'receivables', o.owner, o.owner,
           JSON_OBJECT(
             'customer', o.customer,
             'orderNo', o.order_no,
             'total', o.amount,
             'received', CASE o.status
                           WHEN '已完成' THEN o.amount
                           WHEN '已付款' THEN o.amount
                           WHEN '部分回款' THEN ROUND(o.amount * 0.5 / 10) * 10
                           ELSE 0 END,
             'dueDate', DATE_FORMAT(DATE_ADD(o.order_date, INTERVAL 30 DAY), '%Y-%m-%d'),
             'owner', o.owner)
    FROM crm_orders o
    WHERE o.status <> '已取消'
      AND NOT EXISTS (SELECT 1 FROM crm_module_records m
                      WHERE m.module_name = 'receivables'
                        AND JSON_UNQUOTE(JSON_EXTRACT(m.payload, '$.orderNo')) = o.order_no);

    -- 2) 应收有已收金额但没有收款单的，补一张收款单，日期取订单日 + 15 天
    INSERT INTO crm_module_records(module_name, owner, scope_owner, payload)
    SELECT 'receipts', r.owner, r.owner,
           JSON_OBJECT(
             'receiptNo', CONCAT('SK', DATE_FORMAT(LEAST(DATE_ADD(o.order_date, INTERVAL 15 DAY), CURDATE()), '%Y%m%d'), LPAD(r.id, 3, '0')),
             'customer', o.customer,
             'orderNo', o.order_no,
             'amount', JSON_EXTRACT(r.payload, '$.received'),
             'method', '银行转账',
             'receiptDate', DATE_FORMAT(LEAST(DATE_ADD(o.order_date, INTERVAL 15 DAY), CURDATE()), '%Y-%m-%d'),
             'operator', '刘洋',
             'note', '历史订单回款补录',
             'owner', r.owner)
    FROM crm_module_records r
    JOIN crm_orders o ON o.order_no = JSON_UNQUOTE(JSON_EXTRACT(r.payload, '$.orderNo'))
    WHERE r.module_name = 'receivables'
      AND JSON_EXTRACT(r.payload, '$.received') > 0
      AND NOT EXISTS (SELECT 1 FROM crm_module_records x
                      WHERE x.module_name = 'receipts'
                        AND JSON_UNQUOTE(JSON_EXTRACT(x.payload, '$.orderNo')) = o.order_no);

    -- 3) 按收款单合计回写应收（收款单是事实，应收是派生）
    UPDATE crm_module_records r
    JOIN (SELECT JSON_UNQUOTE(JSON_EXTRACT(payload, '$.orderNo')) AS no,
                 SUM(JSON_EXTRACT(payload, '$.amount')) AS paid
          FROM crm_module_records
          WHERE module_name = 'receipts'
          GROUP BY no) c ON c.no = JSON_UNQUOTE(JSON_EXTRACT(r.payload, '$.orderNo'))
    SET r.payload = JSON_SET(r.payload, '$.received',
                             LEAST(CAST(c.paid AS DECIMAL(14, 2)),
                                   CAST(JSON_EXTRACT(r.payload, '$.total') AS DECIMAL(14, 2))))
    WHERE r.module_name = 'receivables';

    INSERT INTO crm_seed_batches(name, note)
    VALUES ('v21_finance_reconcile', '历史订单补应收 + 补收款 + 应收按收款合计回写');
  END IF;
END$$
DELIMITER ;

CALL reconcile_v21_finance();
DROP PROCEDURE reconcile_v21_finance;

-- 对账结果：应全为 0
SELECT '无应收的未取消订单' AS 检查项, COUNT(*) AS 数量
FROM crm_orders o
WHERE o.status <> '已取消'
  AND NOT EXISTS (SELECT 1 FROM crm_module_records m
                  WHERE m.module_name = 'receivables'
                    AND JSON_UNQUOTE(JSON_EXTRACT(m.payload, '$.orderNo')) = o.order_no)
UNION ALL
SELECT '应收与收款不符', COUNT(*)
FROM crm_module_records r
LEFT JOIN (SELECT JSON_UNQUOTE(JSON_EXTRACT(payload, '$.orderNo')) AS no,
                  SUM(JSON_EXTRACT(payload, '$.amount')) AS paid
           FROM crm_module_records WHERE module_name = 'receipts' GROUP BY no) c
       ON c.no = JSON_UNQUOTE(JSON_EXTRACT(r.payload, '$.orderNo'))
WHERE r.module_name = 'receivables'
  AND ABS(IFNULL(c.paid, 0) - JSON_EXTRACT(r.payload, '$.received')) > 0.01
  AND JSON_EXTRACT(r.payload, '$.received') < JSON_EXTRACT(r.payload, '$.total');
