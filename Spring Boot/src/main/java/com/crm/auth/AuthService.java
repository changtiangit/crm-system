package com.crm.auth;

import com.crm.common.DataScopeService;
import com.crm.common.PasswordHasher;
import com.crm.common.TokenStore;
import com.crm.department.DepartmentService;
import com.crm.sms.SmsService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {
    private static final String ADMIN_ROLE_CODE = "ADMIN";

    /** 菜单级权限 → 菜单名称，用于登录页演示账号一览 */
    private static final String[][] MENU_RULES = {
            {"dashboard:view", "工作台 · 经营概览"},
            {"todo:view", "工作台 · 我的待办"},
            {"stats:sales:view", "数据统计 · 销售统计"},
            {"stats:customer:view", "数据统计 · 客户采购分析"},
            {"stats:product:view", "数据统计 · 产品销售分析"},
            {"stats:market:view", "数据统计 · 市场分析"},
            {"clue:view", "客户管理 · 销售线索"},
            {"sales:team:view", "客户管理 · 销售团队"},
            {"pool:view", "客户管理 · 客户公海"},
            {"customer:view", "客户管理 · 客户档案"},
            {"contact:view", "客户管理 · 联系人"},
            {"followup:view", "客户管理 · 跟进记录"},
            {"opportunity:view", "销售管理 · 销售商机"},
            {"quote:view", "销售管理 · 报价单"},
            {"order:view", "销售管理 · 销售订单"},
            {"product:view", "销售管理 · 商品"},
            {"delivery:view", "交付与回款 · 发货记录"},
            {"receipt:view", "交付与回款 · 收款记录"},
            {"receivable:view", "交付与回款 · 应收账款"},
            {"ticket:view", "售后管理 · 售后工单"},
            {"return:view", "售后管理 · 退换货单"},
            {"revisit:view", "售后管理 · 客户回访"},
            {"system:employee:view", "设置 · 员工管理"},
            {"system:role:view", "设置 · 角色管理"},
            {"system:dept:view", "设置 · 部门管理"},
            {"system:permission:view", "设置 · 权限授权"}
    };

    private final AuthMapper mapper;
    private final TokenStore tokenStore;
    private final SmsService smsService;
    private final DepartmentService departmentService;
    private final DataScopeService dataScopeService;

    public AuthService(AuthMapper mapper, TokenStore tokenStore, SmsService smsService,
                       DepartmentService departmentService, DataScopeService dataScopeService) {
        this.mapper = mapper;
        this.tokenStore = tokenStore;
        this.smsService = smsService;
        this.departmentService = departmentService;
        this.dataScopeService = dataScopeService;
    }

    /** 员工编号（或登录名/手机号）+ 密码登录 */
    @Transactional(readOnly = true)
    public Map<String, Object> login(String account, String rawPassword) {
        String key = account == null ? "" : account.trim();
        if (key.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("请输入手机号/工号和密码");
        }
        AccountRow row = mapper.findByAccount(normalizeAccount(key));
        if (row == null || !PasswordHasher.verify(rawPassword, row.passwordHash)) {
            throw new IllegalArgumentException("账号或密码不正确");
        }
        if (row.status == null || row.status == 0) {
            throw new IllegalArgumentException("该账号已停用，请联系管理员");
        }
        return issue(row);
    }

    /** 手机号 + 短信验证码登录 */
    @Transactional
    public Map<String, Object> loginBySms(String phone, String code) {
        String normalized = SmsService.normalizePhone(phone);
        if (!SmsService.isPhone(normalized)) {
            throw new IllegalArgumentException("请输入正确的 11 位手机号");
        }
        smsService.verify(normalized, SmsService.SCENE_LOGIN, code);

        AccountRow row = mapper.findByPhone(normalized);
        if (row == null) {
            throw new IllegalArgumentException("该手机号未绑定员工账号，请联系管理员");
        }
        if (row.status == null || row.status == 0) {
            throw new IllegalArgumentException("该账号已停用，请联系管理员");
        }
        return issue(row);
    }

    /** 新用户注册；手机号唯一索引作为并发注册的最终保障 */
    @Transactional
    public Map<String, Object> register(String rawPhone, String code, String rawPassword, String rawName, Long deptId) {
        String phone = SmsService.normalizePhone(rawPhone);
        if (!SmsService.isPhone(phone)) throw new IllegalArgumentException("请输入正确的 11 位手机号");
        if (rawName == null || rawName.isBlank()) throw new IllegalArgumentException("姓名不能为空");
        if (deptId == null) throw new IllegalArgumentException("请选择所属部门");
        if (!departmentService.isSelectable(deptId)) throw new IllegalArgumentException("所选部门不可用，请重新选择");
        if (rawPassword == null || rawPassword.length() < 6 || rawPassword.length() > 32) {
            throw new IllegalArgumentException("密码长度需为 6 到 32 位");
        }
        if (mapper.findByPhone(phone) != null) throw new IllegalArgumentException("该手机号已注册");

        smsService.verify(phone, SmsService.SCENE_REGISTER, code);
        // 角色由部门决定：注册页选了哪个部门，就自动拿到该部门配置的角色
        Long roleId = departmentService.roleIdOf(deptId);
        if (roleId == null) throw new IllegalArgumentException("所选部门暂未配置角色，请联系管理员");

        AccountRow account = new AccountRow();
        account.phone = phone;
        // 工号先用占位符，拿到自增 ID 后再格式化为 U0001，保证唯一且可读
        account.employeeNo = "P" + System.currentTimeMillis();
        account.username = phone;
        account.name = rawName.trim();
        account.departmentId = deptId;
        account.passwordHash = PasswordHasher.hash(rawPassword);
        try {
            mapper.insertRegisteredAccount(account);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("该手机号已注册");
        }
        account.employeeNo = String.format("U%04d", account.id);
        mapper.updateEmployeeNo(account.id, account.employeeNo);
        mapper.insertEmployeeRole(account.id, roleId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", account.id);
        result.put("empNo", account.employeeNo);
        result.put("phone", phone);
        result.put("name", account.name);
        result.put("deptId", deptId);
        result.put("deptName", mapper.departmentName(deptId));
        result.put("roleName", mapper.roleNameOf(roleId));
        result.put("message", "注册成功");
        return result;
    }

    /** 只把纯数字/带分隔符的输入当手机号处理，工号（含字母）原样保留 */
    private String normalizeAccount(String input) {
        if (input.matches("[+0-9\\-\\s]+")) return SmsService.normalizePhone(input);
        return input;
    }

    private Map<String, Object> issue(AccountRow row) {
        String token = tokenStore.issue(row.id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", buildUser(row));
        return result;
    }

    @Transactional(readOnly = true)
    public CurrentUser currentUser(Long employeeId) {
        AccountRow row = mapper.findById(employeeId);
        if (row == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        return buildUser(row);
    }

    /** 修改登录密码：校验原密码，改完旧令牌全部失效并下发新令牌 */
    @Transactional
    public Map<String, Object> changePassword(Long employeeId, String oldPassword, String newPassword) {
        AccountRow row = mapper.findById(employeeId);
        if (row == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        if (!PasswordHasher.verify(oldPassword, row.passwordHash)) {
            throw new IllegalArgumentException("原密码不正确");
        }
        validateNewPassword(newPassword, oldPassword);
        mapper.updatePassword(row.id, PasswordHasher.hash(newPassword));

        tokenStore.revokeAll(row.id);
        String token = tokenStore.issue(row.id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("message", "密码修改成功，请牢记新密码");
        return result;
    }

    public void logout(String token) {
        tokenStore.revoke(token);
    }

    /**
     * 演示用：登录页展示全部可用账号，并算出每个账号登录后能看到的菜单，
     * 便于直接对比不同角色的权限差异。不返回任何密码信息。
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> demoAccounts() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AccountRow row : mapper.listActiveAccounts()) {
            List<RoleBrief> roles = mapper.rolesOf(row.id);
            boolean isAdmin = roles.stream().anyMatch((role) -> ADMIN_ROLE_CODE.equals(role.code));
            List<String> permissions = isAdmin ? mapper.allPermissionCodes() : mapper.permissionCodesOf(row.id);

            List<String> menus = new ArrayList<>();
            for (String[] item : MENU_RULES) {
                if (permissions.contains(item[0])) menus.add(item[1]);
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("empNo", row.employeeNo);
            item.put("name", row.name);
            item.put("phone", row.phone);
            item.put("deptName", row.departmentId == null ? null : mapper.departmentName(row.departmentId));
            item.put("roleNames", roles.stream().map((role) -> role.name).toList());
            item.put("menus", menus);
            item.put("isAdmin", isAdmin);
            list.add(item);
        }
        return list;
    }

    private void validateNewPassword(String newPassword, String oldPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少 6 位");
        }
        if (newPassword.length() > 32) {
            throw new IllegalArgumentException("新密码不能超过 32 位");
        }
        if (newPassword.equals(oldPassword)) {
            throw new IllegalArgumentException("新密码不能与原密码相同");
        }
    }

    private CurrentUser buildUser(AccountRow row) {
        List<RoleBrief> roles = mapper.rolesOf(row.id);
        boolean isAdmin = roles.stream().anyMatch((role) -> ADMIN_ROLE_CODE.equals(role.code));
        List<String> permissions = isAdmin ? mapper.allPermissionCodes() : mapper.permissionCodesOf(row.id);
        String deptName = row.departmentId == null ? null : mapper.departmentName(row.departmentId);
        return new CurrentUser(
                row.id,
                row.employeeNo,
                row.username,
                row.name,
                row.phone,
                row.departmentId,
                deptName,
                row.status,
                roles,
                permissions,
                isAdmin,
                dataScopeService.of(row.id)
        );
    }
}
