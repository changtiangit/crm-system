package com.crm.department;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Department> tree() { return service.tree(); }

    /** 公开接口：注册页选择部门用，只返回启用中的部门 */
    @GetMapping("/options")
    public List<DepartmentOption> options() { return service.options(); }

    @GetMapping("/{id}")
    public Department get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    public Department create(@RequestBody DepartmentRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    public Department update(@PathVariable Long id, @RequestBody DepartmentRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public Map<String, String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        service.updateStatus(id, status);
        return Map.of("message", "部门状态已更新");
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("message", "部门已删除");
    }
}
