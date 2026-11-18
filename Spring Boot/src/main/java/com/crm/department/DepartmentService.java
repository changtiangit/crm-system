package com.crm.department;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DepartmentService {
    private final DepartmentMapper mapper;

    public DepartmentService(DepartmentMapper mapper) {
        this.mapper = mapper;
    }

    public List<Department> tree() {
        List<Department> departments = mapper.findAllWithEmployeeCount();
        Map<Long, Department> byId = new LinkedHashMap<>();
        departments.forEach(department -> byId.put(department.getId(), department));
        List<Department> roots = new ArrayList<>();
        for (Department department : departments) {
            Department parent = byId.get(department.getParentId());
            if (parent == null) roots.add(department);
            else parent.getChildren().add(department);
        }
        return roots;
    }

    /**
     * 注册页用的部门选项树：只保留启用中的部门。
     * 父部门被停用时，其子部门会被提到顶层，避免注册页出现无法选择的下级。
     */
    public List<DepartmentOption> options() {
        List<Department> departments = mapper.findAllWithRole();
        Map<Long, DepartmentOption> byId = new LinkedHashMap<>();
        departments.stream()
                .filter(d -> d.getStatus() != null && d.getStatus() == 1)
                .forEach(d -> byId.put(d.getId(), new DepartmentOption(d.getId(), d.getName(), d.getRoleId(), d.getRoleName())));

        List<DepartmentOption> roots = new ArrayList<>();
        for (Department department : departments) {
            DepartmentOption option = byId.get(department.getId());
            if (option == null) continue;
            DepartmentOption parent = byId.get(department.getParentId());
            if (parent == null) roots.add(option);
            else parent.getChildren().add(option);
        }
        return roots;
    }

    /** 注册时按部门取默认角色：部门未配置角色则返回 null */
    public Long roleIdOf(Long deptId) {
        if (deptId == null) return null;
        Department department = mapper.findById(deptId);
        return department == null ? null : department.getRoleId();
    }

    /** 注册时校验部门是否可用 */
    public boolean isSelectable(Long id) {
        if (id == null) return false;
        Department department = mapper.findById(id);
        return department != null && department.getStatus() != null && department.getStatus() == 1;
    }

    public Department get(Long id) {
        Department department = mapper.findById(id);
        if (department == null) throw new IllegalArgumentException("部门不存在");
        return department;
    }

    @Transactional
    public Department create(DepartmentRequest request) {
        Department department = from(request);
        if (department.getParentId() != null) get(department.getParentId());
        mapper.insert(department);
        return mapper.findById(department.getId());
    }

    @Transactional
    public Department update(Long id, DepartmentRequest request) {
        Department department = from(request);
        department.setId(id);
        get(id);
        if (id.equals(department.getParentId())) throw new IllegalArgumentException("不能将部门设为自己的上级");
        if (department.getParentId() != null) get(department.getParentId());
        if (department.getParentId() != null && mapper.isDescendant(id, department.getParentId()) > 0) {
            throw new IllegalArgumentException("不能将部门移动到自己的下级部门");
        }
        mapper.update(department);
        return mapper.findById(id);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        get(id);
        mapper.updateStatus(id, status);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        if (mapper.countChildren(id) > 0) throw new IllegalArgumentException("请先移除子部门");
        if (mapper.countEmployees(id) > 0) throw new IllegalArgumentException("请先调整部门员工归属");
        mapper.delete(id);
    }

    private Department from(DepartmentRequest request) {
        Department department = new Department();
        department.setParentId(request.getParentId());
        department.setCode(request.getCode());
        department.setName(request.getName());
        department.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        department.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        department.setRoleId(request.getRoleId());
        return department;
    }
}
