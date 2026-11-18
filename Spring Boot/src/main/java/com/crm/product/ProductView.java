package com.crm.product;

import java.math.BigDecimal;

/** 返回给前端的商品信息 */
public record ProductView(
        Long id,
        String code,
        String name,
        String category,
        String spec,
        String unit,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        Integer stock,
        Integer status,
        String remark,
        String techNote,
        String createdAt
) {
}
