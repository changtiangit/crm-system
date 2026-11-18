package com.crm.employee;

/** 员工查询 / 写入行（roleIds 是查询时 GROUP_CONCAT 出来的字符串） */
public class EmployeeRow {
    public Long id;
    public String employeeNo;
    public String username;
    public String passwordHash;
    public String name;
    public String position;
    public String salesLevel;
    public Integer clueCapacity;
    public String salesGroup;
    public String dataScope;
    public Long departmentId;
    public String deptName;
    public String phone;
    public String email;
    public Integer status;
    public String roleIds;
    public String createdAt;
}
