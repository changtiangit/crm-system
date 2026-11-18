package com.crm.sms;

import com.crm.auth.AccountRow;
import com.crm.auth.AuthMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/** 短信验证码的生成、发送与校验 */
@Service
public class SmsService {
    public static final String SCENE_LOGIN = "login";
    public static final String SCENE_REGISTER = "register";

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SmsCodeMapper mapper;
    private final AuthMapper authMapper;
    private final SmsProperties properties;
    private final MockSmsSender mockSender;
    private final CloopenSmsSender cloopenSender;

    public SmsService(SmsCodeMapper mapper,
                      AuthMapper authMapper,
                      SmsProperties properties,
                      MockSmsSender mockSender,
                      CloopenSmsSender cloopenSender) {
        this.mapper = mapper;
        this.authMapper = authMapper;
        this.properties = properties;
        this.mockSender = mockSender;
        this.cloopenSender = cloopenSender;
    }

    @Transactional
    public SmsSendResult send(String rawPhone, String scene) {
        String sceneKey = scene == null || scene.isBlank() ? SCENE_LOGIN : scene.trim();
        String phone = normalizePhone(rawPhone);
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("请输入正确的 11 位手机号");
        }

        if (SCENE_REGISTER.equals(sceneKey)) {
            if (authMapper.findByPhone(phone) != null) {
                throw new IllegalArgumentException("该手机号已注册");
            }
        } else {
            // 登录验证码只发给在职员工；注册验证码只发给未注册手机号
            AccountRow account = authMapper.findByPhone(phone);
            if (account == null) {
                throw new IllegalArgumentException("该手机号未绑定员工账号，请联系管理员");
            }
            if (account.status == null || account.status == 0) {
                throw new IllegalArgumentException("该账号已停用，请联系管理员");
            }
        }

        ensureFrequency(phone, sceneKey);

        int ttl = properties.getCodeTtlSeconds();
        int codeLength = effectiveCodeLength();
        String code = randomCode();
        LocalDateTime expireAt = LocalDateTime.now().plusSeconds(ttl);
        mapper.insert(phone, sceneKey, sha256(code), expireAt);

        boolean mock = isMock();
        if (mock) {
            mockSender.send(phone, code, Math.max(1, ttl / 60));
        } else {
            cloopenSender.send(phone, code, Math.max(1, ttl / 60));
        }

        String debugCode = mock && properties.isExposeCode() ? code : null;
        return new SmsSendResult(maskPhone(phone), ttl, properties.getSendIntervalSeconds(), mock, debugCode, codeLength);
    }

    /** 校验验证码，失败抛 IllegalArgumentException；成功后该手机号此前未用的验证码全部作废 */
    @Transactional
    public void verify(String rawPhone, String scene, String code) {
        String phone = normalizePhone(rawPhone);
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("请输入正确的 11 位手机号");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("请输入短信验证码");
        }
        String sceneKey = scene == null || scene.isBlank() ? SCENE_LOGIN : scene.trim();

        SmsCodeRow row = mapper.findLatestValid(phone, sceneKey);
        if (row == null) {
            throw new IllegalArgumentException("验证码已失效，请重新获取");
        }
        if (row.attempts != null && row.attempts >= properties.getMaxAttempts()) {
            mapper.markUsed(row.id);
            throw new IllegalArgumentException("验证码错误次数过多，请重新获取");
        }
        if (!row.codeHash.equals(sha256(code.trim()))) {
            mapper.increaseAttempts(row.id);
            int left = properties.getMaxAttempts() - (row.attempts == null ? 0 : row.attempts) - 1;
            throw new IllegalArgumentException(left > 0 ? "验证码不正确，还可尝试 " + left + " 次" : "验证码错误次数过多，请重新获取");
        }
        mapper.invalidateAll(phone, sceneKey);
    }

    private void ensureFrequency(String phone, String scene) {
        SmsCodeRow latest = mapper.findLatest(phone, scene);
        if (latest != null && latest.createdAt != null) {
            long elapsed = Duration.between(latest.createdAt, LocalDateTime.now()).getSeconds();
            long wait = properties.getSendIntervalSeconds() - elapsed;
            if (wait > 0) {
                throw new IllegalArgumentException("发送过于频繁，请 " + wait + " 秒后再试");
            }
        }
        int sent = mapper.countSince(phone, scene, LocalDateTime.now().minusHours(24));
        if (sent >= properties.getDailyLimit()) {
            throw new IllegalArgumentException("该手机号 24 小时内获取验证码次数过多，请明天再试");
        }
    }

    private boolean isMock() {
        return !"cloopen".equalsIgnoreCase(properties.getProvider());
    }

    private String randomCode() {
        int len = effectiveCodeLength();
        int bound = (int) Math.pow(10, len);
        return String.format("%0" + len + "d", RANDOM.nextInt(bound));
    }

    /**
     * 实际生效的验证码位数。
     * 云通讯测试模板（templateId=1）的变量只接受 1-4 位数字，传 6 位会被 161125 拒绝，
     * 所以这里自动降到 4 位，避免换个模板就发不出去。
     */
    private int effectiveCodeLength() {
        int len = properties.getCodeLength();
        if (!isMock() && "1".equals(properties.getCloopen().getTemplateId()) && len > 4) {
            log.info("检测到使用云通讯测试模板(templateId=1)，验证码自动调整为 4 位（原配置 {} 位）", len);
            len = 4;
        }
        return Math.max(4, Math.min(8, len));
    }

    /** 去掉空格、横线，并兼容 +86 / 86 前缀 */
    public static String normalizePhone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 13 && digits.startsWith("86")) return digits.substring(2);
        if (digits.length() == 11) return digits;
        return digits;
    }

    public static boolean isPhone(String raw) {
        return PHONE_PATTERN.matcher(normalizePhone(raw)).matches();
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }

    /** 启动时打印当前通道，避免误以为真发了短信 */
    @jakarta.annotation.PostConstruct
    public void logChannel() {
        if (isMock()) {
            log.info("短信通道：本地模拟（sms.provider={}），验证码只打印在控制台；切换真实通道用 SMS_PROVIDER=cloopen",
                    properties.getProvider());
            return;
        }
        SmsProperties.Cloopen cfg = properties.getCloopen();
        boolean ready = notBlank(cfg.getAccountSid()) && notBlank(cfg.getAuthToken())
                && notBlank(cfg.getAppId()) && notBlank(cfg.getTemplateId());
        boolean testTemplate = "1".equals(cfg.getTemplateId());
        log.info("短信通道：容联云 Cloopen，AppID={}，模板ID={}，验证码 {} 位，配置{}",
                notBlank(cfg.getAppId()) ? cfg.getAppId() : "未配置",
                notBlank(cfg.getTemplateId()) ? cfg.getTemplateId() : "未配置",
                effectiveCodeLength(),
                ready ? "完整" : "不完整（缺少 template-id 等参数，发送会失败）");
        if (testTemplate) {
            log.info("当前使用云通讯测试模板(templateId=1)：仅能发送给控制台「号码管理 → 测试号码」中已登记的号码，且验证码为 4 位");
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
