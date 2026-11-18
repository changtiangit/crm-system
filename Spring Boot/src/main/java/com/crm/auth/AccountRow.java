package com.crm.auth;

/** 登录查询用的员工行（含密码哈希，只在服务端内部使用） */
public class AccountRow {
    public Long id;
    public String employeeNo;
    public String username;
    public String passwordHash;
    public String name;
    public Long departmentId;
    public String phone;
    public String email;
    public Integer status;
}
