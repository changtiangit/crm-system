import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { checkPermission } from '@/composables/usePermission'
import { hydrateCrmData } from '@/utils/hydrateCrmData'

/**
 * 后台业务菜单（一级 + 二级）。
 * meta.title 菜单名；meta.icon 图标；meta.hidden 不在菜单中显示；
 * meta.permission 访问该页面所需的权限标识（为空表示登录即可访问）。
 */
export const menuRoutes = [
  {
    // 工作台：岗位工作台是每个角色的专属首屏，经营概览是公司级大盘，我的待办是个人的活
    path: 'workbench',
    name: 'Workbench',
    // 登录后先落到「岗位工作台」：营销看到线索调度台，财务看到资金工作台，售后看到服务台
    redirect: () => firstChildPath('workbench', 'desk'),
    meta: { title: '工作台', icon: 'HomeFilled' },
    children: [
      {
        // 岗位工作台：不设权限（登录即可），面板内容按角色画像渲染
        path: 'desk',
        name: 'RoleDesk',
        component: () => import('@/views/workbench/desk.vue'),
        meta: { title: '岗位工作台' }
      },
      {
        path: 'overview',
        name: 'Overview',
        component: () => import('@/views/workbench/overview.vue'),
        meta: { title: '经营概览', permission: 'dashboard:view' }
      },
      {
        path: 'todo',
        name: 'Todo',
        component: () => import('@/views/workbench/todo.vue'),
        meta: { title: '我的待办', permission: 'todo:view' }
      }
    ]
  },
  {
    // 数据统计：给领导和销售主管看数，三张报表同源同口径，可互相钻取
    path: 'stats',
    name: 'StatsModule',
    redirect: () => firstChildPath('stats', 'sales'),
    meta: { title: '数据统计', icon: 'DataAnalysis' },
    children: [
      {
        path: 'sales',
        name: 'StatsSales',
        component: () => import('@/views/stats/sales.vue'),
        meta: { title: '销售统计', permission: 'stats:sales:view' }
      },
      {
        path: 'customer',
        name: 'StatsCustomer',
        component: () => import('@/views/stats/customer.vue'),
        meta: { title: '客户采购分析', permission: 'stats:customer:view' }
      },
      {
        path: 'product',
        name: 'StatsProduct',
        component: () => import('@/views/stats/product.vue'),
        meta: { title: '产品销售分析', permission: 'stats:product:view' }
      },
      {
        // 市场分析：营销唯一的数据报表，口径是线索（渠道 / 漏斗 / 转化），不涉签单金额
        path: 'market',
        name: 'StatsMarket',
        component: () => import('@/views/stats/market.vue'),
        meta: { title: '市场分析', permission: 'stats:market:view' }
      }
    ]
  },
  {
    // 客户管理：线索 → 公海 → 客户档案 → 360 全景 → 联系人 → 跟进记录
    path: 'customer',
    name: 'CustomerModule',
    // 旧地址 /customer 原本就是客户列表，这里沿用，避免老书签跳到别处
    // 客户管理：营销只有线索与团队权限，落到第一个有权限的子页
    redirect: () => firstChildPath('customer', 'list'),
    meta: { title: '客户管理', icon: 'User' },
    children: [
      {
        path: 'clue',
        name: 'Clue',
        component: () => import('@/views/clue/index.vue'),
        meta: { title: '销售线索', permission: 'clue:view' }
      },
      {
        // 销售团队：组长、组员、能力等级与在手线索配额
        path: 'team',
        name: 'SalesTeam',
        component: () => import('@/views/customer/team.vue'),
        meta: { title: '销售团队', permission: 'sales:team:view' }
      },
      {
        path: 'pool',
        name: 'CustomerPool',
        component: () => import('@/views/customer/pool.vue'),
        meta: { title: '客户公海', permission: 'pool:view' }
      },
      {
        path: 'list',
        name: 'Customer',
        component: () => import('@/views/customer/index.vue'),
        meta: { title: '客户档案', permission: 'customer:view' }
      },
      {
        // 客户 360：不占菜单位置，从客户档案、订单、统计排行等处点进来
        path: 'detail/:id',
        name: 'CustomerDetail',
        component: () => import('@/views/customer/detail.vue'),
        meta: { title: '客户 360', permission: 'customer:view', hidden: true }
      },
      {
        path: 'contact',
        name: 'Contact',
        component: () => import('@/views/customer/contact.vue'),
        meta: { title: '联系人', permission: 'contact:view' }
      },
      {
        path: 'followup',
        name: 'FollowUp',
        component: () => import('@/views/customer/followup.vue'),
        meta: { title: '跟进记录', permission: 'followup:view' }
      }
    ]
  },
  {
    // 销售管理：商机 → 报价单 → 销售订单，商品作为下单的基础资料放最后
    path: 'sales',
    name: 'SalesModule',
    redirect: () => firstChildPath('sales', 'opportunity'),
    meta: { title: '销售管理', icon: 'TrendCharts' },
    children: [
      {
        path: 'opportunity',
        name: 'Opportunity',
        component: () => import('@/views/opportunity/index.vue'),
        meta: { title: '销售商机', permission: 'opportunity:view' }
      },
      {
        path: 'quote',
        name: 'Quote',
        component: () => import('@/views/sales/quote.vue'),
        meta: { title: '报价单', permission: 'quote:view' }
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '销售订单', permission: 'order:view' }
      },
      {
        // 订单详情（合同清单）：点单号进来，一屏看完这单签了什么、多少钱、赚多少
        // 用单号而不是自增 id 做路径参数，发货、收款、统计各处手上都只有单号
        path: 'order/detail/:orderNo',
        name: 'OrderDetail',
        component: () => import('@/views/order/detail.vue'),
        meta: { title: '订单详情', permission: 'order:view', hidden: true, activeMenu: '/sales/order' }
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('@/views/product/index.vue'),
        meta: { title: '商品', permission: 'product:view' }
      }
    ]
  },
  {
    // 交付与回款：发货 → 收款 → 应收，订单签完之后的钱和货都在这里
    path: 'delivery',
    name: 'DeliveryModule',
    redirect: () => firstChildPath('delivery', 'shipment'),
    meta: { title: '交付与回款', icon: 'Van' },
    children: [
      {
        path: 'shipment',
        name: 'Shipment',
        component: () => import('@/views/delivery/shipment.vue'),
        meta: { title: '发货记录', permission: 'delivery:view' }
      },
      {
        path: 'receipt',
        name: 'Receipt',
        component: () => import('@/views/delivery/receipt.vue'),
        meta: { title: '收款记录', permission: 'receipt:view' }
      },
      {
        path: 'receivable',
        name: 'Receivable',
        component: () => import('@/views/delivery/receivable.vue'),
        meta: { title: '应收账款', permission: 'receivable:view' }
      }
    ]
  },
  {
    // 售后管理：出问开工单 → 退货换货 → 货到之后做回访，售后一条线走完
    path: 'service',
    name: 'ServiceModule',
    redirect: () => firstChildPath('service', 'ticket'),
    meta: { title: '售后管理', icon: 'Service' },
    children: [
      {
        path: 'ticket',
        name: 'Ticket',
        component: () => import('@/views/service/ticket.vue'),
        meta: { title: '售后工单', permission: 'ticket:view' }
      },
      {
        path: 'return',
        name: 'ReturnOrder',
        component: () => import('@/views/sales/return.vue'),
        meta: { title: '退换货单', permission: 'return:view' }
      },
      {
        path: 'revisit',
        name: 'Revisit',
        component: () => import('@/views/service/revisit.vue'),
        meta: { title: '客户回访', permission: 'revisit:view' }
      }
    ]
  },
  {
    path: 'system',
    name: 'System',
    redirect: () => firstChildPath('system', 'employee'),
    meta: { title: '设置', icon: 'Setting' },
    children: [
      {
        path: 'employee',
        name: 'Employee',
        component: () => import('@/views/system/employee.vue'),
        meta: { title: '员工管理', permission: 'system:employee:view' }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色管理', permission: 'system:role:view' }
      },
      {
        path: 'dept',
        name: 'Dept',
        component: () => import('@/views/system/dept.vue'),
        meta: { title: '部门管理', permission: 'system:dept:view' }
      },
      {
        path: 'permission',
        name: 'Permission',
        component: () => import('@/views/system/permission.vue'),
        meta: { title: '权限授权', permission: 'system:permission:view' }
      }
    ]
  }
]

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { public: true, title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    children: menuRoutes
  },
  // 旧地址兼容：菜单改版前的书签仍能打开
  { path: '/clue', redirect: '/customer/clue' },
  { path: '/opportunity', redirect: '/sales/opportunity' },
  { path: '/order', redirect: '/sales/order' },
  { path: '/order/list', redirect: '/sales/order' },
  { path: '/order/product', redirect: '/sales/product' },
  { path: '/product', redirect: '/sales/product' },
  // 退换货从「销售管理」挪到「售后管理」，老链接继续可用
  { path: '/sales/return', redirect: '/service/return' },
  { path: '/stats', redirect: () => firstChildPath('stats', 'sales') },
  { path: '/customer/detail', redirect: () => firstChildPath('customer', 'list') },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无访问权限' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { public: true, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/**
 * 模块入口：优先落到模块的默认页（preferred），没有该权限时退到第一个可访问的子页面。
 * 营销只有线索权限、销售只有客户权限，直接写死 redirect 会把人送进 403。
 */
function firstChildPath(modulePath, preferred) {
  const mod = menuRoutes.find((r) => r.path === modulePath)
  const children = (mod?.children || []).filter((c) => !c.meta?.hidden)
  const target = children.find((c) => c.path === preferred && checkPermission(c.meta?.permission))
    || children.find((c) => checkPermission(c.meta?.permission))
  return target ? `/${modulePath}/${target.path}` : '/403'
}

/** 首页：按当前账号权限找到第一个可访问的页面 */
function firstAccessiblePath() {
  for (const r of menuRoutes) {
    if (r.meta?.hidden) continue
    if (r.children?.length) {
      for (const c of r.children) {
        if (!c.meta?.hidden && checkPermission(c.meta?.permission)) return `/${r.path}/${c.path}`
      }
    } else if (checkPermission(r.meta?.permission)) {
      return `/${r.path}`
    }
  }
  return '/403'
}

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  document.title = to.meta.title ? `${to.meta.title} · CRM 系统` : 'CRM 客户关系管理系统'

  if (to.meta.public) {
    if (to.path === '/login' && userStore.isLogin) return { path: '/' }
    return true
  }

  if (!userStore.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 首页按权限分流，避免无声一登录就落到 403
  if (to.path === '/') {
    return { path: firstAccessiblePath() }
  }

  // 功能权限校验
  if (to.meta.permission && !checkPermission(to.meta.permission)) {
    return { path: '/403' }
  }

  await hydrateCrmData(userStore.permissionCodes, to.name)

  return true
})

export default router
