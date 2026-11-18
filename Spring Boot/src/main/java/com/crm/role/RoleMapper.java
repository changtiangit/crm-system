package com.crm.role;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RoleMapper {
    @Select("SELECT r.id, r.code, r.name, r.description, r.status, r.parent_id, " +
            "(SELECT COUNT(*) FROM employee_roles er WHERE er.role_id = r.id) AS employee_count, " +
            "(SELECT GROUP_CONCAT(p.code) FROM role_permissions rp " +
            " JOIN permissions p ON p.id = rp.permission_id WHERE rp.role_id = r.id) AS permission_codes " +
            "FROM roles r ORDER BY r.id")
    List<RoleRow> findAll();

    @Select("SELECT r.id, r.code, r.name, r.description, r.status, r.parent_id, " +
            "(SELECT COUNT(*) FROM employee_roles er WHERE er.role_id = r.id) AS employee_count, " +
            "(SELECT GROUP_CONCAT(p.code) FROM role_permissions rp " +
            " JOIN permissions p ON p.id = rp.permission_id WHERE rp.role_id = r.id) AS permission_codes " +
            "FROM roles r WHERE r.id = #{id}")
    RoleRow findById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM roles WHERE code = #{code} AND id <> IFNULL(#{excludeId}, 0)")
    int countByCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    @Insert("INSERT INTO roles(code, name, description, status, parent_id) " +
            "VALUES(#{code}, #{name}, #{description}, #{status}, #{parentId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RoleRow row);

    @Update("UPDATE roles SET code = #{code}, name = #{name}, description = #{description}, " +
            "status = #{status}, parent_id = #{parentId} WHERE id = #{id}")
    int update(RoleRow row);

    @Delete("DELETE FROM roles WHERE id = #{id}")
    int delete(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM roles WHERE parent_id = #{id}")
    int countChildren(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM employee_roles WHERE role_id = #{id}")
    int countEmployees(@Param("id") Long id);

    /** 判断 candidateId 是否是 id 的后代角色，用于阻止层级成环 */
    @Select("WITH RECURSIVE sub AS (" +
            "SELECT id FROM roles WHERE parent_id = #{id} " +
            "UNION ALL SELECT r.id FROM roles r JOIN sub s ON r.parent_id = s.id) " +
            "SELECT COUNT(*) FROM sub WHERE id = #{candidateId}")
    int isDescendant(@Param("id") Long id, @Param("candidateId") Long candidateId);

    @Select("SELECT p.code FROM permissions p JOIN role_permissions rp ON rp.permission_id = p.id " +
            "WHERE rp.role_id = #{roleId} ORDER BY p.sort_order, p.id")
    List<String> permissionCodesOf(@Param("roleId") Long roleId);

    @Select("SELECT id FROM permissions WHERE code = #{code}")
    Long permissionIdByCode(@Param("code") String code);

    @Delete("DELETE FROM role_permissions WHERE role_id = #{roleId}")
    int clearPermissions(@Param("roleId") Long roleId);

    @Insert("INSERT INTO role_permissions(role_id, permission_id) VALUES(#{roleId}, #{permissionId})")
    int insertPermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
