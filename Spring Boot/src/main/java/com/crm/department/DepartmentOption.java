package com.crm.department;

import java.util.ArrayList;
import java.util.List;

/**
 * 注册页等公开场景用的部门选项：只暴露 id / 名称 / 对应角色 / 子部门，不含员工数等内部字段。
 * 带上角色名是为了让注册页能直接提示「选这个部门会拿到什么权限」。
 */
public class DepartmentOption {
    private final Long id;
    private final String name;
    private final Long roleId;
    private final String roleName;
    private final List<DepartmentOption> children = new ArrayList<>();

    public DepartmentOption(Long id, String name, Long roleId, String roleName) {
        this.id = id;
        this.name = name;
        this.roleId = roleId;
        this.roleName = roleName;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getRoleId() { return roleId; }
    public String getRoleName() { return roleName; }
    public List<DepartmentOption> getChildren() { return children; }
}
