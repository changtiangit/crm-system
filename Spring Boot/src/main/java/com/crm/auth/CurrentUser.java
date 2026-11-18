package com.crm.auth;

import com.crm.common.DataScope;

import java.util.List;

/**
 * 返回给前端的当前登录用户。
 * permissions 是功能权限（能进哪些页面），dataScope 是数据权限（页面里能看到谁的数据）。
 */
public record CurrentUser(
        Long id,
        String empNo,
        String username,
        String name,
        String phone,
        Long deptId,
        String deptName,
        Integer status,
        List<RoleBrief> roles,
        List<String> permissions,
        boolean isAdmin,
        DataScope dataScope
) {
}
