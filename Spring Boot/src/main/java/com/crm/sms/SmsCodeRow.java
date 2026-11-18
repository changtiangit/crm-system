package com.crm.sms;

import java.time.LocalDateTime;

/** 一条短信验证码记录 */
public class SmsCodeRow {
    public Long id;
    public String phone;
    public String scene;
    public String codeHash;
    public Integer attempts;
    public Integer used;
    public LocalDateTime expireAt;
    public LocalDateTime createdAt;
}
