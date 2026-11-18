package com.crm.sms;

/** 短信发送通道，实现类由 sms.provider 决定 */
public interface SmsSender {
    void send(String phone, String code, int ttlMinutes);
}
