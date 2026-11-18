package com.crm.auth;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AuthMapper {
    @Select("SELECT id, employee_no, username, password_hash, name, department_id, phone, email, status " +
            "FROM employees WHERE employee_no = #{account} OR username = #{account} OR phone = #{account} LIMIT 1")
    AccountRow findByAccount(@Param("account") String account);

    @Select("SELECT id, employee_no, username, password_hash, name, department_id, phone, email, status " +
            "FROM employees WHERE phone = #{phone} LIMIT 1")
    AccountRow findByPhone(@Param("phone") String phone);

    @Insert("INSERT INTO employees(employee_no, username, password_hash, name, department_id, phone, status) " +
            "VALUES(#{employeeNo}, #{username}, #{passwordHash}, #{name}, #{departmentId}, #{phone}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRegisteredAccount(AccountRow account);

    /** 自注册员工拿到自增 ID 后，把工号统一成 U0001 这种可读格式 */
    @Update("UPDATE employees SET employee_no = #{employeeNo} WHERE id = #{id}")
    int updateEmployeeNo(@Param("id") Long id, @Param("employeeNo") String employeeNo);

    @Select("SELECT name FROM roles WHERE id = #{id}")
    String roleNameOf(@Param("id") Long id);

    @Insert("INSERT INTO employee_roles(employee_id, role_id) VALUES(#{employeeId}, #{roleId})")
    int insertEmployeeRole(@Param("employeeId") Long employeeId, @Param("roleId") Long roleId);

    @Select("SELECT id, employee_no, username, password_hash, name, department_id, phone, email, status " +
            "FROM employees WHERE id = #{id}")
    AccountRow findById(@Param("id") Long id);

    @Select("SELECT r.id, r.code, r.name FROM roles r " +
            "JOIN employee_roles er ON er.role_id = r.id " +
            "WHERE er.employee_id = #{employeeId} AND r.status = 1 ORDER BY r.id")
    List<RoleBrief> rolesOf(@Param("employeeId") Long employeeId);

    @Select("SELECT DISTINCT p.code FROM permissions p " +
            "JOIN role_permissions rp ON rp.permission_id = p.id " +
            "JOIN employee_roles er ON er.role_id = rp.role_id " +
            "JOIN roles r ON r.id = er.role_id " +
            "WHERE er.employee_id = #{employeeId} AND r.status = 1")
    List<String> permissionCodesOf(@Param("employeeId") Long employeeId);

    @Select("SELECT code FROM permissions ORDER BY sort_order, id")
    List<String> allPermissionCodes();

    /** 演示账号一览用：所有启用状态的账号 */
    @Select("SELECT id, employee_no, username, name, department_id, phone, email, status " +
            "FROM employees WHERE status = 1 ORDER BY id")
    List<AccountRow> listActiveAccounts();

    @Select("SELECT name FROM departments WHERE id = #{id}")
    String departmentName(@Param("id") Long id);

    @Update("UPDATE employees SET password_hash = #{hash} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("hash") String hash);
}
