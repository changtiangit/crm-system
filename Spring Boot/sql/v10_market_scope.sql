-- 营销（市场）角色的功能权限收口
-- ---------------------------------------------------------------------------
-- 营销岗的职责是「拉新 + 分发 + 渠道复盘」：拿到线索、判断质量、分配给销售、
-- 盯渠道转化，不参与签单、交付、回款，也不拥有客户档案。原来的 MARKET 角色
-- 因为 data_scope=ALL 又握着 customer/order/quote/售后/经营概览等权限，
-- 登录后能看到全公司的客户与签单金额，属于典型的越权。
--
-- 收敛后的营销权限（9 项）：
--   工作台    todo:view / todo:add / todo:done        我的待办（营销自己的事）
--   线索      clue:view / clue:add / clue:edit / clue:assign
--   团队      sales:team:view                          分配线索要看清谁在接、还剩多少配额
--   数据统计  stats:market:view                        新增：市场分析（渠道/漏斗/趋势）
--
-- 明确回收：经营概览（公司级大盘）、销售统计/客户采购分析/产品销售分析、
-- 客户档案与联系人、跟进记录、商机、报价、订单、交付与回款、售后工单/退换货/回访、
-- 客户公海（公海是客户资产，营销不领客户）、商品目录与进价。
--
-- 幂等：权限点、授权、回收都可重复执行，不会覆盖角色管理里的后续手工调整。
-- ---------------------------------------------------------------------------

USE crm;

-- 1) 新增「市场分析」权限点：营销唯一的数据报表入口，口径是线索而非订单
INSERT INTO permissions(code, name, module, type, sort_order)
SELECT 'stats:market:view', '查看市场分析', '数据统计', 'menu', 54
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'stats:market:view');

-- 2) 市场分析只给管理员与营销：
--    销售部（含总监、组长、顾问）此前已按要求收回数据统计权限，这张表也不开给销售，
--    免得销售侧又冒出一个「数据统计」菜单。真要给总监看，在「权限授权」里勾一下即可。
DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code = 'SALES' AND p.code = 'stats:market:view';

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'stats:market:view'
WHERE r.code IN ('ADMIN', 'MARKET')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = r.id AND x.permission_id = p.id
  );

-- 3) 回收营销的越权功能点
DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code = 'MARKET'
  AND p.code IN (
    'dashboard:view',          -- 经营概览：签单额、回款进度、业绩排行
    'stats:sales:view',        -- 销售统计
    'stats:customer:view',     -- 客户采购分析（含客户明细与采购额）
    'stats:product:view',      -- 产品销售分析
    'customer:view',           -- 客户档案
    'customer:add',            -- 新建客户（建档是销售跟进后的动作）
    'contact:view', 'contact:add', 'contact:edit',
    'followup:view', 'followup:add',
    'opportunity:view',
    'quote:view',
    'order:view',
    'pool:view', 'pool:claim', -- 客户公海
    'ticket:view', 'return:view', 'revisit:view',
    'product:view'             -- 商品目录（含售价，属销售/生产资料）
  );

-- 4) 补齐营销应有权限（线索为主，加团队与市场分析）
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
  'todo:view', 'todo:add', 'todo:done',
  'clue:view', 'clue:add', 'clue:edit', 'clue:assign',
  'sales:team:view',
  'stats:market:view'
)
WHERE r.code = 'MARKET'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = r.id AND x.permission_id = p.id
  );

-- 5) 营销的数据范围只需覆盖「全部线索」；客户/订单等资源已无权限，ALL 不会外溢。
--    这里显式写死，避免以后按岗位推导又放宽。
UPDATE employees SET data_scope = 'ALL' WHERE employee_no IN ('EMP004', 'EMP005');

-- ---------------------------------------------------------------------------
-- 6) 演示数据：补 6-10 月线索，让渠道趋势与转化率有连续 6 个月的口径
--    用 assign_by = '市场活动导入' 作为标记，重复执行不会重复插入。
-- ---------------------------------------------------------------------------
INSERT INTO crm_clues (name, contact, phone, source, intent, status, owner, assign_by, assign_time, create_time)
SELECT x.name, x.contact, x.phone, x.source, x.intent, x.status, x.owner,
       '市场活动导入', x.assign_time, x.create_time
FROM (
  SELECT '宏达汽修-机油滤芯年采' AS name, '李伟' AS contact, '138-2211-3344' AS phone, '官网表单' AS source, '中' AS intent, '已转化' AS status, '张明' AS owner, '2026-06-02 15:20:00' AS assign_time, '2026-06-02 09:10:00' AS create_time
  UNION ALL SELECT '星辉快保-刹车油补货', '王倩', '139-3322-5566', '展会', '低', '已废弃', '李思', '2026-06-05 14:05:00', '2026-06-05 10:30:00'
  UNION ALL SELECT '顺通物流车队-柴机油集采', '张强', '137-4455-6677', '老客户推荐', '高', '已转化', '张明', '2026-06-09 11:40:00', '2026-06-09 08:20:00'
  UNION ALL SELECT '车之家养护-空调滤芯季度单', '刘敏', '136-5566-7788', '电话咨询', '中', '跟进中', '周婷', '2026-06-15 16:30:00', '2026-06-15 11:00:00'
  UNION ALL SELECT '万顺汽配-火花塞分销', '陈刚', '135-6677-8899', '广告投放', '中', '跟进中', '李思', '2026-06-22 13:15:00', '2026-06-22 09:45:00'
  UNION ALL SELECT '优驰汽车-镀晶耗材试用', '杨帆', '134-7788-9900', '官网表单', '低', '已废弃', '周婷', '2026-06-27 17:00:00', '2026-06-27 12:20:00'

  UNION ALL SELECT '恒通维修-底盘件供货', '赵磊', '138-8899-0011', '展会', '高', '已转化', '方宇', '2026-07-03 10:10:00', '2026-07-03 08:40:00'
  UNION ALL SELECT '路安车队-防冻液备货', '孙丽', '139-9900-1122', '老客户推荐', '中', '跟进中', '林晓', '2026-07-06 15:45:00', '2026-07-06 10:15:00'
  UNION ALL SELECT '新程汽修-雨刮片年采', '周鹏', '137-1011-2233', '电话咨询', '中', '跟进中', '何俊', '2026-07-10 09:30:00', '2026-07-10 08:50:00'
  UNION ALL SELECT '车美佳-贴膜工具询价', '吴静', '136-1122-3344', '广告投放', '低', '已废弃', '苏晴', '2026-07-14 14:20:00', '2026-07-14 11:05:00'
  UNION ALL SELECT '中天汽贸-美容品配套', '郑昊', '135-2233-4455', '官网表单', '高', '已转化', '黄志远', '2026-07-18 11:05:00', '2026-07-18 09:00:00'
  UNION ALL SELECT '四方快修-点火线圈试单', '冯雪', '134-3344-5566', '展会', '中', '已废弃', '陈嘉伟', '2026-07-23 16:40:00', '2026-07-23 10:40:00'
  UNION ALL SELECT '长运车队-润滑脂季度单', '蒋涛', '138-4455-6677', '老客户推荐', '中', '已转化', '张明', '2026-07-28 13:50:00', '2026-07-28 09:20:00'

  UNION ALL SELECT '车管家连锁-洗车耗材月补', '沈琳', '139-5566-7788', '广告投放', '中', '跟进中', '李思', '2026-08-04 15:10:00', '2026-08-04 09:30:00'
  UNION ALL SELECT '瑞丰汽修-变速箱油招标', '韩磊', '137-6677-8899', '展会', '高', '已转化', '周婷', '2026-08-08 10:25:00', '2026-08-08 08:15:00'
  UNION ALL SELECT '安达物流-三滤集中采购', '曹阳', '136-7788-9900', '官网表单', '高', '已转化', '方宇', '2026-08-12 14:05:00', '2026-08-12 09:50:00'
  UNION ALL SELECT '车世界-轮毂采购', '谢婉', '135-8899-0011', '电话咨询', '中', '已废弃', '林晓', '2026-08-17 17:20:00', '2026-08-17 13:40:00'
  UNION ALL SELECT '易车行-免拆清洗设备', '卢明', '134-9900-1122', '广告投放', '低', '跟进中', '何俊', '2026-08-21 11:35:00', '2026-08-21 09:05:00'
  UNION ALL SELECT '嘉信汽配-皮带轮分销', '邓超', '138-1011-2233', '老客户推荐', '中', '已废弃', '苏晴', '2026-08-26 15:55:00', '2026-08-26 10:25:00'

  UNION ALL SELECT '明辉快修-机油三滤年采', '范冰', '139-1122-3344', '官网表单', '高', '已转化', '黄志远', '2026-09-02 09:45:00', '2026-09-02 08:30:00'
  UNION ALL SELECT '通达车队-柴油机油集采', '石磊', '137-2233-4455', '展会', '中', '跟进中', '陈嘉伟', '2026-09-07 13:20:00', '2026-09-07 10:00:00'
  UNION ALL SELECT '车之友-刹车片供货', '袁莉', '136-3344-5566', '电话咨询', '中', '跟进中', '张明', '2026-09-11 16:10:00', '2026-09-11 11:20:00'
  UNION ALL SELECT '骏驰汽修-空调系统养护', '崔浩', '135-4455-6677', '广告投放', '低', '已废弃', '李思', '2026-09-15 14:50:00', '2026-09-15 11:50:00'
  UNION ALL SELECT '中环汽贸-原厂替代件试供', '唐宁', '134-5566-7788', '展会', '高', '已转化', '周婷', '2026-09-19 10:40:00', '2026-09-19 08:10:00'
  UNION ALL SELECT '佳运车队-防冻液冬储', '贾斌', '138-6677-8899', '老客户推荐', '中', '已废弃', '方宇', '2026-09-23 15:30:00', '2026-09-23 09:55:00'
  UNION ALL SELECT '鼎盛维修-底盘件长期单', '白雪', '139-7788-9900', '官网表单', '中', '已转化', '林晓', '2026-09-28 11:15:00', '2026-09-28 08:45:00'

  UNION ALL SELECT '车生活-镀晶耗材补货', '顾强', '137-8899-0011', '广告投放', '中', '跟进中', '何俊', '2026-10-03 14:35:00', '2026-10-03 10:05:00'
  UNION ALL SELECT '迅达快保-雨刮片季采', '严雪', '136-9900-1122', '电话咨询', '中', '跟进中', '苏晴', '2026-10-07 09:55:00', '2026-10-07 08:25:00'
  UNION ALL SELECT '万邦汽配-火花塞分销', '罗刚', '135-1011-2233', '展会', '高', '已转化', '黄志远', '2026-10-12 13:40:00', '2026-10-12 09:15:00'
  UNION ALL SELECT '顺捷车队-润滑脂补货', '于洋', '134-1122-3344', '老客户推荐', '中', '已废弃', '陈嘉伟', '2026-10-16 16:20:00', '2026-10-16 11:30:00'
  UNION ALL SELECT '新元汽修-点火线圈试单', '董倩', '138-2233-4455', '官网表单', '低', '已转化', '张明', '2026-10-20 10:50:00', '2026-10-20 08:35:00'
  UNION ALL SELECT '车驿站-洗车耗材月补', '程磊', '139-3344-5566', '广告投放', '中', '已转化', '李思', '2026-10-24 15:15:00', '2026-10-24 09:40:00'
  UNION ALL SELECT '恒达物流-三滤集采', '潘婷', '137-4455-6677', '展会', '高', '跟进中', '周婷', '2026-10-27 11:25:00', '2026-10-27 08:55:00'
  UNION ALL SELECT '康盛汽修-刹车片询价', '秦昊', '136-5566-7788', '电话咨询', '中', '待跟进', '方宇', '2026-10-30 14:10:00', '2026-10-30 10:20:00'
) x
WHERE NOT EXISTS (SELECT 1 FROM crm_clues WHERE assign_by = '市场活动导入');

-- 7) 结果核对：营销的功能点清单 + 线索按月分布
SELECT p.module, p.code, p.name
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code = 'MARKET'
ORDER BY p.module, p.sort_order;

SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COUNT(*) AS leads,
       SUM(status = '已转化') AS converted
FROM crm_clues
GROUP BY 1 ORDER BY 1;
