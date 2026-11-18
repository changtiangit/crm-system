package com.crm.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录令牌存储（演示用内存实现）。
 * 生产环境建议替换为 Redis 或 JWT，此处接口保持不变即可。
 */
@Component
public class TokenStore {
    private static final long TTL_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private record Session(Long employeeId, long expiresAt) {
        boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public String issue(Long employeeId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(employeeId, System.currentTimeMillis() + TTL_MILLIS));
        return token;
    }

    /** 返回令牌对应的员工 id，无效或过期返回 null */
    public Long resolve(String token) {
        if (token == null || token.isBlank()) return null;
        Session session = sessions.get(token);
        if (session == null) return null;
        if (session.expired()) {
            sessions.remove(token);
            return null;
        }
        return session.employeeId();
    }

    public void revoke(String token) {
        if (token != null) sessions.remove(token);
    }

    /** 员工被删除或改密后，让其所有令牌失效 */
    public void revokeAll(Long employeeId) {
        sessions.entrySet().removeIf((entry) -> entry.getValue().employeeId().equals(employeeId));
    }
}
