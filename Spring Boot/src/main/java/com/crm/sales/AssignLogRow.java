package com.crm.sales;

/** 线索分配记录：谁在什么时候把哪条线索从谁手里转给了谁 */
public class AssignLogRow {
    public Long id;
    public Long clueId;
    public String clueName;
    public String fromOwner;
    public String toOwner;
    public String operator;
    public String mode;
    public String createdAt;
}
