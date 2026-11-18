package com.crm.business;

import com.crm.common.AuthInterceptor;
import com.crm.common.DataScope;
import com.crm.common.DataScopeService;
import com.crm.common.ForbiddenException;
import com.crm.common.PermissionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/business")
public class BusinessController {
    private final BusinessService service;
    private final DataScopeService scopeService;
    private final PermissionService permissions;

    public BusinessController(BusinessService service, DataScopeService scopeService, PermissionService permissions) {
        this.service = service;
        this.scopeService = scopeService;
        this.permissions = permissions;
    }

    @GetMapping("/{type}")
    public List<BusinessRow> list(@PathVariable String type,
                                  @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        requirePermission(type, "view", userId);
        return service.list(type, scope(userId), canSeeProfit(userId));
    }

    @PostMapping("/{type}")
    public BusinessRow create(@PathVariable String type, @RequestBody BusinessRow row,
                              @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        requirePermission(type, "add", userId);
        return service.create(type, row, scope(userId), canSeeProfit(userId));
    }

    @PutMapping("/{type}/{id}")
    public BusinessRow update(@PathVariable String type, @PathVariable Long id, @RequestBody BusinessRow row,
                              @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        requirePermission(type, "edit", userId);
        return service.update(type, id, row, scope(userId), canSeeProfit(userId));
    }

    @DeleteMapping("/{type}/{id}")
    public void delete(@PathVariable String type, @PathVariable Long id,
                       @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        requirePermission(type, "delete", userId);
        service.delete(type, id, scope(userId));
    }

    private void requirePermission(String type, String action, Long userId) {
        String code = switch (type + ":" + action) {
            case "clues:view" -> "clue:view";
            case "clues:add" -> "clue:add";
            case "clues:edit" -> "clue:edit";
            case "clues:delete" -> "clue:delete";
            case "customers:view" -> "customer:view";
            case "customers:add" -> "customer:add";
            case "customers:edit" -> "customer:edit";
            case "customers:delete" -> "customer:delete";
            case "opportunities:view" -> "opportunity:view";
            case "opportunities:add" -> "opportunity:add";
            case "opportunities:edit" -> "opportunity:edit";
            case "opportunities:delete" -> "opportunity:delete";
            case "orders:view" -> "order:view";
            case "orders:add" -> "order:add";
            case "orders:edit" -> "order:edit";
            case "orders:delete" -> "order:delete";
            default -> null;
        };
        if (code == null || !permissions.has(userId, code))
            throw new ForbiddenException("当前账号没有该业务操作权限");
    }

    private DataScope scope(Long userId) {
        return scopeService.of(userId);
    }

    /** 成本与毛利属于字段级权限：销售组长 / 销售顾问拿不到，接口直接不下发、也不采信前端提交的成本 */
    private boolean canSeeProfit(Long userId) {
        return permissions.has(userId, PermissionService.ORDER_PROFIT);
    }
}
