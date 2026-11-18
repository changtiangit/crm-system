package com.crm.auth;

import com.crm.common.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    public record LoginRequest(String empNo, String account, String phone, String password, String code, String mode) {
    }

    public record ChangePasswordRequest(String oldPassword, String newPassword) {
    }

    public record RegisterRequest(String phone, String code, String password, String name, Long deptId) {
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest request) {
        return service.register(request.phone(), request.code(), request.password(), request.name(), request.deptId());
    }

    /**
     * 登录：mode=sms 走手机号 + 验证码；否则走手机号/工号 + 密码。
     * 兼容老调用方：只传 empNo + password、或只传 phone + code 也能正确分流。
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        boolean smsMode = "sms".equalsIgnoreCase(request.mode())
                || (isBlank(request.password()) && !isBlank(request.code()));
        if (smsMode) {
            return service.loginBySms(firstNonBlank(request.phone(), request.account(), request.empNo()), request.code());
        }
        return service.login(firstNonBlank(request.account(), request.empNo(), request.phone()), request.password());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) return value;
        }
        return null;
    }

    @GetMapping("/me")
    public CurrentUser me(@RequestAttribute(AuthInterceptor.USER_ID) Long employeeId) {
        return service.currentUser(employeeId);
    }

    @PostMapping("/password")
    public Map<String, Object> changePassword(
            @RequestAttribute(AuthInterceptor.USER_ID) Long employeeId,
            @RequestBody ChangePasswordRequest request) {
        return service.changePassword(employeeId, request.oldPassword(), request.newPassword());
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request) {
        Object token = request.getAttribute(AuthInterceptor.TOKEN);
        if (token != null) service.logout(token.toString());
        return Map.of("message", "已退出登录");
    }

    /** 演示用：登录页列出全部账号及其可见菜单（不含密码） */
    @GetMapping("/demo-accounts")
    public List<Map<String, Object>> demoAccounts() {
        return service.demoAccounts();
    }

    /** 探活，便于排查前后端连通性 */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
