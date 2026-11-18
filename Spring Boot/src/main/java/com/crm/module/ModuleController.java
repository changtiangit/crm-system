package com.crm.module;

import com.crm.common.AuthInterceptor;
import com.crm.common.DataScopeService;
import com.crm.common.ForbiddenException;
import com.crm.common.PermissionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/modules")
public class ModuleController {
    private final ModuleService service;
    private final DataScopeService scopes;
    private final PermissionService permissions;

    public ModuleController(ModuleService service, DataScopeService scopes, PermissionService permissions) {
        this.service = service;
        this.scopes = scopes;
        this.permissions = permissions;
    }

    @GetMapping("/{module}")
    public List<Map<String, Object>> list(@PathVariable String module,
            @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        requirePermission(module, "view", userId);
        return service.list(module, scopes.of(userId));
    }

    @PostMapping("/{module}/seed")
    public List<Map<String, Object>> seed(@PathVariable String module, @RequestBody List<Map<String, Object>> rows,
            @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        requirePermission(module, "view", userId);
        return service.seed(module, rows, scopes.of(userId));
    }

    @PostMapping("/{module}")
    public Map<String, Object> create(@PathVariable String module, @RequestBody Map<String, Object> data,
            @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        requirePermission(module, "add", userId);
        return service.create(module, data, scopes.of(userId));
    }

    @PutMapping("/{module}/{id}")
    public Map<String, Object> update(@PathVariable String module, @PathVariable Long id,
            @RequestBody Map<String, Object> data, @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        requirePermission(module, "edit", userId);
        return service.update(module, id, data, scopes.of(userId));
    }

    @DeleteMapping("/{module}/{id}")
    public void delete(@PathVariable String module, @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        requirePermission(module, "delete", userId);
        service.delete(module, id, scopes.of(userId));
    }

    private void requirePermission(String module, String action, Long userId) {
        String code = switch (module + ":" + action) {
            case "pool-customers:view" -> "pool:view";
            case "pool-customers:delete" -> "pool:claim";
            case "contacts:view" -> "contact:view";
            case "contacts:add" -> "contact:add";
            case "contacts:edit" -> "contact:edit";
            case "contacts:delete" -> "contact:delete";
            case "followups:view" -> "followup:view";
            case "followups:add" -> "followup:add";
            case "followups:edit" -> "followup:edit";
            case "followups:delete" -> "followup:delete";
            case "todos:view" -> "todo:view";
            case "todos:add" -> "todo:add";
            case "todos:edit", "todos:delete" -> "todo:done";
            case "quotes:view" -> "quote:view";
            case "quotes:add" -> "quote:add";
            case "quotes:edit" -> "quote:edit";
            case "quotes:delete" -> "quote:delete";
            case "returns:view" -> "return:view";
            case "returns:add" -> "return:add";
            case "returns:edit", "returns:delete" -> "return:edit";
            case "shipments:view" -> "delivery:view";
            case "shipments:add", "shipments:edit", "shipments:delete" -> "delivery:add";
            case "receipts:view" -> "receipt:view";
            case "receipts:add", "receipts:edit", "receipts:delete" -> "receipt:add";
            case "receivables:view" -> "receivable:view";
            case "receivables:edit" -> "receivable:confirm";
            case "tickets:view" -> "ticket:view";
            case "tickets:add" -> "ticket:add";
            case "tickets:edit", "tickets:delete" -> "ticket:edit";
            case "revisits:view" -> "revisit:view";
            case "revisits:add" -> "revisit:add";
            case "revisits:edit", "revisits:delete" -> "revisit:edit";
            default -> null;
        };
        if (code == null || !permissions.has(userId, code))
            throw new ForbiddenException("当前账号没有该操作权限");
    }
}
