package com.crm.department;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DepartmentMapper {
    @Select("SELECT * FROM departments ORDER BY sort_order, id")
    List<Department> findAll();

    @Select("SELECT d.*, r.name AS role_name, " +
            "(SELECT COUNT(*) FROM employees e WHERE e.department_id=d.id) AS employee_count " +
            "FROM departments d LEFT JOIN roles r ON r.id = d.role_id ORDER BY d.sort_order, d.id")
    List<Department> findAllWithEmployeeCount();

    @Select("SELECT d.*, r.name AS role_name FROM departments d LEFT JOIN roles r ON r.id = d.role_id WHERE d.id = #{id}")
    Department findById(Long id);

    @Select("SELECT d.*, r.name AS role_name FROM departments d LEFT JOIN roles r ON r.id = d.role_id " +
            "ORDER BY d.sort_order, d.id")
    List<Department> findAllWithRole();

    @Insert("INSERT INTO departments(parent_id, code, name, sort_order, status, role_id) " +
            "VALUES(#{parentId}, #{code}, #{name}, #{sortOrder}, #{status}, #{roleId})")
    int insert(Department department);

    @Update("UPDATE departments SET parent_id=#{parentId}, code=#{code}, name=#{name}, " +
            "sort_order=#{sortOrder}, status=#{status}, role_id=#{roleId} WHERE id=#{id}")
    int update(Department department);

    @Update("UPDATE departments SET status=#{status} WHERE id=#{id}")
    int updateStatus(Long id, Integer status);

    @Delete("DELETE FROM departments WHERE id=#{id}")
    int delete(Long id);

    @Select("SELECT COUNT(*) FROM departments WHERE parent_id=#{id}")
    int countChildren(Long id);

    @Select("WITH RECURSIVE descendants AS (" +
            "SELECT id FROM departments WHERE parent_id=#{id} " +
            "UNION ALL SELECT d.id FROM departments d JOIN descendants x ON d.parent_id=x.id) " +
            "SELECT COUNT(*) FROM descendants WHERE id=#{candidateId}")
    int isDescendant(Long id, Long candidateId);

    @Select("SELECT COUNT(*) FROM employees WHERE department_id=#{id}")
    int countEmployees(Long id);
}
