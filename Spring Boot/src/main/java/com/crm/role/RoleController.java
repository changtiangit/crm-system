package com.crm.role;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    public record PermissionRequest(List<String> codes) {
    }

    @GetMapping
    public List<RoleView> list() {
        return service.list();
    }

    @PostMapping
    public RoleView create(@RequestBody RoleService.RoleRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public RoleView update(@PathVariable Long id, @RequestBody RoleService.RoleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("message", "角色已删除");
    }

    @PutMapping("/{id}/permissions")
    public Map<String, Object> setPermissions(@PathVariable Long id, @RequestBody PermissionRequest request) {
        service.setPermissions(id, request.codes());
        return Map.of("message", "授权已保存", "count", request.codes() == null ? 0 : request.codes().size());
    }
}
