package com.crm.common;

import com.crm.auth.AuthMapper;
import org.springframework.stereotype.Service;

/**
 * 功能权限（页面 / 按钮 / 字段）判定。
 *
 * <p>权限点在角色上维护，登录接口已经把并集算好发给前端；后端在需要「字段级」控制的地方
 * （例如销售不能看商品进价、订单成本与毛利）再查一次，避免只靠前端隐藏、接口仍能把成本带走。
 */
@Service
public class PermissionService {
    /** 查看商品进价 / 成本 */
    public static final String PRODUCT_COST = "product:cost:view";
    /** 查看订单成本、毛利额与毛利率 */
    public static final String ORDER_PROFIT = "order:profit:view";

    private final AuthMapper mapper;

    public PermissionService(AuthMapper mapper) {
        this.mapper = mapper;
    }

    public boolean has(Long employeeId, String code) {
        if (employeeId == null || code == null || code.isBlank()) return false;
        return mapper.permissionCodesOf(employeeId).contains(code);
    }
}
