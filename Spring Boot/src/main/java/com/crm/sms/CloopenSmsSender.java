package com.crm.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 容联云通讯（Cloopen）短信通道。
 *
 * <p>接口：POST {restUrl}:{port}/2013-12-26/Accounts/{accountSid}/SMS/TemplateSMS?sig={sig}
 * <ul>
 *   <li>sig = MD5(accountSid + authToken + timestamp).toUpperCase()，timestamp 形如 20261111153000</li>
 *   <li>Authorization = Base64(accountSid + ":" + timestamp)</li>
 *   <li>请求体 { to, appId, templateId, datas: [验证码, 有效分钟数] }</li>
 * </ul>
 * 返回 statusCode=000000 表示成功，其余为失败（statusMsg 是原因）。
 */
@Component
public class CloopenSmsSender implements SmsSender {
    private static final Logger log = LoggerFactory.getLogger(CloopenSmsSender.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SmsProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public CloopenSmsSender(SmsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public void send(String phone, String code, int ttlMinutes) {
        SmsProperties.Cloopen cfg = properties.getCloopen();
        require(cfg.getAccountSid(), "sms.cloopen.account-sid");
        require(cfg.getAuthToken(), "sms.cloopen.auth-token");
        require(cfg.getAppId(), "sms.cloopen.app-id");
        require(cfg.getTemplateId(), "sms.cloopen.template-id");

        // 时间戳只取一次：sig 与 Authorization 必须使用同一时刻，否则跨秒会导致签名校验失败
        String timestamp = LocalDateTime.now().format(TS);
        String base = cfg.getRestUrl().endsWith("/")
                ? cfg.getRestUrl().substring(0, cfg.getRestUrl().length() - 1)
                : cfg.getRestUrl();
        String url = base + ":" + cfg.getPort() + "/2013-12-26/Accounts/" + cfg.getAccountSid()
                + "/SMS/TemplateSMS?sig=" + signature(cfg, timestamp);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("to", phone);
        body.put("appId", cfg.getAppId());
        body.put("templateId", cfg.getTemplateId());
        body.put("datas", List.of(code, String.valueOf(ttlMinutes)));

        String raw;
        try {
            raw = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", authorization(cfg, timestamp))
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("调用容联云短信接口失败: {}", e.getMessage());
            throw new IllegalStateException("短信发送失败，请稍后重试");
        }

        // 容联云在未声明 Accept 时会返回 XML，这里两种格式都兼容，避免把业务错误码吞成"稍后重试"
        String statusCode;
        String statusMsg;
        if (raw != null && raw.trim().startsWith("<")) {
            statusCode = firstMatch(raw, "<statusCode>(.*?)</statusCode>");
            statusMsg = firstMatch(raw, "<statusMsg>(.*?)</statusMsg>");
            if (statusCode.isEmpty()) {
                log.error("解析容联云响应失败: {}", raw);
                throw new IllegalStateException("短信发送失败，请稍后重试");
            }
        } else {
            JsonNode node;
            try {
                node = objectMapper.readTree(raw == null ? "{}" : raw);
            } catch (Exception e) {
                log.error("解析容联云响应失败: {}", raw);
                throw new IllegalStateException("短信发送失败，请稍后重试");
            }
            statusCode = node.path("statusCode").asText("");
            statusMsg = node.path("statusMsg").asText("");
            if ("000000".equals(statusCode)) {
                log.info("容联云短信已提交 phone={} sid={}", phone,
                        node.path("templateSMS").path("smsMessageSid").asText("-"));
                return;
            }
        }

        if (!"000000".equals(statusCode)) {
            String msg = statusMsg == null || statusMsg.isBlank() ? "未知错误" : statusMsg;
            log.warn("容联云返回失败 statusCode={} statusMsg={} 响应={}", statusCode, msg, raw);
            String hint = hint(statusCode);
            throw new IllegalStateException("短信发送失败：" + msg + "（" + statusCode + "）" + (hint.isEmpty() ? "" : "。" + hint));
        }
        log.info("容联云短信已提交 phone={}", phone);
    }

    private static String firstMatch(String text, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
        return m.find() ? m.group(1).trim() : "";
    }

    /** 把容联云错误码翻译成可执行的处置建议，避免只看到一个数字 */
    private static String hint(String code) {
        return switch (code) {
            case "112310", "113302" -> "该应用尚未上线，只能发给控制台登记的测试号码；请把接收号码加到「号码管理 → 测试号码」并完成验证，或提交应用上线";
            case "160053" -> "IP 鉴权失败，请把服务器出口 IP 加入容联云「鉴权 IP」白名单";
            case "161125", "160069" -> "模板变量只允许 1-4 位数字，请把 sms.code-length 设为 4，或改用自定义验证码模板";
            case "160032" -> "模板 ID 无效或不属于该应用，请核对 sms.cloopen.template-id";
            case "160043" -> "应用与模板 ID 不匹配，请确认模板创建在这个 AppID 下";
            case "161002", "161003" -> "账户余额不足，请先充值或在「财务 → 短信」确认套餐余量";
            case "160038", "160022" -> "发送过于频繁，请稍后再试";
            case "160034", "160072" -> "该号码在通道黑名单中，无法下发";
            default -> "";
        };
    }

    private String signature(SmsProperties.Cloopen cfg, String timestamp) {
        return md5(cfg.getAccountSid() + cfg.getAuthToken() + timestamp).toUpperCase();
    }

    private String authorization(SmsProperties.Cloopen cfg, String timestamp) {
        String plain = cfg.getAccountSid() + ":" + timestamp;
        return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    private static String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }

    private static void require(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("未配置 " + key + "，请先在 application.yml 或环境变量中填写容联云参数");
        }
    }
}
