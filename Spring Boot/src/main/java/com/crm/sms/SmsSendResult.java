package com.crm.sms;

/** 发送验证码的结果，debugCode 只在 mock 通道且 sms.expose-code=true 时返回 */
public record SmsSendResult(
        String phone,
        int expireIn,
        int interval,
        boolean mock,
        String debugCode,
        /** 本次验证码位数：由通道/模板决定（云通讯测试模板为 4 位），前端据此校验 */
        int codeLength
) {
}
