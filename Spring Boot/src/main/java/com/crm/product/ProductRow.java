package com.crm.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商品表 products 的行对象 */
public class ProductRow {
    public Long id;
    public String code;
    public String name;
    public String category;
    public String spec;
    public String unit;
    public BigDecimal purchasePrice;
    public BigDecimal salePrice;
    public Integer stock;
    public Integer status;
    public String remark;
    /** 技术参数与维修说明，由售后角色维护 */
    public String techNote;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
