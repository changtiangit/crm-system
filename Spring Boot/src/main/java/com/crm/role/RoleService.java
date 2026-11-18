package com.crm.role;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {
    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final RoleMapper mapper;

    public RoleService(RoleMapper mapper) {
        this.mapper = mapper;
    }

    public record RoleRequest(
            String name,
            String code,
            Long parentId,
            String description,
            Integer status
    ) {
    }

    @Transactional(readOnly = true)
    public List<RoleView> list() {
        List<RoleView> views = new ArrayList<>();
        for (RoleRow row : mapper.findAll()) views.add(toView(row));
        return views;
    }

    @Transactional
    public RoleView create(RoleRequest request) {
        validate(request, null);
        RoleRow row = new RoleRow();
        row.code = request.code().trim().toUpperCase();
        row.name = request.name().trim();
        row.description = request.description();
        row.status = request.status() == null ? 1 : request.status();
        row.parentId = request.parentId();
        mapper.insert(row);
        return toView(mapper.findById(row.id));
    }

    @Transactional
    public RoleView update(Long id, RoleRequest request) {
        RoleRow exists = mapper.findById(id);
        if (exists == null) throw new IllegalArgumentException("角色不存在");
        if (ADMIN_ROLE_CODE.equals(exists.code) && !ADMIN_ROLE_CODE.equals(request.code().trim().toUpperCase())) {
            throw new IllegalArgumentException("内置管理员角色的编号不可修改");
        }
        validate(request, id);

        RoleRow row = new RoleRow();
        row.id = id;
        row.code = request.code().trim().toUpperCase();
        row.name = request.name().trim();
        row.description = request.description();
        row.status = request.status() == null ? exists.status : request.status();
        row.parentId = request.parentId();
        mapper.update(row);
        return toView(mapper.findById(id));
    }

    @Transactional
    public void delete(Long id) {
        RoleRow exists = mapper.findById(id);
        if (exists == null) throw new IllegalArgumentException("角色不存在");
        if (ADMIN_ROLE_CODE.equals(exists.code)) throw new IllegalArgumentException("内置管理员角色不可删除");
        if (mapper.countChildren(id) > 0) throw new IllegalArgumentException("请先删除该角色的下级角色");
        int employees = mapper.countEmployees(id);
        if (employees > 0) throw new IllegalArgumentException("该角色下还有 " + employees + " 名员工，请先调整员工角色");
        mapper.delete(id);
    }

    @Transactional
    public void setPermissions(Long id, List<String> codes) {
        RoleRow role = mapper.findById(id);
        if (role == null) throw new IllegalArgumentException("角色不存在");
        if (ADMIN_ROLE_CODE.equals(role.code)) {
            throw new IllegalArgumentException("管理员固定拥有全部功能，无需配置");
        }
        mapper.clearPermissions(id);
        if (codes == null) return;
        Set<String> distinct = codes.stream().filter((c) -> c != null && !c.isBlank()).collect(Collectors.toSet());
        for (String code : distinct) {
            Long permissionId = mapper.permissionIdByCode(code);
            if (permissionId != null) mapper.insertPermission(id, permissionId);
        }
    }

    public Map<String, Object> brief(Long id) {
        RoleRow role = mapper.findById(id);
        if (role == null) throw new IllegalArgumentException("角色不存在");
        return Map.of("id", role.id, "name", role.name, "employeeCount", role.employeeCount);
    }

    private void validate(RoleRequest request, Long excludeId) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        if (request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("角色编号不能为空");
        }
        String code = request.code().trim().toUpperCase();
        if (mapper.countByCode(code, excludeId) > 0) {
            throw new IllegalArgumentException("角色编号 " + code + " 已存在");
        }
        Long parentId = request.parentId();
        if (parentId != null) {
            if (parentId.equals(excludeId)) throw new IllegalArgumentException("上级角色不能是自己");
            if (excludeId != null && mapper.isDescendant(excludeId, parentId) > 0) {
                throw new IllegalArgumentException("上级角色不能是自己的下级角色");
            }
        }
    }

    private RoleView toView(RoleRow row) {
        List<String> codes = row.permissionCodes == null || row.permissionCodes.isBlank()
                ? List.of()
                : Arrays.stream(row.permissionCodes.split(",")).map(String::trim).toList();
        return new RoleView(
                row.id,
                row.code,
                row.name,
                row.description,
                row.status,
                row.parentId,
                ADMIN_ROLE_CODE.equals(row.code),
                row.employeeCount == null ? 0 : row.employeeCount,
                codes
        );
    }
}
