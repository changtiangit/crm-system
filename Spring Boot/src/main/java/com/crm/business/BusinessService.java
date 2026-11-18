package com.crm.business;

import com.crm.common.DataScope;
import com.crm.common.ForbiddenException;
import com.crm.module.ModuleSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BusinessService {
    private final BusinessMapper mapper;
    private final ModuleSyncService sync;

    public BusinessService(BusinessMapper mapper, ModuleSyncService sync) {
        this.mapper = mapper;
        this.sync = sync;
    }

    /**
     * 列表按数据范围过滤：全公司不限制；组长看本组；销售只看本人。
     * 线索里「待分配」归公司所有，组长可见并可拉进本组，销售看不到。
     */
    public List<BusinessRow> list(String type, DataScope scope, boolean canSeeProfit) {
        List<String> owners = scope.isAll() ? null : scope.owners();
        return switch (type) {
            case "clues" -> mapper.clues(owners, scope.isGroup());
            case "customers" -> mapper.customers(owners);
            case "opportunities" -> mapper.opportunities(owners);
            case "orders" -> ordersWithItems(owners, canSeeProfit);
            default -> throw new IllegalArgumentException("未知业务类型");
        };
    }

    /** 订单带上商品明细：列表页要显示构成，也方便前端直接算件数 */
    private List<BusinessRow> ordersWithItems(List<String> owners, boolean canSeeProfit) {
        List<BusinessRow> rows = mapper.orders(owners);
        if (rows.isEmpty()) return rows;
        // 明细只挂到本页可见的订单上，避免把别家的清单带出去
        Set<String> visible = rows.stream().map((r) -> r.orderNo).collect(Collectors.toSet());
        Map<String, List<OrderItemRow>> grouped = mapper.orderItems().stream()
                .filter((i) -> visible.contains(i.orderNo))
                .collect(Collectors.groupingBy(i -> i.orderNo));
        rows.forEach(r -> r.items = grouped.getOrDefault(r.orderNo, List.of()));
        // 没有「成本与毛利」权限的账号，接口里就不带成本，前端即使用调试工具也拿不到进价
        if (!canSeeProfit) {
            rows.forEach(r -> r.items.forEach(i -> i.cost = null));
        }
        return rows;
    }

    @Transactional
    public BusinessRow create(String type, BusinessRow row, DataScope scope, boolean canSeeProfit) {
        if ("orders".equals(type)) {
            normalizeOrderItems(row);
            if (!canSeeProfit) applyProductCosts(row);
        }
        if ("clues".equals(type)) normalizeClue(row);
        applyOwner(type, row, scope);
        validate(type, row);
        switch (type) {
            case "clues" -> mapper.insertClue(row);
            case "customers" -> mapper.insertCustomer(row);
            case "opportunities" -> mapper.insertOpportunity(row);
            case "orders" -> {
                mapper.insertOrder(row);
                saveOrderItems(row);
                // 下单即开账：生成应收与待发货单，并扣减库存
                sync.afterOrderCreated(row);
            }
            default -> throw new IllegalArgumentException("未知业务类型");
        }
        return row;
    }

    @Transactional
    public BusinessRow update(String type, Long id, BusinessRow row, DataScope scope, boolean canSeeProfit) {
        row.id = id;
        requireManage(type, id, scope);
        // 明细和订单头一起提交：先把明细整理干净（金额、序号按明细重算），再校验
        if ("orders".equals(type)) {
            normalizeOrderItems(row);
            if (!canSeeProfit) applyProductCosts(row);
        }
        if ("clues".equals(type)) normalizeClue(row);
        applyOwner(type, row, scope);
        validate(type, row);
        if ("orders".equals(type)) {
            // 单号被改过时，旧单号下的明细也要清掉，否则会留下一堆孤儿行
            String oldNo = mapper.orderNoOf(id);
            int changed = mapper.updateOrder(row);
            if (changed == 0) throw new IllegalArgumentException("记录不存在");
            if (oldNo != null && !oldNo.equals(row.orderNo)) mapper.deleteOrderItems(oldNo);
            saveOrderItems(row);
            // 金额、客户、单号改了，应收与发货单要跟着对齐
            sync.afterOrderUpdated(row);
            return row;
        }
        int changed = switch (type) {
            case "clues" -> mapper.updateClue(row);
            case "customers" -> mapper.updateCustomer(row);
            case "opportunities" -> mapper.updateOpportunity(row);
            default -> throw new IllegalArgumentException("未知业务类型");
        };
        if (changed == 0) throw new IllegalArgumentException("记录不存在或没有发生变化");
        return row;
    }

    @Transactional
    public void delete(String type, Long id, DataScope scope) {
        requireManage(type, id, scope);
        if ("orders".equals(type)) {
            String orderNo = mapper.orderNoOf(id);
            if (orderNo != null) {
                // 已经收过钱的单不能删：删掉合同凭证，财务的收款、应收、发货会全部挂空
                if (sync.receiptsPaid(orderNo).signum() > 0)
                    throw new IllegalArgumentException("该订单已有收款记录，请先在财务冲销收款后再删除");
                sync.removeOrderLinks(orderNo);
                // 订单删了，明细不能留在库里
                mapper.deleteOrderItems(orderNo);
            }
        }
        int changed = switch (type) {
            case "clues" -> mapper.deleteClue(id);
            case "customers" -> mapper.deleteCustomer(id);
            case "opportunities" -> mapper.deleteOpportunity(id);
            case "orders" -> mapper.deleteOrder(id);
            default -> throw new IllegalArgumentException("未知业务类型");
        };
        if (changed == 0) throw new IllegalArgumentException("记录不存在");
    }

    /**
     * 写入前把负责人收敛到当前账号的数据范围内：
     * 销售只能写自己名下；组长可以写本组成员；管理员/总监保持原样。
     */
    private void applyOwner(String type, BusinessRow row, DataScope scope) {
        if (scope.isAll()) {
            if (row.owner == null) row.owner = "";
            return;
        }
        String owner = row.owner == null ? "" : row.owner.trim();
        if (owner.isEmpty()) {
            // 线索留空表示回到待分配池；客户、商机、订单留空则默认归自己
            row.owner = ("clues".equals(type) && scope.isGroup()) ? "" : scope.employeeName();
            return;
        }
        if (!scope.canSee(owner)) throw new ForbiddenException(scope.denyMessage(owner));
        row.owner = owner;
    }

    /** 改 / 删之前校验数据归属，越权直接 403，而不是「看不到但能改」 */
    private void requireManage(String type, Long id, DataScope scope) {
        String owner = switch (type) {
            case "clues" -> mapper.clueOwner(id);
            case "customers" -> mapper.customerOwner(id);
            case "opportunities" -> mapper.opportunityOwner(id);
            case "orders" -> mapper.orderOwner(id);
            default -> throw new IllegalArgumentException("未知业务类型");
        };
        if (owner == null) throw new IllegalArgumentException("记录不存在");
        if (!scope.canManage(owner)) throw new ForbiddenException(scope.denyMessage(owner));
    }

    /**
     * 整理线索：意向等级兜底；没有负责人就是「待分配」，有负责人但还挂着待分配就顺延为待跟进。
     * 分配动作本身由 SalesService 负责，这里只保证页面手工新建 / 编辑时状态与负责人不打架。
     */
    private void normalizeClue(BusinessRow row) {
        if (row.intent == null || row.intent.isBlank()) row.intent = "中";
        if (row.owner == null) row.owner = "";
        row.owner = row.owner.trim();
        if (row.owner.isEmpty()) {
            row.status = "待分配";
        } else if (row.status == null || row.status.isBlank() || "待分配".equals(row.status)) {
            row.status = "待跟进";
        }
    }

    /**
     * 整理订单明细：丢掉空行、校验数量与单价、按「单价 × 数量」重算行金额与订单总额。
     * 有明细时以明细合计为准，避免表头金额和清单对不上。
     */
    private void normalizeOrderItems(BusinessRow row) {
        if (row.items == null) return;
        List<OrderItemRow> clean = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (OrderItemRow item : row.items) {
            if (item == null || blank(item.product)) continue;
            if (!seen.add(item.product))
                throw new IllegalArgumentException("同一张订单里「" + item.product + "」只能有一行，请合并数量");
            if (item.qty == null || item.qty <= 0)
                throw new IllegalArgumentException("「" + item.product + "」的数量要大于 0");
            if (item.price == null || item.price.signum() < 0)
                throw new IllegalArgumentException("「" + item.product + "」的单价不能为负数");
            if (item.cost == null) item.cost = BigDecimal.ZERO;
            item.amount = item.price.multiply(BigDecimal.valueOf(item.qty));
            item.sortNo = clean.size() + 1;
            clean.add(item);
        }
        row.items = clean;
        if (!clean.isEmpty()) {
            row.amount = clean.stream()
                    .map(i -> i.amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /**
     * 没有成本字段权限时，明细成本按商品资料里的进价补齐。
     * 前端拿不到进价、就算手工改成 0，也不会把毛利算错。
     */
    private void applyProductCosts(BusinessRow row) {
        if (row.items == null || row.items.isEmpty()) return;
        Map<String, BigDecimal> costs = mapper.productCosts().stream()
                .filter((c) -> c.product != null && c.cost != null)
                .collect(Collectors.toMap((c) -> c.product, (c) -> c.cost, (a, b) -> a));
        for (OrderItemRow item : row.items) {
            item.cost = costs.getOrDefault(item.product, BigDecimal.ZERO);
        }
    }

    /** 明细整单替换：先按单号清空，再按当前顺序写回，前端不必自己算增删改 */
    private void saveOrderItems(BusinessRow row) {
        if (row.items == null) return;
        mapper.deleteOrderItems(row.orderNo);
        for (OrderItemRow item : row.items) {
            item.orderNo = row.orderNo;
            mapper.insertOrderItem(item);
        }
    }

    private void validate(String type, BusinessRow row) {
        if (row == null) throw new IllegalArgumentException("提交数据不能为空");
        if ("orders".equals(type)) {
            if (blank(row.orderNo)) throw new IllegalArgumentException("订单编号不能为空");
        } else if (blank(row.name)) {
            throw new IllegalArgumentException("名称不能为空");
        }
        if ("clues".equals(type) && (blank(row.contact) || blank(row.phone) || blank(row.source) || blank(row.status)))
            throw new IllegalArgumentException("线索联系人、手机号、来源和状态不能为空");
        if ("customers".equals(type) && (blank(row.industry) || blank(row.level) || blank(row.contact) || blank(row.phone)))
            throw new IllegalArgumentException("客户行业、级别、联系人和电话不能为空");
        if ("opportunities".equals(type) && (blank(row.customer) || row.amount == null || blank(row.stage) || row.expectDate == null))
            throw new IllegalArgumentException("商机客户、金额、阶段和预计成交日期不能为空");
        if ("orders".equals(type) && (blank(row.orderNo) || blank(row.customer) || blank(row.contact) || row.amount == null || row.orderDate == null))
            throw new IllegalArgumentException("订单编号、客户、联系人、金额和日期不能为空");
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
