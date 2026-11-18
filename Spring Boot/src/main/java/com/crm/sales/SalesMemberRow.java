package com.crm.sales;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

/**
 * 销售团队成员 + 实时线索负载。
 * 团队成员就是员工表里带「销售组」的人，字段直接从 employees 读取，不再单独维护一份名单。
 */
public class SalesMemberRow {
    public Long id;
    public String empNo;
    public String name;
    public String phone;
    /** 职位：销售总监 / 销售组长 / 销售顾问 */
    public String position;
    /** 能力等级 S/A/B/C，决定智能分配时的权重 */
    public String level;
    /** 所属销售组 */
    public String grp;
    public Long leaderId;
    public String leaderName;
    public String deptName;
    public Integer status;
    /** 在手线索上限 */
    public Integer capacity;

    /** 以下两个只用于新增成员时写库，不返回给前端 */
    @JsonIgnore public String passwordHash;
    @JsonIgnore public Long departmentId;

    // ---- 以下由 Service 依据线索表实时计算 ----
    public Integer inHand;
    public Integer monthNew;
    public Integer converted;
    public Integer total;
    /** 剩余可接线索数 = 容量 - 在手 */
    public Integer remaining;
    /** 转化率（已转化 / 累计线索），百分比数值，保留一位小数 */
    public BigDecimal convRate;
    /** 负载率（在手 / 容量），百分比数值 */
    public BigDecimal loadRate;
    /** 能力权重：S 1.2 / A 1.1 / B 1.0 / C 0.8 */
    public BigDecimal weight;
    /** 是否还能接线索（在职 + 有剩余容量） */
    public Boolean assignable;
}
