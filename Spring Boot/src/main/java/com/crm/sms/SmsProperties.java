package com.crm.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 短信通道配置：mock=本地模拟（开发自测），cloopen=容联云通讯 TemplateSMS */
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {
    /** mock | cloopen */
    private String provider = "mock";
    /** 开发期把验证码回传给前端，生产必须为 false */
    private boolean exposeCode = true;
    /** 验证码有效期（秒） */
    private int codeTtlSeconds = 300;
    /** 验证码位数：自定义模板一般 6 位；云通讯测试模板(templateId=1)只支持 1-4 位 */
    private int codeLength = 6;
    /** 同一手机号两次发送的最小间隔（秒） */
    private int sendIntervalSeconds = 60;
    /** 同一手机号 24 小时内的发送上限 */
    private int dailyLimit = 10;
    /** 单个验证码允许的错误次数 */
    private int maxAttempts = 5;

    private final Cloopen cloopen = new Cloopen();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isExposeCode() {
        return exposeCode;
    }

    public void setExposeCode(boolean exposeCode) {
        this.exposeCode = exposeCode;
    }

    public int getCodeTtlSeconds() {
        return codeTtlSeconds;
    }

    public void setCodeTtlSeconds(int codeTtlSeconds) {
        this.codeTtlSeconds = codeTtlSeconds;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public int getSendIntervalSeconds() {
        return sendIntervalSeconds;
    }

    public void setSendIntervalSeconds(int sendIntervalSeconds) {
        this.sendIntervalSeconds = sendIntervalSeconds;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Cloopen getCloopen() {
        return cloopen;
    }

    public static class Cloopen {
        /** 接口地址，默认 app.cloopen.com；专有云按商务提供的地址填写 */
        private String restUrl = "https://app.cloopen.com";
        private int port = 8883;
        /** 主账号 ACCOUNT SID */
        private String accountSid = "";
        /** 主账号 AUTH TOKEN */
        private String authToken = "";
        /** 短信应用 AppID */
        private String appId = "";
        /** 短信模板 ID */
        private String templateId = "";

        public String getRestUrl() {
            return restUrl;
        }

        public void setRestUrl(String restUrl) {
            this.restUrl = restUrl;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getAccountSid() {
            return accountSid;
        }

        public void setAccountSid(String accountSid) {
            this.accountSid = accountSid;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }
    }
}
