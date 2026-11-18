package com.crm.employee;

import com.crm.auth.RoleBrief;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface EmployeeMapper {
    @Select("SELECT e.id, e.employee_no, e.username, e.name, e.department_id, d.name AS dept_name, " +
            "e.position, e.data_scope, e.phone, e.email, e.status, e.created_at, " +
            "(SELECT GROUP_CONCAT(er.role_id) FROM employee_roles er WHERE er.employee_id = e.id) AS role_ids " +
            "FROM employees e LEFT JOIN departments d ON d.id = e.department_id " +
            "ORDER BY e.id")
    List<EmployeeRow> findAll();

    @Select("SELECT e.id, e.employee_no, e.username, e.name, e.department_id, d.name AS dept_name, " +
            "e.position, e.data_scope, e.phone, e.email, e.status, e.created_at, " +
            "(SELECT GROUP_CONCAT(er.role_id) FROM employee_roles er WHERE er.employee_id = e.id) AS role_ids " +
            "FROM employees e LEFT JOIN departments d ON d.id = e.department_id WHERE e.id = #{id}")
    EmployeeRow findById(@Param("id") Long id);

    @Select("SELECT name FROM departments WHERE id = #{id}")
    String deptName(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM employees WHERE employee_no = #{employeeNo} AND id <> IFNULL(#{excludeId}, 0)")
    int countByEmployeeNo(@Param("employeeNo") String employeeNo, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(*) FROM employees WHERE phone = #{phone} AND id <> IFNULL(#{excludeId}, 0)")
    int countByPhone(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    @Select("SELECT id, code, name FROM roles ORDER BY id")
    List<RoleBrief> allRoles();

    @Insert("INSERT INTO employees(employee_no, username, password_hash, name, position, " +
            "sales_level, clue_capacity, sales_group, data_scope, department_id, phone, email, status) " +
            "VALUES(#{employeeNo}, #{username}, #{passwordHash}, #{name}, #{position}, " +
            "#{salesLevel}, #{clueCapacity}, #{salesGroup}, #{dataScope}, #{departmentId}, #{phone}, #{email}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EmployeeRow row);

    @Update("UPDATE employees SET employee_no = #{employeeNo}, username = #{username}, name = #{name}, position = #{position}, " +
            "sales_level = IFNULL(#{salesLevel}, sales_level), " +
            "clue_capacity = IFNULL(#{clueCapacity}, clue_capacity), " +
            "sales_group = IFNULL(#{salesGroup}, sales_group), " +
            "data_scope = IFNULL(#{dataScope}, data_scope), " +
            "department_id = #{departmentId}, phone = #{phone}, email = #{email}, status = #{status} " +
            "WHERE id = #{id}")
    int update(EmployeeRow row);

    @Update("UPDATE employees SET password_hash = #{hash} WHERE id = #{id}")
    int resetPassword(@Param("id") Long id, @Param("hash") String hash);

    @Delete("DELETE FROM employees WHERE id = #{id}")
    int delete(@Param("id") Long id);

    @Delete("DELETE FROM employee_roles WHERE employee_id = #{employeeId}")
    int clearRoles(@Param("employeeId") Long employeeId);

    @Insert("INSERT INTO employee_roles(employee_id, role_id) VALUES(#{employeeId}, #{roleId})")
    int insertRole(@Param("employeeId") Long employeeId, @Param("roleId") Long roleId);

    @Select("SELECT COUNT(*) FROM employees e JOIN employee_roles er ON er.employee_id = e.id " +
            "JOIN roles r ON r.id = er.role_id WHERE r.code = 'ADMIN'")
    int countAdmins();
}
