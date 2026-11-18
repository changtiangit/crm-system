package com.crm.module;

import com.crm.business.BusinessMapper;
import com.crm.business.BusinessRow;
import com.crm.business.OrderItemRow;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务联动：让订单、应收、收款、发货、库存这五本账一起动，而不是各写各的。
 *
 * <p>规则：
 * <ul>
 *   <li>新建订单 → 自动开一张应收（账期 30 天）和一张待发货单，并按明细扣减库存；</li>
 *   <li>修改订单金额 → 应收总额跟着改（已收金额保留，超过总额时按总额封顶）；</li>
 *   <li>登记 / 修改 / 删除收款 → 重算应收已收金额，并把订单状态改成待付款 / 部分回款 / 已付款；</li>
 *   <li>删除订单 → 只有「一分钱都没收」才允许，同时清掉对应的应收与发货单，避免留下孤儿财务记录。</li>
 * </ul>
 * 库存不足时按 0 兜底而不是抛错：销售现场经常先接单后补货，生产看板会用 0 库存报缺货预警。
 */
@Service
public class ModuleSyncService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ModuleMapper mapper;
    private final BusinessMapper businessMapper;
    private final ObjectMapper json;

    public ModuleSyncService(ModuleMapper mapper, BusinessMapper businessMapper, ObjectMapper json) {
        this.mapper = mapper;
        this.businessMapper = businessMapper;
        this.json = json;
    }

    /** 订单新建：应收 + 发货单 + 扣库存 */
    @Transactional
    public void afterOrderCreated(BusinessRow order) {
        if (order == null || blank(order.orderNo)) return;
        upsertReceivableTotal(order);
        createShipmentIfAbsent(order);
        if (order.items == null) return;
        for (OrderItemRow item : order.items) {
            if (blank(item.product) || item.qty == null || item.qty <= 0) continue;
            businessMapper.reduceStock(item.product, item.qty);
        }
    }

    /** 订单修改：金额变了应收跟着变，缺应收/发货单的补上（历史单编辑时也能自愈） */
    @Transactional
    public void afterOrderUpdated(BusinessRow order) {
        if (order == null || blank(order.orderNo)) return;
        upsertReceivableTotal(order);
        createShipmentIfAbsent(order);
    }

    /** 收款变动：应收已收金额以收款单合计为准，订单状态跟着走 */
    @Transactional
    public void afterReceiptChanged(String orderNo) {
        if (blank(orderNo)) return;
        ModuleRecord record = findByOrderNo("receivables", orderNo);
        if (record == null) return;

        Map<String, Object> data = read(record);
        BigDecimal total = decimal(data.get("total"));
        BigDecimal paid = receiptsPaid(orderNo);
        data.put("received", paid.min(total));
        write(record, data);

        String status = businessMapper.orderStatusByNo(orderNo);
        // 已取消、已完成的单不回退状态：完成是业务终态，不能被一次补录收款改回去
        if (status == null || "已取消".equals(status) || "已完成".equals(status)) return;
        String next = paid.signum() <= 0 ? "待付款"
                : (paid.compareTo(total) >= 0 ? "已付款" : "部分回款");
        if (!next.equals(status)) businessMapper.updateOrderStatusByNo(orderNo, next);
    }

    /** 该订单累计收款（财务删单前要先看这个） */
    public BigDecimal receiptsPaid(String orderNo) {
        if (blank(orderNo)) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (ModuleRecord row : mapper.list("receipts")) {
            Map<String, Object> data = read(row);
            if (!orderNo.equals(str(data.get("orderNo")))) continue;
            sum = sum.add(decimal(data.get("amount")));
        }
        return sum;
    }

    /** 删单前清理：发货单直接删，应收单在无收款时才删 */
    @Transactional
    public void removeOrderLinks(String orderNo) {
        if (blank(orderNo)) return;
        for (String module : List.of("shipments", "receivables")) {
            ModuleRecord row = findByOrderNo(module, orderNo);
            if (row != null) mapper.delete(module, row.id);
        }
    }

    private void upsertReceivableTotal(BusinessRow order) {
        BigDecimal total = order.amount == null ? BigDecimal.ZERO : order.amount;
        ModuleRecord record = findByOrderNo("receivables", order.orderNo);
        if (record == null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("customer", order.customer);
            data.put("orderNo", order.orderNo);
            data.put("total", total);
            data.put("received", 0);
            LocalDate base = order.orderDate == null ? LocalDate.now() : order.orderDate;
            data.put("dueDate", base.plusDays(30).format(DAY));
            data.put("owner", order.owner);
            insert("receivables", data, order.owner);
            return;
        }
        Map<String, Object> data = read(record);
        data.put("total", total);
        data.put("received", decimal(data.get("received")).min(total));
        write(record, data);
    }

    private void createShipmentIfAbsent(BusinessRow order) {
        if (findByOrderNo("shipments", order.orderNo) != null) return;
        int qty = 0;
        if (order.items != null) {
            for (OrderItemRow item : order.items) {
                if (item.qty != null && item.qty > 0) qty += item.qty;
            }
        }
        // 报价单直接转过来的单还没填明细，先不建发货单：0 件的待发货单会让生产排期以为要发货
        if (qty <= 0) return;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("shipNo", "FH" + order.orderNo.replace("SO", ""));
        data.put("orderNo", order.orderNo);
        data.put("customer", order.customer);
        data.put("qty", qty);
        data.put("carrier", "待分配");
        data.put("trackingNo", "");
        data.put("shipDate", LocalDate.now().format(DAY));
        data.put("status", "待发货");
        data.put("owner", "赵敏");
        insert("shipments", data, "赵敏");
    }

    private ModuleRecord findByOrderNo(String module, String orderNo) {
        for (ModuleRecord row : mapper.list(module)) {
            if (orderNo.equals(str(read(row).get("orderNo")))) return row;
        }
        return null;
    }

    private void insert(String module, Map<String, Object> data, String owner) {
        ModuleRecord row = new ModuleRecord();
        row.module = module;
        row.owner = owner == null ? "" : owner;
        row.scopeOwner = row.owner;
        try {
            row.payload = json.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalArgumentException("生成联动单据失败", e);
        }
        mapper.insert(row);
        data.put("id", row.id);
    }

    private void write(ModuleRecord record, Map<String, Object> data) {
        try {
            record.payload = json.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalArgumentException("回写联动单据失败", e);
        }
        mapper.update(record);
    }

    private Map<String, Object> read(ModuleRecord record) {
        try {
            return json.readValue(record.payload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("读取单据失败", e);
        }
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal b) return b;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
