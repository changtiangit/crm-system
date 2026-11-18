package com.crm.employee;

import com.crm.common.AuthInterceptor;
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
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    public List<EmployeeView> list() {
        return service.list();
    }

    @PostMapping
    public EmployeeView create(@RequestBody EmployeeService.EmployeeRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EmployeeView update(
            @PathVariable Long id,
            @RequestBody EmployeeService.EmployeeRequest request,
            @RequestAttribute(AuthInterceptor.USER_ID) Long operatorId) {
        return service.update(id, request, operatorId);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.USER_ID) Long operatorId) {
        service.delete(id, operatorId);
        return Map.of("message", "员工已删除");
    }
}
