// 各业务模块的演示数据（可在页面内直接增删改，刷新后还原）
// 业务背景：面向汽修 / 洗车门店销售保养滤芯、油液、点火电气、制动底盘等小件耗材，
// 所以客户都是 4S 店、综合维修厂、洗车连锁、汽车美容店与汽配经销商；
// 客户是公司，签合同、收货对接的是具体的人，因此订单上单独记录「联系人」。

export const clues = [
  { id: 1, name: '连锁门店保养滤芯年度采购', contact: '陈晓峰', phone: '138-1024-5566', source: '官网表单', status: '跟进中', owner: '张明', createTime: '2026-11-08 09:32' },
  { id: 2, name: '二类维修厂刹车片供货询价', contact: '王莉', phone: '139-2288-7711', source: '展会', status: '待跟进', owner: '李思', createTime: '2026-11-08 11:05' },
  { id: 3, name: '洗车连锁清洁养护耗材试用', contact: '刘子豪', phone: '137-6600-3322', source: '广告投放', status: '跟进中', owner: '张明', createTime: '2026-11-07 15:48' },
  { id: 4, name: '4S 店原厂替代件长期供货', contact: '赵敏', phone: '135-8899-1200', source: '老客户推荐', status: '已转化', owner: '李思', createTime: '2026-11-06 10:20' },
  { id: 5, name: '车队机油与三滤集中采购', contact: '孙浩', phone: '136-7788-4455', source: '电话咨询', status: '待跟进', owner: '周婷', createTime: '2026-11-05 16:10' },
  { id: 6, name: '汽车美容店镀晶耗材补货', contact: '吴倩', phone: '188-5566-7788', source: '官网表单', status: '已废弃', owner: '周婷', createTime: '2026-11-04 09:15' },
  { id: 7, name: '汽配城二级分销合作洽谈', contact: '郑凯', phone: '131-2233-9900', source: '展会', status: '跟进中', owner: '张明', createTime: '2026-11-03 14:02' },
  { id: 8, name: '快修连锁变速箱油招标', contact: '黄磊', phone: '150-6677-2233', source: '老客户推荐', status: '待跟进', owner: '李思', createTime: '2026-11-02 17:26' }
]

export const customers = [
  { id: 1, name: '杭州车匠汽车维修连锁', industry: '汽车维修', level: 'A', contact: '陈晓峰', phone: '138-1024-5566', owner: '张明', createTime: '2026-09-12' },
  { id: 2, name: '上海睿联汽车服务集团', industry: '4S 店', level: 'A', contact: '王强', phone: '139-2288-7711', owner: '李思', createTime: '2026-08-28' },
  { id: 3, name: '深圳前海车享快修连锁', industry: '汽车维修', level: 'B', contact: '林雅', phone: '137-6600-3322', owner: '张明', createTime: '2026-10-06' },
  { id: 4, name: '北京博远汽车美容中心', industry: '汽车美容', level: 'B', contact: '赵敏', phone: '135-8899-1200', owner: '周婷', createTime: '2026-07-19' },
  { id: 5, name: '成都康泰汽配经销部', industry: '汽配经销', level: 'C', contact: '孙浩', phone: '136-7788-4455', owner: '李思', createTime: '2026-10-21' },
  { id: 6, name: '广州速达洗车连锁', industry: '洗车连锁', level: 'B', contact: '黄磊', phone: '150-6677-2233', owner: '张明', createTime: '2026-06-30' },
  { id: 7, name: '苏州精工汽车维修厂', industry: '汽车维修', level: 'C', contact: '周天成', phone: '188-1122-3344', owner: '周婷', createTime: '2026-11-01' }
]

export const opportunities = [
  { id: 1, name: '车匠连锁-保养滤芯年度框架', customer: '杭州车匠汽车维修连锁', amount: 68000, stage: '方案报价', expectDate: '2026-12-15', owner: '张明' },
  { id: 2, name: '睿联集团-制动底盘件集采', customer: '上海睿联汽车服务集团', amount: 125000, stage: '商务谈判', expectDate: '2026-12-05', owner: '李思' },
  { id: 3, name: '车享快修-变速箱油招标', customer: '深圳前海车享快修连锁', amount: 42000, stage: '需求确认', expectDate: '2027-01-20', owner: '张明' },
  { id: 4, name: '博远美容-清洁养护耗材补货', customer: '北京博远汽车美容中心', amount: 26000, stage: '初步沟通', expectDate: '2027-02-10', owner: '周婷' },
  { id: 5, name: '速达洗车-洗车液与毛巾年度供货', customer: '广州速达洗车连锁', amount: 89000, stage: '方案报价', expectDate: '2026-12-28', owner: '张明' },
  { id: 6, name: '康泰汽配-点火电气件分销', customer: '成都康泰汽配经销部', amount: 17500, stage: '输单', expectDate: '2026-11-10', owner: '李思' },
  { id: 7, name: '精工维修-制动片试销转正', customer: '苏州精工汽车维修厂', amount: 31000, stage: '赢单', expectDate: '2026-11-25', owner: '周婷' }
]

// ---------------- 商品目录（售价与「商品」模块保持一致，用于算订单明细与毛利） ----------------
// [名称, 分类, 售价, 进价]，只列会走订单的主销品，商品模块里还有更全的 57 个 SKU
export const catalog = [
  ['机油滤芯', '保养滤芯类', 25, 12],
  ['空气滤芯', '保养滤芯类', 35, 18],
  ['空调滤芯', '保养滤芯类', 45, 22],
  ['燃油滤芯', '保养滤芯类', 52, 25],
  ['全合成机油', '油液类', 228, 130],
  ['变速箱油', '油液类', 88, 45],
  ['刹车油', '油液类', 48, 25],
  ['防冻液', '油液类', 68, 38],
  ['玻璃水', '油液类', 12, 6],
  ['火花塞', '点火/电气小件', 58, 28],
  ['近光灯泡', '点火/电气小件', 26, 12],
  ['继电器', '点火/电气小件', 20, 9],
  ['保险丝', '点火/电气小件', 22, 10],
  ['刹车片', '制动/底盘小件', 138, 68],
  ['刹车分泵修理包', '制动/底盘小件', 45, 20],
  ['防尘套', '制动/底盘小件', 30, 14],
  ['刹车报警线', '制动/底盘小件', 20, 9],
  ['雨刮片', '车身/橡胶小件', 38, 18],
  ['洗车液', '清洁养护耗材', 52, 26],
  ['内饰清洗剂', '清洁养护耗材', 32, 15],
  ['镀膜剂', '清洁养护耗材', 58, 28],
  ['轮胎蜡', '清洁养护耗材', 38, 18],
  ['擦车毛巾', '清洁养护耗材', 12, 5],
  ['清洁海绵', '清洁养护耗材', 8, 3]
]

const PRICE = Object.fromEntries(catalog.map(([n, c, p, cost]) => [n, { category: c, price: p, cost }]))

/** 一行订单明细：金额与毛利都由商品单价算出来，不手填，避免和商品资料对不上 */
function line(product, qty) {
  const p = PRICE[product]
  return {
    product,
    category: p.category,
    qty,
    price: p.price,
    cost: p.cost,
    amount: p.price * qty,
    profit: (p.price - p.cost) * qty
  }
}

/** 造一张订单：金额 = 明细合计，所以订单列表、商品分析、客户采购分析三处永远对得上 */
function mkOrder(id, orderNo, customerNo, owner, orderDate, status, items) {
  const c = customers[customerNo]
  const lines = items.map(([n, q]) => line(n, q))
  return {
    id,
    orderNo,
    customer: c.name,
    contact: c.contact,
    phone: c.phone,
    amount: lines.reduce((s, l) => s + l.amount, 0),
    status,
    orderDate,
    owner,
    items: lines
  }
}

// customers 的下标别名，写订单时不必反复抄客户全名
const 车匠 = 0
const 睿联 = 1
const 车享 = 2
const 博远 = 3
const 康泰 = 4
const 速达 = 5
const 精工 = 6

export const orders = [
  mkOrder(1, 'SO20261108001', 精工, '周婷', '2026-11-08', '已付款', [['刹车片', 20], ['机油滤芯', 12], ['玻璃水', 5]]),
  mkOrder(2, 'SO20261105002', 博远, '周婷', '2026-11-05', '部分回款', [['镀膜剂', 90], ['洗车液', 120], ['擦车毛巾', 100], ['清洁海绵', 25]]),
  mkOrder(3, 'SO20261028003', 速达, '张明', '2026-10-28', '待付款', [['洗车液', 500], ['擦车毛巾', 800], ['镀膜剂', 100], ['内饰清洗剂', 100], ['清洁海绵', 125]]),
  mkOrder(4, 'SO20261015004', 睿联, '李思', '2026-10-15', '已完成', [['刹车片', 300], ['变速箱油', 200], ['火花塞', 150], ['全合成机油', 100], ['空气滤芯', 150], ['空调滤芯', 50]]),
  mkOrder(5, 'SO20261009005', 车享, '张明', '2026-10-09', '已付款', [['变速箱油', 150], ['全合成机油', 20], ['机油滤芯', 120], ['空气滤芯', 32], ['玻璃水', 10]]),
  mkOrder(6, 'SO20260926006', 康泰, '李思', '2026-09-26', '已取消', [['火花塞', 100], ['继电器', 140]]),
  mkOrder(7, 'SO20261110007', 车匠, '张明', '2026-11-10', '部分回款', [['机油滤芯', 600], ['空气滤芯', 300], ['空调滤芯', 150]]),
  mkOrder(8, 'SO20261026008', 车匠, '张明', '2026-10-26', '已完成', [['机油滤芯', 400], ['空气滤芯', 100], ['变速箱油', 55]]),
  mkOrder(9, 'SO20261020009', 博远, '周婷', '2026-10-20', '已付款', [['镀膜剂', 100], ['洗车液', 50], ['擦车毛巾', 100]]),
  mkOrder(10, 'SO20260918010', 速达, '张明', '2026-09-18', '已完成', [['洗车液', 300], ['擦车毛巾', 500], ['清洁海绵', 400], ['内饰清洗剂', 100]]),
  mkOrder(11, 'SO20260912011', 精工, '周婷', '2026-09-12', '已付款', [['刹车片', 30], ['刹车报警线', 60], ['防尘套', 30]]),
  mkOrder(12, 'SO20260828012', 睿联, '李思', '2026-08-28', '已完成', [['刹车片', 250], ['变速箱油', 200], ['火花塞', 150], ['全合成机油', 66]]),
  mkOrder(13, 'SO20260815013', 车享, '张明', '2026-08-15', '已完成', [['变速箱油', 100], ['火花塞', 40], ['保险丝', 80]]),
  mkOrder(14, 'SO20260720014', 车匠, '张明', '2026-07-20', '已完成', [['洗车液', 200], ['擦车毛巾', 400], ['镀膜剂', 100], ['内饰清洗剂', 100]]),
  mkOrder(15, 'SO20260706015', 车匠, '张明', '2026-07-06', '已完成', [['机油滤芯', 800], ['空气滤芯', 300], ['空调滤芯', 200]]),
  mkOrder(16, 'SO20260714016', 睿联, '李思', '2026-07-14', '已完成', [['刹车片', 200], ['变速箱油', 150]]),
  mkOrder(17, 'SO20260727017', 速达, '张明', '2026-07-27', '已完成', [['洗车液', 400], ['擦车毛巾', 600]]),
  mkOrder(18, 'SO20260709018', 车享, '张明', '2026-07-09', '已完成', [['全合成机油', 100], ['机油滤芯', 300], ['空气滤芯', 100]]),
  mkOrder(19, 'SO20260730019', 精工, '周婷', '2026-07-30', '已完成', [['刹车片', 150], ['刹车油', 100], ['防冻液', 50]]),
  mkOrder(20, 'SO20260803020', 博远, '周婷', '2026-08-03', '已完成', [['镀膜剂', 120], ['洗车液', 150], ['擦车毛巾', 300], ['轮胎蜡', 100]]),
  mkOrder(21, 'SO20260811021', 车匠, '张明', '2026-08-11', '已完成', [['机油滤芯', 600], ['空气滤芯', 250], ['燃油滤芯', 150]]),
  mkOrder(22, 'SO20260819022', 速达, '张明', '2026-08-19', '已完成', [['洗车液', 500], ['擦车毛巾', 800]]),
  mkOrder(23, 'SO20260824023', 康泰, '李思', '2026-08-24', '已完成', [['火花塞', 100], ['继电器', 100], ['近光灯泡', 200]]),
  mkOrder(24, 'SO20260905024', 精工, '周婷', '2026-09-05', '已完成', [['刹车片', 120], ['刹车油', 60], ['雨刮片', 60]]),
  mkOrder(25, 'SO20260921025', 睿联, '李思', '2026-09-21', '已完成', [['刹车片', 300], ['变速箱油', 250], ['火花塞', 100], ['空气滤芯', 200]]),
  mkOrder(26, 'SO20260908026', 车匠, '张明', '2026-09-08', '已完成', [['机油滤芯', 500], ['空调滤芯', 200], ['空气滤芯', 200]]),
  mkOrder(27, 'SO20260928027', 车享, '张明', '2026-09-28', '已完成', [['全合成机油', 120], ['机油滤芯', 200], ['玻璃水', 100]]),
  mkOrder(28, 'SO20261012028', 康泰, '李思', '2026-10-12', '已完成', [['火花塞', 150], ['继电器', 200], ['近光灯泡', 200]]),
  mkOrder(29, 'SO20261022029', 精工, '周婷', '2026-10-22', '已完成', [['刹车片', 100], ['刹车分泵修理包', 60], ['刹车油', 50]]),
  mkOrder(30, 'SO20261112030', 睿联, '李思', '2026-11-12', '待付款', [['变速箱油', 300], ['刹车片', 200], ['火花塞', 100]]),
  mkOrder(31, 'SO20261102031', 车享, '张明', '2026-11-02', '已完成', [['全合成机油', 80], ['机油滤芯', 200], ['玻璃水', 50]]),
  mkOrder(32, 'SO20261106032', 速达, '张明', '2026-11-06', '待付款', [['洗车液', 200], ['擦车毛巾', 300], ['镀膜剂', 50], ['清洁海绵', 200]]),
  mkOrder(33, 'SO20260610033', 睿联, '李思', '2026-06-10', '已完成', [['刹车片', 250], ['变速箱油', 200], ['空气滤芯', 200]]),
  mkOrder(34, 'SO20260618034', 车匠, '张明', '2026-06-18', '已完成', [['机油滤芯', 700], ['空调滤芯', 300], ['空气滤芯', 300]]),
  mkOrder(35, 'SO20260624035', 速达, '张明', '2026-06-24', '已完成', [['洗车液', 400], ['擦车毛巾', 600], ['镀膜剂', 80]]),
  mkOrder(36, 'SO20260606036', 车享, '张明', '2026-06-06', '已完成', [['全合成机油', 100], ['机油滤芯', 200]]),
  mkOrder(37, 'SO20260628037', 精工, '周婷', '2026-06-28', '已完成', [['刹车片', 100], ['刹车油', 80], ['防冻液', 40]])
]


// ---------------- 客户公海 ----------------
// 长期未跟进、离职交接、主动退回的客户会回到公海，销售可自助领取
export const poolCustomers = [
  { id: 101, name: '宁波德众汽车服务有限公司', industry: '汽车维修', level: 'B', city: '宁波', source: '展会', contact: '张伟', phone: '137-5566-1122', lastFollow: '2026-09-18', idleDays: 55, reason: '超过 30 天未跟进' },
  { id: 102, name: '无锡捷通汽车养护中心', industry: '汽车美容', level: 'C', city: '无锡', source: '官网表单', contact: '钱磊', phone: '139-6677-8899', lastFollow: '2026-09-30', idleDays: 43, reason: '超过 30 天未跟进' },
  { id: 103, name: '青岛海润 4S 店集团', industry: '4S 店', level: 'A', city: '青岛', source: '老客户推荐', contact: '刘建国', phone: '138-8899-2200', lastFollow: '2026-10-02', idleDays: 41, reason: '销售离职交接' },
  { id: 104, name: '郑州顺达汽配商行', industry: '汽配经销', level: 'B', city: '郑州', source: '电话咨询', contact: '马涛', phone: '136-2233-4455', lastFollow: '2026-10-11', idleDays: 32, reason: '超过 30 天未跟进' },
  { id: 105, name: '合肥安驰车队服务有限公司', industry: '车队物流', level: 'B', city: '合肥', source: '展会', contact: '高翔', phone: '135-7788-9900', lastFollow: '2026-10-15', idleDays: 28, reason: '客户主动退回' },
  { id: 106, name: '武汉三镇洗车连锁', industry: '洗车连锁', level: 'C', city: '武汉', source: '广告投放', contact: '徐静', phone: '150-3344-5566', lastFollow: '2026-10-19', idleDays: 24, reason: '报价后无回应' },
  { id: 107, name: '长沙湘江汽车维修厂', industry: '汽车维修', level: 'C', city: '长沙', source: '官网表单', contact: '彭勇', phone: '188-6677-3344', lastFollow: '2026-10-24', idleDays: 19, reason: '客户主动退回' },
  { id: 108, name: '西安秦川汽配经销中心', industry: '汽配经销', level: 'B', city: '西安', source: '老客户推荐', contact: '崔斌', phone: '131-5566-7788', lastFollow: '2026-10-28', idleDays: 15, reason: '超过 30 天未跟进' }
]

// ---------------- 联系人 ----------------
// 客户是公司，联系人才是签字、收货、对账的具体人；一个客户可有多个联系人
export const contacts = [
  { id: 1, name: '陈晓峰', customer: '杭州车匠汽车维修连锁', title: '采购经理', phone: '138-1024-5566', wechat: 'chenxf_auto', primary: '是', owner: '张明' },
  { id: 2, name: '王强', customer: '上海睿联汽车服务集团', title: '集团采购总监', phone: '139-2288-7711', wechat: 'wq1392288', primary: '是', owner: '李思' },
  { id: 3, name: '李娜', customer: '上海睿联汽车服务集团', title: '仓储主管', phone: '139-2288-7712', wechat: 'lina_wms', primary: '否', owner: '李思' },
  { id: 4, name: '林雅', customer: '深圳前海车享快修连锁', title: '运营总监', phone: '137-6600-3322', wechat: 'linya_chexiang', primary: '是', owner: '张明' },
  { id: 5, name: '赵敏', customer: '北京博远汽车美容中心', title: '店长', phone: '135-8899-1200', wechat: 'zhaomin_boyuan', primary: '是', owner: '周婷' },
  { id: 6, name: '孙浩', customer: '成都康泰汽配经销部', title: '老板', phone: '136-7788-4455', wechat: 'sunhao_kt', primary: '是', owner: '李思' },
  { id: 7, name: '黄磊', customer: '广州速达洗车连锁', title: '采购主管', phone: '150-6677-2233', wechat: 'huanglei_sd', primary: '是', owner: '张明' },
  { id: 8, name: '周天成', customer: '苏州精工汽车维修厂', title: '厂长', phone: '188-1122-3344', wechat: 'zhoutc_jg', primary: '是', owner: '周婷' },
  { id: 9, name: '吴敏', customer: '苏州精工汽车维修厂', title: '库管', phone: '188-1122-3355', wechat: 'wumin_jg', primary: '否', owner: '周婷' },
  { id: 10, name: '郑凯', customer: '杭州车匠汽车维修连锁', title: '区域经理', phone: '131-2233-9900', wechat: 'zhengkai_cj', primary: '否', owner: '张明' }
]

// ---------------- 跟进记录 ----------------
export const followups = [
  { id: 1, time: '2026-11-12 09:20', customer: '杭州车匠汽车维修连锁', contact: '陈晓峰', type: '电话', content: '确认年度框架的滤芯型号与首批数量，客户希望月底前到货。', next: '11-14 前发正式报价单', owner: '张明' },
  { id: 2, time: '2026-11-11 16:05', customer: '上海睿联汽车服务集团', contact: '王强', type: '上门拜访', content: '带样件上门测试制动片，技术反馈噪音低于原厂件。', next: '等对方走内部招标流程', owner: '李思' },
  { id: 3, time: '2026-11-11 10:40', customer: '深圳前海车享快修连锁', contact: '林雅', type: '微信', content: '客户询问变速箱油的到货周期，已回复现货 3 天可发。', next: '11-13 电话确认下单', owner: '张明' },
  { id: 4, time: '2026-11-10 15:30', customer: '北京博远汽车美容中心', contact: '赵敏', type: '电话', content: '镀晶耗材库存只剩 6 瓶，提醒补货并推送冬季养护套餐。', next: '11-12 发补货清单', owner: '周婷' },
  { id: 5, time: '2026-11-09 14:10', customer: '广州速达洗车连锁', contact: '黄磊', type: '上门拜访', content: '核对上月对账单，客户确认 4.56 万货款将在本周支付。', next: '11-15 跟进回款', owner: '张明' },
  { id: 6, time: '2026-11-08 11:00', customer: '成都康泰汽配经销部', contact: '孙浩', type: '微信', content: '点火线圈分销价谈不拢，客户暂时观望。', next: '下季度再回访', owner: '李思' },
  { id: 7, time: '2026-11-07 09:35', customer: '苏州精工汽车维修厂', contact: '周天成', type: '电话', content: '试销的制动片 200 套已装车，反馈良好，准备转正式供货。', next: '11-20 前签年度协议', owner: '周婷' }
]

// ---------------- 我的待办（已停用，保留仅作历史参考） ----------------
// 待办不再用「一份通用数据给所有账号」，而是按岗位画像在
// config/roleProfile.js 里为每个角色生成专属待办（营销=线索分配，
// 财务=回款催收，售后=工单受理…），由 stores/todo.js 首次进入时落库。
export const todos = [
  { id: 1, title: '车匠连锁：发年度框架报价单', type: '客户跟进', priority: '紧急', due: '2026-11-12', related: '杭州车匠汽车维修连锁', owner: '张明', done: false },
  { id: 2, title: '速达洗车：催 4.56 万货款回款', type: '合同回款', priority: '紧急', due: '2026-11-13', related: '广州速达洗车连锁', owner: '张明', done: false },
  { id: 3, title: '睿联集团：招标文件盖章寄出', type: '报价审批', priority: '重要', due: '2026-11-14', related: '上海睿联汽车服务集团', owner: '李思', done: false },
  { id: 4, title: '博远美容：补货清单确认', type: '客户跟进', priority: '重要', due: '2026-11-12', related: '北京博远汽车美容中心', owner: '周婷', done: false },
  { id: 5, title: '精工维修：年度协议条款复核', type: '合同回款', priority: '一般', due: '2026-11-20', related: '苏州精工汽车维修厂', owner: '周婷', done: false },
  { id: 6, title: '售后回访：车享快修变速箱油使用情况', type: '售后回访', priority: '一般', due: '2026-11-10', related: '深圳前海车享快修连锁', owner: '孙浩', done: false },
  { id: 7, title: '整理 10 月销售月报', type: '客户跟进', priority: '一般', due: '2026-11-06', related: '销售部', owner: '张明', done: true }
]

// ---------------- 经营概览：全年销售额与线索来源 ----------------
// 月度金额直接由订单汇总（已取消不计），目标单独配，这样概览与「销售统计」永远一致。
// 目标按 2026 年 1-11 月的实际签单量配到 95%-110% 的完成率，别让页面出现 300% 这种假数据。
const MONTH_TARGET = {
  '1月': 550000, '2月': 600000, '3月': 720000, '4月': 480000,
  '5月': 750000, '6月': 680000, '7月': 1000000, '8月': 920000,
  '9月': 1150000, '10月': 1250000, '11月': 1000000
}

export const monthSales = Object.keys(MONTH_TARGET).map((month) => ({
  month,
  amount: orders
    .filter((o) => o.status !== '已取消' && `${Number(o.orderDate.slice(5, 7))}月` === month)
    .reduce((s, o) => s + o.amount, 0),
  target: MONTH_TARGET[month]
}))

export const sourceStats = [
  { name: '官网表单', count: 32 },
  { name: '老客户推荐', count: 26 },
  { name: '展会', count: 21 },
  { name: '电话咨询', count: 15 },
  { name: '广告投放', count: 11 }
]

// ---------------- 报价单 ----------------
// 商机谈妥后先出报价单，审批通过再转销售订单；报价有效期过了自动失效
export const quotes = [
  { id: 1, quoteNo: 'QT20261112001', customer: '杭州车匠汽车维修连锁', contact: '陈晓峰', amount: 68000, status: '待审批', quoteDate: '2026-11-12', validTo: '2026-11-27', owner: '张明' },
  { id: 2, quoteNo: 'QT20261110002', customer: '上海睿联汽车服务集团', contact: '王强', amount: 125000, status: '已通过', quoteDate: '2026-11-10', validTo: '2026-11-25', owner: '李思' },
  { id: 3, quoteNo: 'QT20261108003', customer: '广州速达洗车连锁', contact: '黄磊', amount: 89000, status: '已转订单', quoteDate: '2026-11-08', validTo: '2026-11-23', owner: '张明' },
  { id: 4, quoteNo: 'QT20261105004', customer: '深圳前海车享快修连锁', contact: '林雅', amount: 42000, status: '已通过', quoteDate: '2026-11-05', validTo: '2026-11-20', owner: '张明' },
  { id: 5, quoteNo: 'QT20261103005', customer: '北京博远汽车美容中心', contact: '赵敏', amount: 26000, status: '草稿', quoteDate: '2026-11-03', validTo: '2026-11-18', owner: '周婷' },
  { id: 6, quoteNo: 'QT20261028006', customer: '苏州精工汽车维修厂', contact: '周天成', amount: 31000, status: '已转订单', quoteDate: '2026-10-28', validTo: '2026-11-12', owner: '周婷' },
  { id: 7, quoteNo: 'QT20261020007', customer: '成都康泰汽配经销部', contact: '孙浩', amount: 17500, status: '已失效', quoteDate: '2026-10-20', validTo: '2026-11-04', owner: '李思' }
]

// ---------------- 退换货单 ----------------
export const returnOrders = [
  { id: 1, returnNo: 'RT20261111001', orderNo: 'SO20261105002', customer: '北京博远汽车美容中心', type: '换货', qty: 12, amount: 2160, reason: '镀膜剂喷头堵塞，客户要求换新批次', status: '待审核', applyDate: '2026-11-11', owner: '周婷' },
  { id: 2, returnNo: 'RT20261108002', orderNo: 'SO20261108001', customer: '苏州精工汽车维修厂', type: '退货', qty: 8, amount: 960, reason: '制动片型号发错，与订单不符', status: '已受理', applyDate: '2026-11-08', owner: '周婷' },
  { id: 3, returnNo: 'RT20261102003', orderNo: 'SO20261028003', customer: '广州速达洗车连锁', type: '换货', qty: 20, amount: 1400, reason: '洗车液桶身破损，运输途中挤压', status: '已完成', applyDate: '2026-11-02', owner: '张明' },
  { id: 4, returnNo: 'RT20261026004', orderNo: 'SO20261015004', customer: '上海睿联汽车服务集团', type: '退货', qty: 4, amount: 5200, reason: '变速箱油包装渗漏，质检不合格', status: '已完成', applyDate: '2026-10-26', owner: '李思' },
  { id: 5, returnNo: 'RT20261018005', orderNo: 'SO20260926006', customer: '成都康泰汽配经销部', type: '退货', qty: 30, amount: 2700, reason: '点火线圈装车后故障灯常亮', status: '已驳回', applyDate: '2026-10-18', owner: '李思' }
]

// ---------------- 发货记录 ----------------
export const shipments = [
  { id: 1, shipNo: 'FH20261112001', orderNo: 'SO20261105002', customer: '北京博远汽车美容中心', qty: 36, carrier: '德邦物流', trackingNo: 'DB7788120099', shipDate: '2026-11-12', status: '待发货', owner: '赵敏' },
  { id: 2, shipNo: 'FH20261109002', orderNo: 'SO20261108001', customer: '苏州精工汽车维修厂', qty: 24, carrier: '顺丰速运', trackingNo: 'SF9988210077', shipDate: '2026-11-09', status: '已签收', owner: '赵敏' },
  { id: 3, shipNo: 'FH20261103003', orderNo: 'SO20261028003', customer: '广州速达洗车连锁', qty: 120, carrier: '专线物流', trackingNo: 'ZX20261103008', shipDate: '2026-11-03', status: '已发出', owner: '赵敏' },
  { id: 4, shipNo: 'FH20261018004', orderNo: 'SO20261015004', customer: '上海睿联汽车服务集团', qty: 260, carrier: '专线物流', trackingNo: 'ZX20261018012', shipDate: '2026-10-18', status: '已签收', owner: '赵敏' },
  { id: 5, shipNo: 'FH20261012005', orderNo: 'SO20261009005', customer: '深圳前海车享快修连锁', qty: 48, carrier: '顺丰速运', trackingNo: 'SF9987100066', shipDate: '2026-10-12', status: '已签收', owner: '赵敏' },
  { id: 6, shipNo: 'FH20261112006', orderNo: 'SO20261112030', customer: '上海睿联汽车服务集团', qty: 600, carrier: '专线物流', trackingNo: 'ZX20261112021', shipDate: '2026-11-13', status: '待发货', owner: '赵敏' }
]

// ---------------- 收款记录 ----------------
export const receipts = [
  { id: 1, receiptNo: 'SK20261110001', customer: '北京博远汽车美容中心', orderNo: 'SO20261105002', amount: 5000, method: '承兑汇票', receiptDate: '2026-11-10', operator: '刘洋', note: '首笔预付，余款月底结清' },
  { id: 2, receiptNo: 'SK20261109002', customer: '苏州精工汽车维修厂', orderNo: 'SO20261108001', amount: 3120, method: '微信收款', receiptDate: '2026-11-09', operator: '刘洋', note: '试销款全额' },
  { id: 3, receiptNo: 'SK20261022003', customer: '上海睿联汽车服务集团', orderNo: 'SO20261015004', amount: 98000, method: '银行转账', receiptDate: '2026-10-22', operator: '刘洋', note: '集采合同尾款' },
  { id: 4, receiptNo: 'SK20261014004', customer: '深圳前海车享快修连锁', orderNo: 'SO20261009005', amount: 22000, method: '银行转账', receiptDate: '2026-10-14', operator: '刘洋', note: '货到付款' },
  { id: 5, receiptNo: 'SK20261111005', customer: '杭州车匠汽车维修连锁', orderNo: 'SO20261110007', amount: 10000, method: '银行转账', receiptDate: '2026-11-11', operator: '刘洋', note: '年度框架首批货款，余款 11-25 前结' }
]

// ---------------- 应收账款 ----------------
// 应收 = 订单金额 - 已收金额；超过约定回款日即为逾期
export const receivables = [
  { id: 1, customer: '广州速达洗车连锁', orderNo: 'SO20261028003', total: 45600, received: 0, dueDate: '2026-11-10', owner: '张明' },
  { id: 2, customer: '北京博远汽车美容中心', orderNo: 'SO20261105002', total: 12860, received: 5000, dueDate: '2026-11-30', owner: '周婷' },
  { id: 3, customer: '上海睿联汽车服务集团', orderNo: 'SO20261015004', total: 98000, received: 98000, dueDate: '2026-10-25', owner: '李思' },
  { id: 4, customer: '苏州精工汽车维修厂', orderNo: 'SO20261108001', total: 3120, received: 3120, dueDate: '2026-11-15', owner: '周婷' },
  { id: 5, customer: '深圳前海车享快修连锁', orderNo: 'SO20261009005', total: 22000, received: 22000, dueDate: '2026-10-20', owner: '张明' },
  { id: 6, customer: '杭州车匠汽车维修连锁', orderNo: 'SO20261110007', total: 32250, received: 10000, dueDate: '2026-11-25', owner: '张明' },
  { id: 7, customer: '上海睿联汽车服务集团', orderNo: 'SO20261112030', total: 59800, received: 0, dueDate: '2026-12-12', owner: '李思' }
]

// ---------------- 售后工单 ----------------
export const tickets = [
  { id: 1, ticketNo: 'GD20261112001', customer: '上海睿联汽车服务集团', contact: '王强', product: '变速箱油', issue: '到货渗漏', level: '高', status: '待受理', createTime: '2026-11-12 09:10', owner: '孙浩' },
  { id: 2, ticketNo: 'GD20261110002', customer: '苏州精工汽车维修厂', contact: '周天成', product: '刹车片', issue: '装车后异响', level: '高', status: '处理中', createTime: '2026-11-10 14:25', owner: '孙浩' },
  { id: 3, ticketNo: 'GD20261106003', customer: '北京博远汽车美容中心', contact: '赵敏', product: '镀膜剂', issue: '喷头堵塞不出液', level: '中', status: '已解决', createTime: '2026-11-06 10:40', owner: '吴磊' },
  { id: 4, ticketNo: 'GD20261103004', customer: '深圳前海车享快修连锁', contact: '林雅', product: '火花塞', issue: '故障灯常亮', level: '中', status: '处理中', createTime: '2026-11-03 16:05', owner: '吴磊' },
  { id: 5, ticketNo: 'GD20261028005', customer: '成都康泰汽配经销部', contact: '孙浩', product: '机油滤芯', issue: '规格与车型不符', level: '低', status: '已关闭', createTime: '2026-10-28 11:15', owner: '吴磊' }
]

// ---------------- 客户回访 ----------------
export const revisits = [
  { id: 1, time: '2026-11-12 10:20', customer: '深圳前海车享快修连锁', contact: '林雅', way: '电话', score: '满意', content: '变速箱油使用一周，换挡平顺，无渗漏', next: '12 月首周再回访一次', owner: '孙浩' },
  { id: 2, time: '2026-11-10 15:40', customer: '上海睿联汽车服务集团', contact: '王强', way: '上门', score: '满意', content: '制动片装车 200 套，噪音低于原厂件', next: '跟进渗漏工单处理结果', owner: '吴磊' },
  { id: 3, time: '2026-11-08 09:30', customer: '苏州精工汽车维修厂', contact: '周天成', way: '电话', score: '一般', content: '制动片试销反馈良好，但希望包装再加固', next: '把包装问题反馈生产', owner: '孙浩' },
  { id: 4, time: '2026-11-05 14:00', customer: '杭州车匠汽车维修连锁', contact: '陈晓峰', way: '上门', score: '满意', content: '年度框架耗材使用正常，续签意向明确', next: '11-14 前发正式报价单', owner: '张明' },
  { id: 5, time: '2026-11-02 11:10', customer: '北京博远汽车美容中心', contact: '赵敏', way: '微信', score: '不满意', content: '镀膜剂喷头问题影响施工，已开售后工单', next: '等换货到店后再回访', owner: '吴磊' }
]
