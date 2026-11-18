-- =====================================================================
-- v15 待办按人隔离 + 按岗位初始化
--
-- 背景：待办原先按「模块」播种一份通用数据，再走数据范围过滤。
--       数据范围是 ALL 的账号（营销、财务、售后、生产、总监）会看到
--       其他销售名下的待办，于是营销的待办里出现了「合同回款」「报价审批」，
--       和岗位职责完全对不上。
--
-- 本次调整：
--   1. 后端 crm_module_records 里 module_name='todos' 的数据只按 owner 过滤，
--      数据范围不再生效（管理员的 ALL 也看不到别人的待办）
--   2. 种子按「人」记录：crm_module_seed_state.module_name = 'todos@姓名'
--   3. 待办内容由前端 config/roleProfile.js 按岗位生成：
--        营销   → 线索分配 / 线索复核 / 渠道活动 / 市场复盘
--        财务   → 回款催收 / 到账核销 / 开票申请 / 月末对账
--        售后   → 工单受理 / 客诉处理 / 退换货处理 / 客户回访
--        生产   → 排产计划 / 备货补货 / 发货安排 / 退换货处理
--        销售   → 目标复盘 / 大单推进 / 报价审批 / 客户跟进
--
-- 脚本可重复执行。
-- =====================================================================

USE crm;

-- 按 (模块, 负责人) 查待办是高频操作，补一个索引；已存在则跳过
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'crm_module_records'
    AND index_name = 'idx_module_owner'
);

SET @sql := IF(
  @idx_exists = 0,
  'ALTER TABLE crm_module_records ADD INDEX idx_module_owner (module_name, owner)',
  'SELECT ''index idx_module_owner already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 旧的按模块播种标记已经失效（现在按人播种），清掉不影响任何业务数据
DELETE FROM crm_module_seed_state WHERE module_name = 'todos';

-- 校验：每个角色应有的待办类型（与前端 roleProfile.js 保持一致，仅作对照用）
SELECT 'ADMIN' AS role, '权限复核 / 账号交接 / 数据治理' AS todo_types
UNION ALL SELECT 'MARKET', '线索分配 / 线索复核 / 渠道活动 / 市场复盘'
UNION ALL SELECT 'FINANCE', '回款催收 / 到账核销 / 开票申请 / 月末对账'
UNION ALL SELECT 'SERVICE', '工单受理 / 客诉处理 / 退换货处理 / 客户回访'
UNION ALL SELECT 'PRODUCTION', '排产计划 / 备货补货 / 发货安排 / 退换货处理'
UNION ALL SELECT 'SALES', '目标复盘 / 大单推进 / 报价审批 / 团队辅导'
UNION ALL SELECT 'SALES_G1/G2', '线索派发 / 组员陪跑 / 商机推进 / 报价审核';
