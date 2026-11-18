package com.crm.auth;

public class RoleBrief {
    public Long id;
    public String code;
    public String name;

    public RoleBrief() {
    }

    public RoleBrief(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }
}
