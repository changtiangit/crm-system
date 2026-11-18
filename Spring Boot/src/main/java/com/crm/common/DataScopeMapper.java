package com.crm.common;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 数据范围查询：员工岗位 / 手工配置 / 直属组员 */
@Mapper
public interface DataScopeMapper {

    @Select("SELECT e.id, e.name, e.position, e.data_scope AS dataScope, d.name AS deptName " +
            "FROM employees e LEFT JOIN departments d ON d.id = e.department_id WHERE e.id = #{id}")
    ScopeEmployeeRow employee(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM employee_roles er JOIN roles r ON r.id = er.role_id " +
            "WHERE er.employee_id = #{id} AND r.code = 'ADMIN'")
    int isAdmin(@Param("id") Long id);

    /** 本组：本人 + 直属组员（离职/停用的不再计入） */
    @Select("SELECT name FROM employees WHERE status = 1 AND (id = #{id} OR leader_id = #{id}) ORDER BY id")
    List<String> groupMemberNames(@Param("id") Long id);

    /** 项目组成员（暂未启用，保留给后续「协作人」扩展） */
    @Select("SELECT name FROM employees WHERE status = 1 AND department_id = #{deptId} ORDER BY id")
    List<String> departmentMemberNames(@Param("deptId") Long deptId);
}
