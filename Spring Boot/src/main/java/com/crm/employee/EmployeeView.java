package com.crm.employee;

import java.util.List;

/** 返回给前端的员工信息 */
public record EmployeeView(
        Long id,
        String empNo,
        String name,
        String position,
        Long deptId,
        String deptName,
        List<Long> roleIds,
        List<String> roleNames,
        String phone,
        String email,
        Integer status,
        String createdAt,
        /** 数据范围：ALL 全公司 / GROUP 本组 / SELF 仅本人 */
        String dataScope
) {
}
