package com.crm.sales;

import com.crm.business.BusinessRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SalesMapper {

    /** 销售团队成员：员工表里挂了销售组的人，按职位（总监 → 组长 → 顾问）与组排序 */
    @Select("SELECT e.id, e.employee_no AS empNo, e.name, e.phone, e.position, " +
            "e.sales_level AS level, e.clue_capacity AS capacity, e.sales_group AS grp, " +
            "e.leader_id AS leaderId, l.name AS leaderName, d.name AS deptName, e.status " +
            "FROM employees e " +
            "LEFT JOIN employees l ON l.id = e.leader_id " +
            "LEFT JOIN departments d ON d.id = e.department_id " +
            "WHERE e.sales_group <> '' " +
            "ORDER BY CASE WHEN e.position = '销售总监' THEN 0 ELSE 1 END, e.sales_group, " +
            "FIELD(e.position, '销售总监', '销售组长', '销售顾问'), " +
            "FIELD(e.sales_level, 'S', 'A', 'B', 'C'), e.clue_capacity DESC, e.id")
    List<SalesMemberRow> members();

    @Select("SELECT id, employee_no AS empNo, name, phone, position, sales_level AS level, " +
            "clue_capacity AS capacity, sales_group AS grp, leader_id AS leaderId, status " +
            "FROM employees WHERE id = #{id}")
    SalesMemberRow memberById(@Param("id") Long id);

    /** 按负责人汇总线索：在手（待跟进 + 跟进中）、已转化、本月新增 */
    @Select("SELECT owner, COUNT(*) AS total, " +
            "SUM(CASE WHEN status IN ('待跟进', '跟进中') THEN 1 ELSE 0 END) AS inHand, " +
            "SUM(CASE WHEN status = '已转化' THEN 1 ELSE 0 END) AS converted, " +
            "SUM(CASE WHEN create_time >= DATE_FORMAT(CURDATE(), '%Y-%m-01') THEN 1 ELSE 0 END) AS monthNew " +
            "FROM crm_clues WHERE owner <> '' GROUP BY owner")
    List<ClueStatRow> clueStats();

    @Select("SELECT COUNT(*) FROM crm_clues WHERE status = '待分配' OR owner = ''")
    int pendingCount();

    /** 本月新进线索（不区分是否已分配），团队页顶部的「本月新增」用它 */
    @Select("SELECT COUNT(*) FROM crm_clues WHERE create_time >= DATE_FORMAT(CURDATE(), '%Y-%m-01')")
    int monthNewCount();

    /** 待分配池：高意向排前面，同意向先到先分 */
    @Select("SELECT id, name, contact, phone, source, intent, status, owner, " +
            "assign_time AS assignTime, assign_by AS assignBy, create_time AS createTime " +
            "FROM crm_clues WHERE status = '待分配' OR owner = '' " +
            "ORDER BY FIELD(intent, '高', '中', '低'), create_time, id")
    List<BusinessRow> pendingClues();

    @Select("SELECT id, name, contact, phone, source, intent, status, owner, " +
            "assign_time AS assignTime, assign_by AS assignBy, create_time AS createTime " +
            "FROM crm_clues WHERE id = #{id}")
    BusinessRow clueById(@Param("id") Long id);

    @Update("UPDATE crm_clues SET owner = #{owner}, status = #{status}, " +
            "assign_time = NOW(), assign_by = #{operator} WHERE id = #{id}")
    int assignClue(@Param("id") Long id,
                   @Param("owner") String owner,
                   @Param("status") String status,
                   @Param("operator") String operator);

    @Insert("INSERT INTO crm_clue_assign_log(clue_id, clue_name, from_owner, to_owner, operator, mode) " +
            "VALUES(#{clueId}, #{clueName}, #{fromOwner}, #{toOwner}, #{operator}, #{mode})")
    int insertLog(@Param("clueId") Long clueId,
                  @Param("clueName") String clueName,
                  @Param("fromOwner") String fromOwner,
                  @Param("toOwner") String toOwner,
                  @Param("operator") String operator,
                  @Param("mode") String mode);

    @Select("SELECT id, clue_id AS clueId, clue_name AS clueName, from_owner AS fromOwner, " +
            "to_owner AS toOwner, operator, mode, created_at AS createdAt " +
            "FROM crm_clue_assign_log ORDER BY created_at DESC, id DESC LIMIT 12")
    List<AssignLogRow> recentLogs();

    @Select("SELECT name FROM employees WHERE id = #{id}")
    String employeeName(@Param("id") Long id);

    @Select("SELECT id FROM departments WHERE name = #{name} LIMIT 1")
    Long deptIdByName(@Param("name") String name);

    @Select("SELECT COUNT(*) FROM employees WHERE employee_no = #{empNo}")
    int countEmpNo(@Param("empNo") String empNo);

    @Select("SELECT COUNT(*) FROM employees WHERE phone = #{phone}")
    int countPhone(@Param("phone") String phone);

    @Insert("INSERT INTO employees(employee_no, username, password_hash, name, position, sales_level, " +
            "clue_capacity, leader_id, sales_group, department_id, phone, status) " +
            "VALUES(#{empNo}, #{empNo}, #{passwordHash}, #{name}, #{position}, #{level}, " +
            "#{capacity}, #{leaderId}, #{grp}, #{departmentId}, #{phone}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMember(SalesMemberRow row);

    @Update("UPDATE employees SET position = #{position}, sales_level = #{level}, " +
            "clue_capacity = #{capacity}, sales_group = #{grp}, leader_id = #{leaderId}, status = #{status} " +
            "WHERE id = #{id}")
    int updateMember(SalesMemberRow row);

    @Select("SELECT id FROM roles WHERE code = #{code}")
    Long roleIdByCode(@Param("code") String code);

    @Insert("INSERT IGNORE INTO employee_roles(employee_id, role_id) VALUES(#{employeeId}, #{roleId})")
    int insertMemberRole(@Param("employeeId") Long employeeId, @Param("roleId") Long roleId);

    @Delete("DELETE er FROM employee_roles er JOIN roles r ON r.id = er.role_id " +
            "WHERE er.employee_id = #{employeeId} AND r.code IN ('SALES_G1', 'SALES_G2')")
    int clearSalesRoles(@Param("employeeId") Long employeeId);
}
