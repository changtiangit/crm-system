/**
 * 岗位画像：谁该看到什么。
 *
 * 这套系统里「角色」不是一个称谓，而是三段信息的组合：
 *   1. 角色码（roles.code）      → 能进哪些页面（功能权限）
 *   2. 数据范围（dataScope.type） → 页面里能看到谁的数据（行级权限）
 *   3. 岗位（position）           → 同一个角色下职责不同（销售总监 / 组长 / 顾问）
 *
 * 所以岗位画像用「角色码 + 数据范围」来判定，而不是只看角色名：
 *   SALES + ALL          → 销售总监，看全公司业绩
 *   SALES_G1/G2 + GROUP  → 销售组长，看本组
 *   SALES_G1/G2 + SELF   → 销售顾问，只看自己
 *
 * 每个岗位画像决定四件事：
 *   - 工作台首屏放什么（desks/ 下对应的面板组件）
 *   - 待办默认给哪些类型、哪些事项（seedTodos）
 *   - 侧边栏和工作台里的快捷入口
 *   - 待办的「关联对象」点进去落到哪个业务页（必须是自己有权限的页）
 *
 * 营销不该看到「合同回款」「售后回访」，财务不该看到「线索分配」，
 * 这些差异全部收敛在这里，页面本身只负责渲染。
 */

/** 相对今天偏移 n 天，用于生成待办截止日期（种子只生成一次，日期按当时算） */
function dayOffset(n) {
  const d = new Date()
  d.setDate(d.getDate() + n)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 当前用户拥有哪些角色码 */
function roleCodesOf(user) {
  return (user?.roles || []).map((r) => r.code)
}

/** 岗位判定：角色码 + 数据范围，销售三个岗位靠数据范围区分 */
export function resolveRoleKey(user) {
  const codes = roleCodesOf(user)
  const scope = user?.dataScope?.type || 'ALL'
  if (codes.includes('ADMIN')) return 'admin'
  if (codes.includes('MARKET')) return 'market'
  if (codes.includes('FINANCE')) return 'finance'
  if (codes.includes('SERVICE')) return 'service'
  if (codes.includes('PRODUCTION')) return 'production'
  if (codes.includes('SALES')) return 'sales_lead'
  if (codes.includes('SALES_G1') || codes.includes('SALES_G2')) {
    return scope === 'GROUP' ? 'sales_group' : 'sales_rep'
  }
  return 'guest'
}

/* ------------------------------------------------------------------ */
/* 各岗位画像                                                          */
/* ------------------------------------------------------------------ */

const PROFILES = {
  admin: {
    key: 'admin',
    name: '系统管理员',
    deskTitle: '系统管理台',
    tagline: '账号、角色、权限、部门都在你手上；先看系统健康度，再处理账号与授权',
    duty: ['账号与角色维护', '权限矩阵治理', '部门与数据范围', '全盘数据监控'],
    accent: '#4b5bdc',
    soft: '#eef0ff',
    todoTypes: ['权限复核', '账号交接', '数据治理', '系统公告', '流程优化'],
    shortcuts: [
      { label: '员工管理', path: '/system/employee', perm: 'system:employee:view' },
      { label: '角色管理', path: '/system/role', perm: 'system:role:view' },
      { label: '权限授权', path: '/system/permission', perm: 'system:permission:view' },
      { label: '部门管理', path: '/system/dept', perm: 'system:dept:view' }
    ],
    seedTodos: [
      {
        title: '核对新入职账号的部门与角色是否匹配',
        type: '权限复核',
        priority: '重要',
        offset: 0,
        related: '员工管理',
        relatedType: '系统',
        link: '/system/employee'
      },
      {
        title: '检查各销售组的数据范围（ALL / GROUP / SELF）配置',
        type: '权限复核',
        priority: '重要',
        offset: 2,
        related: '销售团队',
        relatedType: '组织',
        link: '/customer/team'
      },
      {
        title: '复核财务与售后岗位的权限矩阵',
        type: '权限复核',
        priority: '一般',
        offset: 3,
        related: '权限授权',
        relatedType: '系统',
        link: '/system/permission'
      },
      {
        title: '清理长期未登录的离职账号',
        type: '账号交接',
        priority: '一般',
        offset: -1,
        related: '员工管理',
        relatedType: '系统',
        link: '/system/employee'
      }
    ]
  },

  market: {
    key: 'market',
    name: '营销',
    deskTitle: '线索调度台',
    tagline: '线索是营销交付给销售的「产品」：拿得准、洗得净、分得快，签单才有源头',
    duty: ['多渠道获客', '线索清洗与补全', '按销售能力分配', '渠道 ROI 复盘'],
    accent: '#0891b2',
    soft: '#e6f7fb',
    todoTypes: ['线索分配', '线索复核', '渠道活动', '内容投放', '市场复盘'],
    shortcuts: [
      { label: '销售线索', path: '/customer/clue', perm: 'clue:view' },
      { label: '待分配线索', path: '/customer/clue?status=待分配', perm: 'clue:view' },
      { label: '市场分析', path: '/stats/market', perm: 'stats:market:view' },
      { label: '销售团队', path: '/customer/team', perm: 'sales:team:view' }
    ],
    seedTodos: [
      {
        title: '分配今日新增线索（展会 / 官网表单）',
        type: '线索分配',
        priority: '紧急',
        offset: -1,
        related: '待分配线索池',
        relatedType: '线索',
        link: '/customer/clue?status=待分配'
      },
      {
        title: '复核高意向线索的联系人与需求描述',
        type: '线索复核',
        priority: '重要',
        offset: 0,
        related: '高意向线索',
        relatedType: '线索',
        link: '/customer/clue?intent=高'
      },
      {
        title: '统计上周各渠道线索量，更新渠道 ROI',
        type: '市场复盘',
        priority: '重要',
        offset: 1,
        related: '市场分析',
        relatedType: '报表',
        link: '/stats/market'
      },
      {
        title: '与销售一组对齐本周线索承接能力',
        type: '线索分配',
        priority: '一般',
        offset: 2,
        related: '销售团队',
        relatedType: '组织',
        link: '/customer/team'
      },
      {
        title: '回收展会线索的跟进结果，标记无效线索',
        type: '线索复核',
        priority: '一般',
        offset: -2,
        related: '展会来源线索',
        relatedType: '线索',
        link: '/customer/clue?source=展会'
      }
    ]
  },

  finance: {
    key: 'finance',
    name: '财务',
    deskTitle: '资金工作台',
    tagline: '订单签完只是开始：钱什么时候到、账多久能平，才是财务的主战场',
    duty: ['回款核销与催收', '应收账款账龄', '开票与对账', '退换货款项处理'],
    accent: '#7c3aed',
    soft: '#f3edff',
    todoTypes: ['回款催收', '到账核销', '开票申请', '月末对账', '账龄复核'],
    shortcuts: [
      { label: '应收账款', path: '/delivery/receivable', perm: 'receivable:view' },
      { label: '收款记录', path: '/delivery/receipt', perm: 'receipt:view' },
      { label: '销售订单', path: '/sales/order', perm: 'order:view' },
      { label: '退换货单', path: '/service/return', perm: 'return:view' }
    ],
    seedTodos: [
      {
        title: '催收速达洗车 4.56 万逾期货款',
        type: '回款催收',
        priority: '紧急',
        offset: -4,
        related: '广州速达洗车连锁',
        relatedType: '应收账款',
        link: '/delivery/receivable?customer=广州速达洗车连锁'
      },
      {
        title: '核销车匠连锁年度框架首批款 1 万',
        type: '到账核销',
        priority: '重要',
        offset: 0,
        related: '杭州车匠汽车维修连锁',
        relatedType: '收款',
        link: '/delivery/receipt?customer=杭州车匠汽车维修连锁'
      },
      {
        title: '为睿联集团 5.98 万新单开具发票',
        type: '开票申请',
        priority: '重要',
        offset: 1,
        related: '上海睿联汽车服务集团',
        relatedType: '订单',
        link: '/sales/order?customer=上海睿联汽车服务集团'
      },
      {
        title: '博远美容尾款 7,860 元到期提醒',
        type: '回款催收',
        priority: '一般',
        offset: 16,
        related: '北京博远汽车美容中心',
        relatedType: '应收账款',
        link: '/delivery/receivable?customer=北京博远汽车美容中心'
      },
      {
        title: '核对 10 月收款与订单金额，出对账单',
        type: '月末对账',
        priority: '一般',
        offset: -2,
        related: '10 月账单',
        relatedType: '对账',
        link: '/delivery/receipt'
      }
    ]
  },

  service: {
    key: 'service',
    name: '售后',
    deskTitle: '客户服务台',
    tagline: '交付之后的每一次响应都决定复购：工单不积压、客诉不过夜、回访有下文',
    duty: ['工单受理与派工', '客诉处理与升级', '退换货执行', '客户回访'],
    accent: '#0f9d76',
    soft: '#e8f8f2',
    todoTypes: ['工单受理', '客诉处理', '退换货处理', '客户回访', '技术支援'],
    shortcuts: [
      { label: '售后工单', path: '/service/ticket', perm: 'ticket:view' },
      { label: '退换货单', path: '/service/return', perm: 'return:view' },
      { label: '客户回访', path: '/service/revisit', perm: 'revisit:view' },
      { label: '客户档案', path: '/customer/list', perm: 'customer:view' }
    ],
    seedTodos: [
      {
        title: '受理睿联集团变速箱油渗漏工单（高优先级）',
        type: '工单受理',
        priority: '紧急',
        offset: -1,
        related: '上海睿联汽车服务集团',
        relatedType: '工单',
        link: '/service/ticket?customer=上海睿联汽车服务集团'
      },
      {
        title: '跟进精工维修刹车片异响处理结果',
        type: '客诉处理',
        priority: '紧急',
        offset: 0,
        related: '苏州精工汽车维修厂',
        relatedType: '工单',
        link: '/service/ticket?customer=苏州精工汽车维修厂'
      },
      {
        title: '博远美容镀膜剂换货到店后回访',
        type: '客户回访',
        priority: '重要',
        offset: 2,
        related: '北京博远汽车美容中心',
        relatedType: '回访',
        link: '/service/revisit'
      },
      {
        title: '车享快修变速箱油使用情况回访',
        type: '客户回访',
        priority: '一般',
        offset: -3,
        related: '深圳前海车享快修连锁',
        relatedType: '回访',
        link: '/service/revisit'
      },
      {
        title: '高等级工单 SLA 巡检，超时工单升级',
        type: '工单受理',
        priority: '重要',
        offset: 1,
        related: '售后工单',
        relatedType: '工单',
        link: '/service/ticket'
      }
    ]
  },

  production: {
    key: 'production',
    name: '生产',
    deskTitle: '交付保障台',
    tagline: '订单进来就要排得下去、备得齐、发得出：交期是生产给客户的承诺',
    duty: ['订单排产', '备货与库存预警', '发货与物流', '退换货补发'],
    accent: '#b45309',
    soft: '#fdf4e3',
    todoTypes: ['排产计划', '备货补货', '发货安排', '库存盘点', '退换货处理'],
    shortcuts: [
      { label: '销售订单', path: '/sales/order', perm: 'order:view' },
      { label: '发货记录', path: '/delivery/shipment', perm: 'delivery:view' },
      { label: '商品库存', path: '/sales/product', perm: 'product:view' },
      { label: '退换货单', path: '/service/return', perm: 'return:view' }
    ],
    seedTodos: [
      {
        title: '睿联集团 600 件订单排产，锁定 11-18 交期',
        type: '排产计划',
        priority: '紧急',
        offset: 0,
        related: 'SO20261112030',
        relatedType: '订单',
        link: '/sales/order?customer=上海睿联汽车服务集团'
      },
      {
        title: '博远美容 36 件待发货，今日安排物流',
        type: '发货安排',
        priority: '紧急',
        offset: -1,
        related: '北京博远汽车美容中心',
        relatedType: '发货',
        link: '/delivery/shipment?customer=北京博远汽车美容中心'
      },
      {
        title: '滤芯类安全库存盘点，低于阈值补货',
        type: '库存盘点',
        priority: '重要',
        offset: 2,
        related: '商品库存',
        relatedType: '商品',
        link: '/sales/product'
      },
      {
        title: '速达洗车 120 件到货签收确认',
        type: '发货安排',
        priority: '一般',
        offset: 1,
        related: '广州速达洗车连锁',
        relatedType: '发货',
        link: '/delivery/shipment?customer=广州速达洗车连锁'
      },
      {
        title: '换货 12 件镀膜剂补发，跟进生产批次',
        type: '退换货处理',
        priority: '重要',
        offset: 1,
        related: 'RT20261111001',
        relatedType: '退换货',
        link: '/service/return'
      }
    ]
  },

  sales_lead: {
    key: 'sales_lead',
    name: '销售总监',
    deskTitle: '业绩驾驶舱',
    tagline: '盯目标、看团队、推大单、控回款——四件事定胜负',
    duty: ['目标拆解与达成', '团队业绩管理', '大单与报价审批', '回款风险把控'],
    accent: '#2f5fe0',
    soft: '#eef3ff',
    todoTypes: ['目标复盘', '大单推进', '报价审批', '团队辅导', '回款督促'],
    shortcuts: [
      { label: '销售统计', path: '/stats/sales', perm: 'stats:sales:view' },
      { label: '销售团队', path: '/customer/team', perm: 'sales:team:view' },
      { label: '销售商机', path: '/sales/opportunity', perm: 'opportunity:view' },
      { label: '销售订单', path: '/sales/order', perm: 'order:view' }
    ],
    seedTodos: [
      {
        title: '车匠连锁年度框架报价单审批',
        type: '报价审批',
        priority: '紧急',
        offset: 0,
        related: '杭州车匠汽车维修连锁',
        relatedType: '报价单',
        link: '/sales/quote?customer=杭州车匠汽车维修连锁'
      },
      {
        title: '速达洗车 4.56 万逾期回款，找负责人谈话',
        type: '回款督促',
        priority: '紧急',
        offset: -3,
        related: '广州速达洗车连锁',
        relatedType: '应收账款',
        link: '/delivery/receivable?customer=广州速达洗车连锁'
      },
      {
        title: '睿联集团 12.5 万大单推进，本周定标',
        type: '大单推进',
        priority: '重要',
        offset: 2,
        related: '上海睿联汽车服务集团',
        relatedType: '商机',
        link: '/sales/opportunity'
      },
      {
        title: '11 月目标复盘：距离 26 万还差多少',
        type: '目标复盘',
        priority: '重要',
        offset: 3,
        related: '销售统计',
        relatedType: '报表',
        link: '/stats/sales'
      },
      {
        title: '与二组组长对齐组员商机陪跑计划',
        type: '团队辅导',
        priority: '一般',
        offset: 4,
        related: '销售二组',
        relatedType: '组织',
        link: '/customer/team'
      }
    ]
  },

  sales_group: {
    key: 'sales_group',
    name: '销售组长',
    deskTitle: '小组作战台',
    tagline: '线索到组就要有人跟：派得准、陪得勤、单子才推得动',
    duty: ['组内线索派发', '组员陪跑辅导', '商机推进', '组内回款督促'],
    accent: '#1d6fe0',
    soft: '#eaf3ff',
    todoTypes: ['线索派发', '组员陪跑', '商机推进', '报价审核', '回款督促'],
    shortcuts: [
      { label: '销售线索', path: '/customer/clue', perm: 'clue:view' },
      { label: '销售团队', path: '/customer/team', perm: 'sales:team:view' },
      { label: '销售商机', path: '/sales/opportunity', perm: 'opportunity:view' },
      { label: '跟进记录', path: '/customer/followup', perm: 'followup:view' }
    ],
    seedTodos: [
      {
        title: '把本周新线索派给组内销售（按在手配额）',
        type: '线索派发',
        priority: '紧急',
        offset: -1,
        related: '待分配线索池',
        relatedType: '线索',
        link: '/customer/clue?status=待分配'
      },
      {
        title: '陪访睿联集团制动底盘件集采谈判',
        type: '组员陪跑',
        priority: '重要',
        offset: 1,
        related: '上海睿联汽车服务集团',
        relatedType: '商机',
        link: '/sales/opportunity'
      },
      {
        title: '审核组员报价折扣（低于 9 折需签批）',
        type: '报价审核',
        priority: '重要',
        offset: 0,
        related: '报价单',
        relatedType: '报价',
        link: '/sales/quote'
      },
      {
        title: '组内超 14 天未跟进的商机逐条过一遍',
        type: '商机推进',
        priority: '一般',
        offset: 2,
        related: '销售商机',
        relatedType: '商机',
        link: '/sales/opportunity'
      }
    ]
  },

  sales_rep: {
    key: 'sales_rep',
    name: '销售顾问',
    deskTitle: '我的作战台',
    tagline: '今天该打的电话、该推的单、该收的钱，一屏看完',
    duty: ['线索跟进转化', '客户拜访', '商机推进', '报价与签约'],
    accent: '#3b6cff',
    soft: '#eef3ff',
    todoTypes: ['客户跟进', '客户拜访', '报价跟进', '商机推进', '回款提醒'],
    shortcuts: [
      { label: '我的线索', path: '/customer/clue', perm: 'clue:view' },
      { label: '客户档案', path: '/customer/list', perm: 'customer:view' },
      { label: '销售商机', path: '/sales/opportunity', perm: 'opportunity:view' },
      { label: '报价单', path: '/sales/quote', perm: 'quote:view' }
    ],
    seedTodos: [
      {
        // 顾问的数据范围是「仅本人」，模板不写死别家的客户名，落到自己能进的页面
        title: '跟进本周新分配线索，48 小时内完成首次联系',
        type: '客户跟进',
        priority: '紧急',
        offset: 0,
        related: '我的线索',
        relatedType: '线索',
        link: '/customer/clue'
      },
      {
        title: '整理在手商机，更新阶段与预计签约日',
        type: '商机推进',
        priority: '重要',
        offset: 1,
        related: '销售商机',
        relatedType: '商机',
        link: '/sales/opportunity'
      },
      {
        title: '回访在谈报价的客户，确认报价有效期',
        type: '报价跟进',
        priority: '重要',
        offset: 2,
        related: '报价单',
        relatedType: '报价',
        link: '/sales/quote'
      },
      {
        // 顾问没有「应收账款」权限，回款动作落到订单列表里看，别让人点进 403
        title: '核对名下客户订单的回款状态，提醒临期账款',
        type: '回款提醒',
        priority: '一般',
        offset: -1,
        related: '销售订单',
        relatedType: '订单',
        link: '/sales/order'
      },
      {
        title: '从客户公海领取 1 家本地门店，补足在手客户',
        type: '客户跟进',
        priority: '一般',
        offset: 3,
        related: '客户公海',
        relatedType: '公海',
        link: '/customer/pool'
      }
    ]
  },

  guest: {
    key: 'guest',
    name: '成员',
    deskTitle: '我的工作台',
    tagline: '当前账号还没有配置岗位职责，请联系管理员在「权限授权」里分配角色',
    duty: ['待分配岗位'],
    accent: '#52627a',
    soft: '#f2f5fb',
    todoTypes: ['其他事项'],
    shortcuts: [],
    seedTodos: [
      {
        title: '联系管理员分配岗位角色与权限',
        type: '其他事项',
        priority: '重要',
        offset: 0,
        related: '系统管理员',
        relatedType: '系统',
        link: '/403'
      }
    ]
  }
}

export const roleProfiles = PROFILES

/** 取当前用户对应的岗位画像 */
export function getRoleProfile(user) {
  return PROFILES[resolveRoleKey(user)] || PROFILES.guest
}

/**
 * 生成这个岗位的默认待办（首次进入「我的待办」时落库）。
 * due 用相对日期算，保证种子数据看起来就是「今天该干的事」。
 */
export function seedTodosOf(profile, ownerName) {
  return (profile.seedTodos || []).map((t, i) => ({
    id: undefined,
    title: t.title,
    type: t.type,
    priority: t.priority,
    due: dayOffset(t.offset),
    related: t.related,
    relatedType: t.relatedType,
    link: t.link,
    owner: ownerName,
    done: false,
    sort: i
  }))
}

/** 待办类型下拉：岗位专属类型 + 该角色历史数据里出现过的类型，避免老数据选不到 */
export function todoTypesOf(profile, extraTypes = []) {
  return [...new Set([...(profile?.todoTypes || []), ...extraTypes.filter(Boolean)])]
}
