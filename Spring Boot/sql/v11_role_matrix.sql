-- 全角色功能权限对齐岗位职责（v10 收口了营销，这里把财务 / 售后 / 生产一起理顺）
-- ---------------------------------------------------------------------------
-- 口径：功能权限决定「能看到哪个菜单、能点哪个按钮」，数据范围决定「能看到谁的数据」，
-- 字段权限决定「同一条记录里哪些字段不能看」。这里只动第一层。
--
-- 三个岗位的问题与调整：
--
-- 财务（FINANCE）
--   ✗ 能新建订单：建单是销售的动作，财务只做审单、作废与收款
--   ✓ 补商品目录（product:view）：成本核算要对着商品看，原来只有进价权限却没有商品页
--   ✓ 补产品销售分析（stats:product:view）：毛利与品类结构是财务的分析口径
--   保留：经营概览（签单额 / 回款进度）、销售统计、客户采购分析、订单、报价单（核价）、
--         发货记录（只读）、收款、应收核销、退换货（退款）
--
-- 售后（SERVICE）
--   ✗ 经营概览：公司级签单大盘，售后不背销售指标
--   ✗ 销售商机：商机是销售的管线，售后看不到也不需要看
--   ✗ 产品销售分析：销量分析不能指导维修，售后看的是工单与故障
--   ✓ 补退换货查看与处理（return:view / return:edit）：退换货本来就是售后在办
--   保留：客户档案（可改联系方式与地址）、联系人、跟进记录、订单（保修依据）、
--         商品（技术资料）、发货记录、工单全流程、客户回访
--
-- 生产（PRODUCTION）
--   ✗ 经营概览：签单额与生产排产无关
--   ✗ 客户档案 / 联系人：生产只需要发货信息，不需要客户资产
--   保留：订单（看要发什么）、商品与库存、进价（成本由生产维护）、发货登记、退换货接收、
--         产品销售分析（备货依据）
--
-- 销售总监 / 销售组长 / 顾问沿用 v8 的收敛结果，本次不动。
-- 幂等：全部是「缺则补、多则删」，可重复执行。
-- ---------------------------------------------------------------------------

USE crm;

-- ---------------- 财务 ----------------
DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code = 'FINANCE'
  AND p.code IN ('order:add');

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('product:view', 'stats:product:view')
WHERE r.code = 'FINANCE'
  AND NOT EXISTS (SELECT 1 FROM role_permissions x WHERE x.role_id = r.id AND x.permission_id = p.id);

-- ---------------- 售后 ----------------
DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code = 'SERVICE'
  AND p.code IN ('dashboard:view', 'opportunity:view', 'stats:product:view');

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('return:view', 'return:edit')
WHERE r.code = 'SERVICE'
  AND NOT EXISTS (SELECT 1 FROM role_permissions x WHERE x.role_id = r.id AND x.permission_id = p.id);

-- ---------------- 生产 ----------------
DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code = 'PRODUCTION'
  AND p.code IN ('dashboard:view', 'customer:view', 'contact:view');

-- ---------------- 结果核对 ----------------
SELECT r.code AS role, COUNT(*) AS perms,
       GROUP_CONCAT(p.code ORDER BY p.sort_order SEPARATOR ' ') AS permission_list
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
GROUP BY r.code
ORDER BY r.id;
