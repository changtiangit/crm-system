package com.crm.business;

import java.math.BigDecimal;

/** 订单明细行：这张订单卖了什么、几件、单价与成本，金额由单价×数量得出 */
public class OrderItemRow {
    public Long id;
    public String orderNo;
    public String product;
    public String category;
    public Integer qty;
    public BigDecimal price;
    public BigDecimal cost;
    public BigDecimal amount;
    public Integer sortNo;
}
