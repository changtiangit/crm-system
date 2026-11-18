package com.crm.business;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BusinessRow {
    public Long id;
    public String name;
    public String contact;
    public String phone;
    public String source;
    /** 线索意向等级：高 / 中 / 低 */
    public String intent;
    public String status;
    public String owner;
    /** 线索最近一次分配时间与分配人（由分配接口写入） */
    public String assignTime;
    public String assignBy;
    /**
     * 创建时间：直接按数据库的 yyyy-MM-dd HH:mm:ss 返回。
     * 用 String 而不是 LocalDateTime，是为了让客户端传 "2026-11-12" 这种纯日期也不会反序列化失败
     * （新增时本字段由数据库默认值生成，前端不必提交）。
     */
    public String createTime;
    public String industry;
    public String level;
    public String customer;
    public BigDecimal amount;
    public String stage;
    public LocalDate expectDate;
    public String orderNo;
    public LocalDate orderDate;
    /** 仅订单返回：该单的商品明细，列表页据此显示「N 种商品 · N 件」 */
    public List<OrderItemRow> items;
}
