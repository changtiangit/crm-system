#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CRM 演示数据生成器 → v20_demo_dataset.sql

为什么用生成器而不是手写 SQL：
  客户、订单、应收、收款、发货、工单之间靠「客户名 / 订单号 / 负责人」互相引用，
  手写必然对不上；这里统一用同一份内存数据算出，保证三处金额、状态、归属永远一致。

幂等：整批写 crm_seed_batches('v20_demo_dataset')，重复执行直接跳过。
"""

import json
import random
from datetime import date, datetime, timedelta

random.seed(20261116)

TODAY = date(2026, 11, 16)


def sq(value):
    """SQL 字符串字面量"""
    if value is None:
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def payload(obj):
    raw = json.dumps(obj, ensure_ascii=False, separators=(',', ':'))
    return sq(raw)


def d(value):
    return value.strftime('%Y-%m-%d')


def ym(value):
    return value.strftime('%Y-%m')


# ---------------------------------------------------------------- 基础字典
REPS = ['陈嘉伟', '方宇', '林晓', '何俊', '苏晴', '黄志远']
LEADERS = ['李思', '周婷']
DIRECTOR = '张明'
SALES_OWNERS = REPS + LEADERS + [DIRECTOR]

CITIES = ['杭州', '上海', '深圳', '北京', '成都', '广州', '苏州', '宁波', '无锡', '青岛',
          '郑州', '合肥', '武汉', '长沙', '西安', '南京', '天津', '重庆', '东莞', '佛山',
          '济南', '福州', '昆明', '石家庄', '南昌', '太原', '沈阳', '厦门']
BRANDS = ['德众', '捷通', '海润', '顺达', '安驰', '宏远', '汇通', '恒信', '联众', '华胜',
          '瑞通', '乾通', '金顺', '万通', '博世达', '驰远', '凯达', '新干线', '顺和',
          '长通', '嘉运', '安泰', '正大', '合力', '鑫源', '天成', '远航', '立信']

INDUSTRY_TEMPLATE = {
    '汽车维修': ['汽车维修厂', '汽车维修连锁', '汽车服务有限公司', '汽车养护中心'],
    '4S 店': ['汽车销售服务有限公司', '汽车 4S 店'],
    '洗车连锁': ['洗车连锁', '汽车清洗服务有限公司'],
    '汽车美容': ['汽车美容中心', '汽车美容连锁'],
    '汽配经销': ['汽配经销部', '汽配贸易有限公司'],
    '车队物流': ['车队服务有限公司', '物流运输有限公司'],
}
INDUSTRIES = list(INDUSTRY_TEMPLATE.keys())

CONTACT_SURNAME = ['陈', '王', '李', '张', '刘', '杨', '黄', '赵', '周', '吴', '徐', '孙',
                   '马', '朱', '胡', '郭', '何', '高', '林', '罗', '郑', '梁', '谢', '宋']
CONTACT_GIVEN = ['晓峰', '志强', '建国', '海涛', '文斌', '凯', '磊', '敏', '娜', '静', '丽',
                 '鹏', '宇', '浩', '婷', '雪', '阳', '晨', '博', '振兴', '国栋', '雅琴',
                 '嘉怡', '明辉', '天宇', '子豪', '若曦', '书豪']
TITLES = ['采购经理', '采购主管', '老板', '店长', '厂长', '运营总监', '仓储主管', '区域经理',
          '集团采购总监', '库管', '技术负责人', '财务主管', '总经理']

# 与 products / 订单明细口径一致的主销品：[名称, 分类, 售价, 进价]
CATALOG = [
    ['机油滤芯', '保养滤芯类', 25, 12], ['空气滤芯', '保养滤芯类', 35, 18],
    ['空调滤芯', '保养滤芯类', 45, 22], ['燃油滤芯', '保养滤芯类', 52, 25],
    ['全合成机油', '油液类', 228, 130], ['变速箱油', '油液类', 88, 45],
    ['刹车油', '油液类', 48, 25], ['防冻液', '油液类', 68, 38],
    ['玻璃水', '油液类', 12, 6], ['火花塞', '点火/电气小件', 58, 28],
    ['近光灯泡', '点火/电气小件', 26, 12], ['继电器', '点火/电气小件', 20, 9],
    ['保险丝', '点火/电气小件', 22, 10], ['刹车片', '制动/底盘小件', 138, 68],
    ['刹车分泵修理包', '制动/底盘小件', 45, 20], ['防尘套', '制动/底盘小件', 30, 14],
    ['刹车报警线', '制动/底盘小件', 20, 9], ['雨刮片', '车身/橡胶小件', 38, 18],
    ['洗车液', '清洁养护耗材', 52, 26], ['内饰清洗剂', '清洁养护耗材', 32, 15],
    ['镀膜剂', '清洁养护耗材', 58, 28], ['轮胎蜡', '清洁养护耗材', 38, 18],
    ['擦车毛巾', '清洁养护耗材', 12, 5], ['清洁海绵', '清洁养护耗材', 8, 3],
]
PRODUCT_BY_NAME = {c[0]: {'category': c[1], 'price': c[2], 'cost': c[3]} for c in CATALOG}
PRODUCT_NAMES = [c[0] for c in CATALOG]
# 按行业给订单加一点「像真的」偏好
INDUSTRY_PRODUCTS = {
    '汽车维修': ['机油滤芯', '空气滤芯', '空调滤芯', '燃油滤芯', '刹车片', '火花塞', '全合成机油'],
    '4S 店': ['刹车片', '变速箱油', '火花塞', '全合成机油', '空气滤芯', '空调滤芯'],
    '洗车连锁': ['洗车液', '擦车毛巾', '清洁海绵', '镀膜剂', '内饰清洗剂', '轮胎蜡'],
    '汽车美容': ['镀膜剂', '洗车液', '擦车毛巾', '轮胎蜡', '内饰清洗剂', '雨刮片'],
    '汽配经销': ['火花塞', '继电器', '近光灯泡', '保险丝', '刹车片', '雨刮片'],
    '车队物流': ['全合成机油', '机油滤芯', '刹车片', '防冻液', '刹车油', '玻璃水'],
}

# ---------------------------------------------------------------- 现有客户（库里已有，复用，不重复插入）
EXISTING = [
    ('杭州车匠汽车维修连锁', '汽车维修', 'A', '陈晓峰', '138-1024-5566', '张明', '2026-09-12'),
    ('上海睿联汽车服务集团', '4S 店', 'A', '王强', '139-2288-7711', '李思', '2026-08-28'),
    ('深圳前海车享快修连锁', '汽车维修', 'B', '林雅', '137-6600-3322', '张明', '2026-10-06'),
    ('北京博远汽车美容中心', '汽车美容', 'B', '赵敏', '135-8899-1200', '周婷', '2026-07-19'),
    ('成都康泰汽配经销部', '汽配经销', 'C', '孙浩', '136-7788-4455', '李思', '2026-10-21'),
    ('广州速达洗车连锁', '洗车连锁', 'B', '黄磊', '150-6677-2233', '张明', '2026-06-30'),
    ('苏州精工汽车维修厂', '汽车维修', 'C', '周天成', '188-1122-3344', '周婷', '2026-11-01'),
    ('宁波甬城汽车服务有限公司', '汽车维修', 'B', '张伟', '138-5741-2088', '陈嘉伟', '2026-09-20'),
    ('合肥皖通汽配贸易行', '汽配经销', 'C', '刘敏', '139-5512-7788', '陈嘉伟', '2026-10-18'),
]

customers = [{'name': n, 'industry': i, 'level': lv, 'contact': c, 'phone': p, 'owner': o, 'create': cr}
             for n, i, lv, c, p, o, cr in EXISTING]
used_names = {c['name'] for c in customers}
used_phones = {c['phone'] for c in customers}
used_people = {(c['name'], c['contact']) for c in customers}


def new_phone():
    """11 位手机号，按 3-4-4 排版（和库里已有的 138-1024-5566 一致）"""
    while True:
        digits = '1' + str(random.choice([3, 5, 7, 8, 9])) + ''.join(
            str(random.randint(0, 9)) for _ in range(9))
        p = '%s-%s-%s' % (digits[:3], digits[3:7], digits[7:])
        if p not in used_phones:
            used_phones.add(p)
            return p


def new_person():
    return random.choice(CONTACT_SURNAME) + random.choice(CONTACT_GIVEN)


# ---------------------------------------------------------------- 客户：新增 56 家
TARGET_CUSTOMERS = 56
created = 0
attempts = 0
while created < TARGET_CUSTOMERS and attempts < 4000:
    attempts += 1
    city = random.choice(CITIES)
    brand = random.choice(BRANDS)
    industry = random.choice(INDUSTRIES)
    suffix = random.choice(INDUSTRY_TEMPLATE[industry])
    name = f'{city}{brand}{suffix}'
    if name in used_names:
        continue
    used_names.add(name)
    level = random.choices(['A', 'B', 'C'], weights=[2, 5, 3])[0]
    # A 类大客户归组长/总监，B/C 分给一线顾问，模拟真实的分层跟单
    if level == 'A':
        owner = random.choice(LEADERS + [DIRECTOR])
    else:
        owner = random.choice(REPS)
    person = new_person()
    phone = new_phone()
    created_days = random.randint(30, 320)
    customers.append({
        'name': name, 'industry': industry, 'level': level,
        'contact': person, 'phone': phone, 'owner': owner,
        'create': d(TODAY - timedelta(days=created_days)),
    })
    created += 1

NEW_CUSTOMERS = customers[len(EXISTING):]
customer_by_name = {c['name']: c for c in customers}

# ---------------------------------------------------------------- 联系人：新客户每个 1-2 人（老客户库里已有，不重复插）
contacts = []
for c in NEW_CUSTOMERS:
    people = [(c['contact'], random.choice(TITLES))]
    extra = random.random() < 0.62
    if extra:
        for _ in range(200):
            p = new_person()
            if p != c['contact']:
                people.append((p, random.choice(TITLES)))
                break
    for idx, (person, title) in enumerate(people):
        contacts.append({
            'name': person,
            'customer': c['name'],
            'title': title,
            'phone': c['phone'] if idx == 0 else new_phone(),
            'wechat': 'wx_%s%02d' % (person.encode('utf-8').hex()[:6], idx),
            'primary': '是' if idx == 0 else '否',
            'owner': c['owner'],
        })

# ---------------------------------------------------------------- 商机：55 条
STAGES = ['初步沟通', '需求确认', '方案报价', '商务谈判', '赢单', '输单']
STAGE_W = [16, 18, 20, 16, 18, 12]
STAGE_PRODUCT = {
    '初步沟通': '年度耗材框架', '需求确认': '保养件集采', '方案报价': '制动件年度供货',
    '商务谈判': '油液与三滤集采', '赢单': '洗车耗材年度供货', '输单': '点火电气件分销',
}
opportunities = []
shown = 0
for c in customers:
    if shown >= 55:
        break
    if c['level'] == 'C' and random.random() < 0.55:
        continue
    stage = random.choices(STAGES, weights=STAGE_W)[0]
    amount = random.choice([18000, 26000, 31000, 42000, 56000, 68000, 89000, 125000, 158000, 186000])
    amount += random.randint(0, 40) * 500
    expect = TODAY + timedelta(days=random.randint(-45, 110))
    opportunities.append({
        'name': '%s-%s' % (c['name'][:10], STAGE_PRODUCT[stage]),
        'customer': c['name'],
        'amount': amount,
        'stage': stage,
        'expectDate': d(expect),
        'owner': c['owner'],
    })
    shown += 1

# ---------------------------------------------------------------- 订单：2026-01 → 2026-11
MONTH_PLAN = {
    1: 14, 2: 12, 3: 18, 4: 17, 5: 20,
    6: 19, 7: 21, 8: 23, 9: 25, 10: 27, 11: 22,
}
LEVEL_QTY = {'A': (80, 520), 'B': (40, 260), 'C': (15, 130)}
orders = []
seq_by_day = {}


def next_seq(day):
    key = d(day)
    seq_by_day[key] = seq_by_day.get(key, 100) + 1
    return seq_by_day[key]


def pick_products(customer):
    pool = INDUSTRY_PRODUCTS[customer['industry']]
    count = random.choices([1, 2, 3, 4, 5], weights=[18, 30, 26, 16, 10])[0]
    picked = random.sample(pool, min(count, len(pool)))
    if random.random() < 0.35:
        extra = random.choice(PRODUCT_NAMES)
        if extra not in picked:
            picked.append(extra)
    return picked


for month, count in MONTH_PLAN.items():
    for _ in range(count):
        # 大客户下单频次更高
        customer = random.choices(customers, weights=[3 if c['level'] == 'A' else 2 if c['level'] == 'B' else 1
                                                     for c in customers])[0]
        max_day = 15 if month == 11 else 28
        day = random.randint(1, max_day)
        order_date = date(2026, month, day)
        seq = next_seq(order_date)
        order_no = 'SO%s%03d' % (order_date.strftime('%Y%m%d'), seq)
        low, high = LEVEL_QTY[customer['level']]
        items = []
        for product in pick_products(customer):
            info = PRODUCT_BY_NAME[product]
            qty = random.randint(low, high)
            qty = max(1, int(round(qty / 5.0)) * 5)
            items.append({'product': product, 'category': info['category'], 'qty': qty,
                          'price': info['price'], 'cost': info['cost'],
                          'amount': info['price'] * qty})
        amount = sum(i['amount'] for i in items)
        days_ago = (TODAY - order_date).days
        if days_ago > 90:
            status = random.choices(['已完成', '已付款', '部分回款', '已取消'], weights=[72, 16, 6, 6])[0]
        elif days_ago > 30:
            status = random.choices(['已完成', '已付款', '部分回款', '待付款', '已取消'],
                                    weights=[38, 24, 20, 14, 4])[0]
        else:
            status = random.choices(['已完成', '已付款', '部分回款', '待付款', '已取消'],
                                    weights=[14, 20, 26, 36, 4])[0]
        orders.append({
            'orderNo': order_no, 'customer': customer['name'], 'contact': customer['contact'],
            'phone': customer['phone'], 'amount': amount, 'status': status,
            'orderDate': order_date, 'owner': customer['owner'], 'items': items,
            'level': customer['level'],
        })

# ---------------------------------------------------------------- 应收 / 收款 / 发货
receivables, receipts, shipments = [], [], []
receipt_seq, ship_seq = 100, 100

for o in orders:
    if o['status'] == '已取消':
        continue
    total = o['amount']
    if o['status'] in ('已完成', '已付款'):
        received = total
    elif o['status'] == '部分回款':
        ratio = random.uniform(0.2, 0.72)
        received = int(round(total * ratio / 10.0)) * 10
        received = min(max(received, 100), total - 100)
    else:
        received = 0

    # 约定回款日：A 类客户 45 天账期，其余 30 天
    term = 45 if o['level'] == 'A' else 30
    due = o['orderDate'] + timedelta(days=term)
    receivables.append({
        'customer': o['customer'], 'orderNo': o['orderNo'], 'total': total,
        'received': received, 'dueDate': d(due), 'owner': o['owner'],
    })

    if received > 0:
        # 拆成 1-2 笔收款，最后一笔补足差额
        parts = []
        if random.random() < 0.3 and received > 2000:
            first = int(round(received * random.uniform(0.3, 0.7) / 10.0)) * 10
            parts = [first, received - first]
        else:
            parts = [received]
        for idx, part in enumerate(parts):
            receipt_seq += 1
            pay_day = o['orderDate'] + timedelta(days=random.randint(3, term + 5))
            if pay_day > TODAY:
                pay_day = TODAY - timedelta(days=random.randint(0, 3))
            receipts.append({
                'receiptNo': 'SK%s%03d' % (pay_day.strftime('%Y%m%d'), receipt_seq),
                'customer': o['customer'], 'orderNo': o['orderNo'], 'amount': part,
                'method': random.choices(['银行转账', '承兑汇票', '微信收款', '支付宝', '现金'],
                                         weights=[52, 16, 14, 10, 8])[0],
                'receiptDate': d(pay_day), 'operator': '刘洋',
                'note': random.choice(['按合同账期付款', '首笔预付，余款月底结清', '对账后一次性付清',
                                       '客户财务批量付款', '货到付款']),
                'owner': o['owner'],
            })

    # 发货：取消单不发；当天的单可能还没发
    if random.random() < 0.9:
        ship_seq += 1
        days_ago = (TODAY - o['orderDate']).days
        if days_ago > 14:
            ship_status = random.choices(['已签收', '已发出'], weights=[86, 14])[0]
        elif days_ago > 4:
            ship_status = random.choices(['已发出', '已签收', '待发货'], weights=[56, 24, 20])[0]
        else:
            ship_status = '待发货'
        ship_day = o['orderDate'] + timedelta(days=random.randint(1, 4))
        shipments.append({
            'shipNo': 'FH%s%03d' % (ship_day.strftime('%Y%m%d'), ship_seq),
            'orderNo': o['orderNo'], 'customer': o['customer'],
            'qty': sum(i['qty'] for i in o['items']),
            'carrier': random.choice(['德邦物流', '顺丰速运', '专线物流', '中通快运', '安能物流']),
            'trackingNo': '%s%02d%06d' % (random.choice(['DB', 'SF', 'ZX', 'ZT', 'AN']),
                                          random.randint(10, 99), random.randint(100000, 999999)),
            'shipDate': d(ship_day),
            'status': ship_status,
            'owner': '赵敏',
        })

# ---------------------------------------------------------------- 报价单
quotes = []
quote_seq = 100
for op in opportunities:
    if op['stage'] not in ('方案报价', '商务谈判', '赢单'):
        continue
    quote_seq += 1
    qdate = TODAY - timedelta(days=random.randint(3, 80))
    if op['stage'] == '赢单':
        status = '已转订单'
    elif op['stage'] == '商务谈判':
        status = random.choice(['已通过', '待审批'])
    else:
        status = random.choice(['待审批', '草稿', '已通过', '已失效'])
    quotes.append({
        'quoteNo': 'QT%s%03d' % (qdate.strftime('%Y%m%d'), quote_seq),
        'customer': op['customer'],
        'contact': customer_by_name[op['customer']]['contact'],
        'amount': op['amount'], 'status': status, 'quoteDate': d(qdate),
        'validTo': d(qdate + timedelta(days=15)), 'owner': op['owner'],
    })

# 临散报价：老客户常规补货也会先走报价单，凑出足够的报价列表数据
for _ in range(38):
    c = random.choice(customers)
    quote_seq += 1
    qdate = TODAY - timedelta(days=random.randint(1, 90))
    amount = random.choice([8600, 12400, 18600, 23500, 31200, 42800, 56600, 78400])
    quotes.append({
        'quoteNo': 'QT%s%03d' % (qdate.strftime('%Y%m%d'), quote_seq),
        'customer': c['name'], 'contact': c['contact'], 'amount': amount,
        'status': random.choices(['待审批', '已通过', '已转订单', '已失效', '草稿'],
                                 weights=[22, 26, 20, 16, 10])[0],
        'quoteDate': d(qdate), 'validTo': d(qdate + timedelta(days=15)),
        'owner': c['owner'],
    })

# ---------------------------------------------------------------- 退换货（挂已完成订单）
done_orders = [o for o in orders if o['status'] in ('已完成', '已付款')]
return_orders = []
return_seq = 100
for o in random.sample(done_orders, min(26, len(done_orders))):
    item = random.choice(o['items'])
    qty = max(2, int(round(item['qty'] * random.uniform(0.05, 0.2))))
    return_seq += 1
    apply_day = o['orderDate'] + timedelta(days=random.randint(4, 25))
    if apply_day > TODAY:
        apply_day = TODAY - timedelta(days=1)
    return_orders.append({
        'returnNo': 'RT%s%03d' % (apply_day.strftime('%Y%m%d'), return_seq),
        'orderNo': o['orderNo'], 'customer': o['customer'],
        'type': random.choice(['退货', '换货']), 'qty': qty,
        'amount': item['price'] * qty,
        'reason': random.choice([
            '运输途中挤压变形，外包装破损', '型号与订单不符，无法装车',
            '客户反馈批次质量波动，要求换新批次', '喷头 / 密封件缺陷导致渗漏',
            '库存积压，客户申请部分退货', '装车后出现异响，退回检测',
        ]),
        'status': random.choices(['待审核', '已受理', '已完成', '已驳回'], weights=[24, 26, 42, 8])[0],
        'applyDate': d(apply_day), 'owner': o['owner'],
    })

# ---------------------------------------------------------------- 售后工单
ISSUES = {
    '保养滤芯类': ['滤芯尺寸偏差装不进去', '滤纸破损导致漏油', '密封圈老化渗油'],
    '油液类': ['到货渗漏，桶身变形', '黏度与标号不符', '低温启动困难'],
    '点火/电气小件': ['装车后故障灯常亮', '火花塞间隙不一致', '灯泡寿命短'],
    '制动/底盘小件': ['刹车片装车后异响', '刹车抖动，制动距离变长', '防尘套开裂'],
    '车身/橡胶小件': ['雨刮片刮不干净', '胶条老化开裂'],
    '清洁养护耗材': ['喷头堵塞不出液', '浓度偏稀，清洁力不足', '桶盖密封不严'],
}
tickets = []
ticket_seq = 100
for _ in range(78):
    c = random.choice(customers)
    product = random.choice(INDUSTRY_PRODUCTS[c['industry']])
    category = PRODUCT_BY_NAME[product]['category']
    ticket_seq += 1
    created_at = datetime.combine(TODAY - timedelta(days=random.randint(0, 75)),
                                  datetime.min.time()) + timedelta(hours=random.randint(8, 19),
                                                                    minutes=random.randint(0, 59))
    days_ago = (TODAY - created_at.date()).days
    if days_ago < 3:
        status = random.choices(['待受理', '处理中'], weights=[62, 38])[0]
    elif days_ago < 15:
        status = random.choices(['处理中', '已解决', '待受理'], weights=[52, 34, 14])[0]
    else:
        status = random.choices(['已解决', '已关闭'], weights=[64, 36])[0]
    tickets.append({
        'ticketNo': 'GD%s%03d' % (created_at.strftime('%Y%m%d'), ticket_seq),
        'customer': c['name'], 'contact': c['contact'], 'product': product,
        'issue': random.choice(ISSUES[category]),
        'level': random.choices(['高', '中', '低'], weights=[22, 50, 28])[0],
        'status': status, 'createTime': created_at.strftime('%Y-%m-%d %H:%M'),
        'owner': random.choice(['孙浩', '吴磊']),
    })

# ---------------------------------------------------------------- 客户回访
revisits = []
for _ in range(72):
    c = random.choice(customers)
    when = datetime.combine(TODAY - timedelta(days=random.randint(0, 80)),
                            datetime.min.time()) + timedelta(hours=random.randint(9, 18),
                                                              minutes=random.randint(0, 59))
    product = random.choice(INDUSTRY_PRODUCTS[c['industry']])
    score = random.choices(['满意', '一般', '不满意'], weights=[72, 22, 6])[0]
    if score == '满意':
        content = f'{product}使用正常，客户对到货速度和包装满意'
        nxt = '下月常规回访'
    elif score == '一般':
        content = f'{product}基本可用，但客户希望交期再快 2 天'
        nxt = '跟催生产排期后回复客户'
    else:
        content = f'{product}出现质量问题，已转售后开单处理'
        nxt = '等工单处理结果再回访'
    revisits.append({
        'time': when.strftime('%Y-%m-%d %H:%M'), 'customer': c['name'], 'contact': c['contact'],
        'way': random.choice(['电话', '微信', '上门']), 'score': score,
        'content': content, 'next': nxt,
        'owner': random.choice(['孙浩', '吴磊', DIRECTOR]),
    })

# ---------------------------------------------------------------- 公海客户
pool = []
for _ in range(22):
    for _try in range(200):
        city = random.choice(CITIES)
        name = f"{city}{random.choice(BRANDS)}{random.choice(INDUSTRY_TEMPLATE['汽车维修'])}"
        if name not in used_names:
            used_names.add(name)
            break
    else:
        continue
    idle = random.randint(8, 70)
    pool.append({
        'name': name,
        'industry': random.choice(INDUSTRIES),
        'level': random.choices(['A', 'B', 'C'], weights=[1, 4, 5])[0],
        'city': city,
        'source': random.choice(['展会', '官网表单', '老客户推荐', '电话咨询', '广告投放']),
        'contact': new_person(), 'phone': new_phone(),
        'lastFollow': d(TODAY - timedelta(days=idle)),
        'idleDays': idle,
        'reason': random.choice(['超过 30 天未跟进', '销售离职交接', '客户主动退回', '报价后无回应', '长期未成交释放']),
    })

# ---------------------------------------------------------------- 跟进记录：每个客户 2-4 条
FOLLOW_CONTENT = [
    ('电话', '确认{product}的补货计划，客户希望本周内到货'),
    ('微信', '发送{product}最新报价单，客户在内部比价'),
    ('上门拜访', '带样件上门测试{product}，技术反馈良好'),
    ('电话', '催收上批货款，客户回复月底统一付款'),
    ('微信', '客户询问{product}的交期与最小起订量'),
    ('上门拜访', '核对季度对账单，确认下季度{product}用量'),
    ('电话', '客户反馈{product}使用中出现小问题，已转售后'),
]
followups = []
for c in customers:
    for _ in range(random.randint(2, 4)):
        way, tpl = random.choice(FOLLOW_CONTENT)
        product = random.choice(INDUSTRY_PRODUCTS[c['industry']])
        when = datetime.combine(TODAY - timedelta(days=random.randint(0, 120)),
                                datetime.min.time()) + timedelta(hours=random.randint(9, 18),
                                                                  minutes=random.randint(0, 59))
        followups.append({
            'time': when.strftime('%Y-%m-%d %H:%M'), 'customer': c['name'], 'contact': c['contact'],
            'type': way, 'content': tpl.format(product=product),
            'next': random.choice(['3 天内回访确认', '本周内发正式报价', '催回款', '等客户内部审批',
                                   '安排样件寄送', '下周上门复核']),
            'owner': c['owner'],
        })

# ---------------------------------------------------------------- 线索：150 条（营销看板的数据源）
CLUE_TITLES = [
    '{city}连锁门店保养滤芯年度采购', '{city}二类维修厂刹车片供货询价',
    '{city}洗车连锁清洁养护耗材试用', '{city}4S 店原厂替代件长期供货',
    '{city}车队机油与三滤集中采购', '{city}汽车美容店镀晶耗材补货',
    '{city}汽配城二级分销合作洽谈', '{city}快修连锁变速箱油招标',
]
clues = []
for _ in range(150):
    city = random.choice(CITIES)
    created_at = datetime.combine(TODAY - timedelta(days=random.randint(0, 160)),
                                  datetime.min.time()) + timedelta(hours=random.randint(8, 20),
                                                                    minutes=random.randint(0, 59))
    status = random.choices(['待分配', '待跟进', '跟进中', '已转化', '已废弃'],
                            weights=[28, 24, 26, 16, 6])[0]
    owner = '' if status == '待分配' else random.choice(REPS + LEADERS)
    assign_time = None
    assign_by = ''
    if owner:
        assign_time = created_at + timedelta(hours=random.randint(1, 30))
        assign_by = random.choice(['陈默', '王芳'])
    clues.append({
        'name': random.choice(CLUE_TITLES).format(city=city),
        'contact': new_person(), 'phone': new_phone(),
        'source': random.choices(['官网表单', '老客户推荐', '展会', '电话咨询', '广告投放'],
                                 weights=[30, 24, 20, 15, 15])[0],
        'intent': random.choices(['高', '中', '低'], weights=[26, 48, 26])[0],
        'status': status, 'owner': owner,
        'assignTime': assign_time.strftime('%Y-%m-%d %H:%M:%S') if assign_time else None,
        'assignBy': assign_by,
        'createTime': created_at.strftime('%Y-%m-%d %H:%M:%S'),
    })

# ---------------------------------------------------------------- 输出 SQL
out = []
out.append('-- ============================================================')
out.append('-- v20 演示数据扩充：客户 / 联系人 / 商机 / 订单 / 应收 / 收款 / 发货')
out.append('--      / 报价 / 退换货 / 工单 / 回访 / 公海 / 线索 / 跟进')
out.append('-- 生成器：Spring Boot/sql/gen_demo_dataset.py（随机种子固定，可重跑）')
out.append('-- 幂等：批次表 crm_seed_batches 记录 v20_demo_dataset，重复执行直接跳过')
out.append('-- ============================================================')
out.append('')
out.append("CREATE TABLE IF NOT EXISTS crm_seed_batches (")
out.append("  name varchar(60) NOT NULL PRIMARY KEY,")
out.append("  applied_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,")
out.append("  note varchar(200) DEFAULT NULL")
out.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;")
out.append('')
out.append('DROP PROCEDURE IF EXISTS seed_v20_demo;')
out.append('DELIMITER $$')
out.append('CREATE PROCEDURE seed_v20_demo()')
out.append('BEGIN')
out.append("  IF EXISTS (SELECT 1 FROM crm_seed_batches WHERE name='v20_demo_dataset') THEN")
out.append("    SELECT 'skip: v20_demo_dataset 已执行过' AS result;")
out.append('  ELSE')

# 客户
cust_rows = NEW_CUSTOMERS
for i in range(0, len(cust_rows), 40):
    chunk = cust_rows[i:i + 40]
    vals = ',\n    '.join('(%s,%s,%s,%s,%s,%s,%s)' % (
        sq(c['name']), sq(c['industry']), sq(c['level']), sq(c['contact']),
        sq(c['phone']), sq(c['owner']), sq(c['create'])) for c in chunk)
    out.append('    INSERT INTO crm_customers(name,industry,level,contact,phone,owner,create_time) VALUES')
    out.append('    ' + vals + ';')

# 联系人
def module_insert(table, rows, builder, owner_key='owner', scope_key='owner'):
    for i in range(0, len(rows), 50):
        chunk = rows[i:i + 50]
        vals = ',\n    '.join(builder(r) for r in chunk)
        out.append("    INSERT INTO crm_module_records(module_name,owner,scope_owner,payload) VALUES")
        out.append('    ' + vals + ';')


module_insert('contacts', contacts, lambda r: "('contacts',%s,%s,%s)" % (
    sq(r['owner']), sq(customer_by_name[r['customer']]['owner']), payload(r)))

# 商机
for i in range(0, len(opportunities), 40):
    chunk = opportunities[i:i + 40]
    vals = ',\n    '.join('(%s,%s,%s,%s,%s,%s)' % (
        sq(o['name']), sq(o['customer']), o['amount'], sq(o['stage']),
        sq(o['expectDate']), sq(o['owner'])) for o in chunk)
    out.append('    INSERT INTO crm_opportunities(name,customer,amount,stage,expect_date,owner) VALUES')
    out.append('    ' + vals + ';')

# 订单 + 明细
for i in range(0, len(orders), 50):
    chunk = orders[i:i + 50]
    vals = ',\n    '.join('(%s,%s,%s,%s,%s,%s,%s,%s)' % (
        sq(o['orderNo']), sq(o['customer']), sq(o['contact']), sq(o['phone']),
        o['amount'], sq(o['status']), sq(d(o['orderDate'])), sq(o['owner'])) for o in chunk)
    out.append('    INSERT INTO crm_orders(order_no,customer,contact,phone,amount,status,order_date,owner) VALUES')
    out.append('    ' + vals + ';')

item_rows = []
for o in orders:
    for idx, it in enumerate(o['items'], start=1):
        item_rows.append((o['orderNo'], it, idx))
for i in range(0, len(item_rows), 80):
    chunk = item_rows[i:i + 80]
    vals = ',\n    '.join('(%s,%s,%s,%d,%s,%s,%s,%d)' % (
        sq(no), sq(it['product']), sq(it['category']), it['qty'],
        it['price'], it['cost'], it['amount'], idx) for no, it, idx in chunk)
    out.append('    INSERT INTO crm_order_items(order_no,product,category,qty,price,cost,amount,sort_no) VALUES')
    out.append('    ' + vals + ';')

# 应收 / 收款 / 发货 / 报价 / 退换货 / 工单 / 回访 / 公海 / 跟进
module_insert('receivables', receivables, lambda r: "('receivables',%s,%s,%s)" % (
    sq(r['owner']), sq(r['owner']), payload(r)))
module_insert('receipts', receipts, lambda r: "('receipts',%s,%s,%s)" % (
    sq(r['owner']), sq(r['owner']), payload(r)))
module_insert('shipments', shipments, lambda r: "('shipments',%s,%s,%s)" % (
    sq(r['owner']), sq(r['owner']), payload(r)))
module_insert('quotes', quotes, lambda r: "('quotes',%s,%s,%s)" % (
    sq(r['owner']), sq(customer_by_name[r['customer']]['owner']), payload(r)))
module_insert('returns', return_orders, lambda r: "('returns',%s,%s,%s)" % (
    sq(r['owner']), sq(r['owner']), payload(r)))
module_insert('tickets', tickets, lambda r: "('tickets',%s,%s,%s)" % (
    sq(r['owner']), sq(customer_by_name[r['customer']]['owner']), payload(r)))
module_insert('revisits', revisits, lambda r: "('revisits',%s,%s,%s)" % (
    sq(r['owner']), sq(customer_by_name[r['customer']]['owner']), payload(r)))
module_insert('pool-customers', pool, lambda r: "('pool-customers','%s','%s',%s)" % ('', '', payload(r)))
module_insert('followups', followups, lambda r: "('followups',%s,%s,%s)" % (
    sq(r['owner']), sq(customer_by_name[r['customer']]['owner']), payload(r)))

# 线索
for i in range(0, len(clues), 50):
    chunk = clues[i:i + 50]
    vals = ',\n    '.join('(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)' % (
        sq(c['name']), sq(c['contact']), sq(c['phone']), sq(c['source']), sq(c['intent']),
        sq(c['status']), sq(c['owner']), sq(c['assignTime']), sq(c['assignBy']),
        sq(c['createTime'])) for c in chunk)
    out.append('    INSERT INTO crm_clues(name,contact,phone,source,intent,status,owner,assign_time,assign_by,create_time) VALUES')
    out.append('    ' + vals + ';')

out.append("    INSERT INTO crm_seed_batches(name,note) VALUES ('v20_demo_dataset',")
out.append("      '客户+%d 联系人+%d 商机+%d 订单+%d 明细+%d 应收+%d 收款+%d 发货+%d 报价+%d 退换货+%d 工单+%d 回访+%d 公海+%d 线索+%d 跟进+%d');"
           % (len(NEW_CUSTOMERS), len(contacts), len(opportunities), len(orders), len(item_rows),
              len(receivables), len(receipts), len(shipments), len(quotes), len(return_orders),
              len(tickets), len(revisits), len(pool), len(clues), len(followups)))
out.append("  END IF;")
out.append('END$$')
out.append('DELIMITER ;')
out.append('')
out.append('CALL seed_v20_demo();')
out.append('DROP PROCEDURE seed_v20_demo;')
out.append('')
out.append('-- 执行后的体量核对')
out.append('SELECT \'customers\' AS 表, COUNT(*) AS 行数 FROM crm_customers')
out.append('UNION ALL SELECT \'orders\', COUNT(*) FROM crm_orders')
out.append('UNION ALL SELECT \'order_items\', COUNT(*) FROM crm_order_items')
out.append('UNION ALL SELECT \'opportunities\', COUNT(*) FROM crm_opportunities')
out.append('UNION ALL SELECT \'clues\', COUNT(*) FROM crm_clues')
out.append('UNION ALL SELECT module_name, COUNT(*) FROM crm_module_records GROUP BY module_name;')

sql_text = '\n'.join(out) + '\n'
with open('v20_demo_dataset.sql', 'w', encoding='utf-8') as fh:
    fh.write(sql_text)

print('生成完成：v20_demo_dataset.sql')
print('客户 +%d / 联系人 +%d / 商机 +%d / 订单 +%d（明细 %d）/ 应收 %d / 收款 %d / 发货 %d'
      % (len(NEW_CUSTOMERS), len(contacts), len(opportunities), len(orders), len(item_rows),
         len(receivables), len(receipts), len(shipments)))
print('报价 %d / 退换货 %d / 工单 %d / 回访 %d / 公海 %d / 线索 %d / 跟进 %d'
      % (len(quotes), len(return_orders), len(tickets), len(revisits), len(pool), len(clues), len(followups)))
print('SQL 大小：%.1f KB' % (len(sql_text.encode('utf-8')) / 1024))
