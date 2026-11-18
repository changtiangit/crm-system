package com.crm.role;

import java.util.List;

public record RoleView(
        Long id,
        String code,
        String name,
        String description,
        Integer status,
        Long parentId,
        boolean builtin,
        int employeeCount,
        List<String> permissionCodes
) {
}
