package com.crm.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 本地模拟通道：不真正发短信，把验证码打到控制台，便于没有短信账号时联调 */
@Component
public class MockSmsSender implements SmsSender {
    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    @Override
    public void send(String phone, String code, int ttlMinutes) {
        log.warn("[短信模拟] 手机号 {} 的登录验证码为 {}，{} 分钟内有效（未真实发送）", phone, code, ttlMinutes);
    }
}
