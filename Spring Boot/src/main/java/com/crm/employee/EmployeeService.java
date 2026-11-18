package com.crm.employee;

import com.crm.auth.RoleBrief;
import com.crm.common.PasswordHasher;
import com.crm.common.TokenStore;
import com.crm.sms.SmsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeService {
    private static final String ADMIN_ROLE_CODE = "ADMIN";
    private static final String DEFAULT_PASSWORD = "123456";

    private final EmployeeMapper mapper;
    private final TokenStore tokenStore;

    public EmployeeService(EmployeeMapper mapper, TokenStore tokenStore) {
        this.mapper = mapper;
        this.tokenStore = tokenStore;
    }

    public record EmployeeRequest(
            String empNo,
            String name,
            String position,
            Long deptId,
            List<Long> roleIds,
            String phone,
            String email,
            Integer status,
            String password,
            /** 数据范围：ALL 全公司 / GROUP 本组 / SELF 仅本人；留空按岗位推导 */
            String dataScope
    ) {
    }

    @Transactional(readOnly = true)
    public List<EmployeeView> list() {
        Map<Long, String> roleNames = roleNameMap();
        List<EmployeeView> views = new ArrayList<>();
        for (EmployeeRow row : mapper.findAll()) {
            views.add(toView(row, roleNames));
        }
        return views;
    }

    @Transactional
    public EmployeeView create(EmployeeRequest request) {
        validate(request, null);
        EmployeeRow row = new EmployeeRow();
        row.employeeNo = request.empNo().trim();
        row.username = row.employeeNo;
        row.passwordHash = PasswordHasher.hash(
                request.password() == null || request.password().isBlank() ? DEFAULT_PASSWORD : request.password());
        row.name = request.name().trim();
        row.position = request.position() == null ? "" : request.position().trim();
        row.departmentId = request.deptId();
        applySalesDefaults(row);
        row.dataScope = scopeFor(request.dataScope(), row.position);
        row.phone = SmsService.normalizePhone(request.phone());
        row.email = request.email();
        row.status = request.status() == null ? 1 : request.status();

        mapper.insert(row);
        replaceRoles(row.id, request.roleIds());
        return toView(mapper.findById(row.id), roleNameMap());
    }

    @Transactional
    public EmployeeView update(Long id, EmployeeRequest request, Long operatorId) {
        EmployeeRow exists = mapper.findById(id);
        if (exists == null) throw new IllegalArgumentException("员工不存在");
        validate(request, id);

        if (request.status() != null && request.status() == 0 && id.equals(operatorId)) {
            throw new IllegalArgumentException("不能停用当前登录账号");
        }
        boolean wasAdmin = isAdmin(exists.roleIds);
        boolean willBeAdmin = request.roleIds() != null && isAdminRoleIds(request.roleIds());
        if (wasAdmin && !willBeAdmin && mapper.countAdmins() <= 1) {
            throw new IllegalArgumentException("系统需保留至少一个管理员账号");
        }

        EmployeeRow row = new EmployeeRow();
        row.id = id;
        row.employeeNo = request.empNo().trim();
        row.username = row.employeeNo;
        row.name = request.name().trim();
        row.position = request.position() == null
                ? (exists.position == null ? "" : exists.position)
                : request.position().trim();
        row.departmentId = request.deptId();
        applySalesDefaults(row);
        row.dataScope = scopeFor(request.dataScope(), row.position);
        row.phone = SmsService.normalizePhone(request.phone());
        row.email = request.email();
        row.status = request.status() == null ? exists.status : request.status();
        mapper.update(row);

        if (request.roleIds() != null) replaceRoles(id, request.roleIds());
        if (request.password() != null && !request.password().isBlank()) {
            mapper.resetPassword(id, PasswordHasher.hash(request.password()));
            tokenStore.revokeAll(id);
        }
        return toView(mapper.findById(id), roleNameMap());
    }

    @Transactional
    public void delete(Long id, Long operatorId) {
        if (id.equals(operatorId)) throw new IllegalArgumentException("不能删除当前登录账号");
        EmployeeRow exists = mapper.findById(id);
        if (exists == null) throw new IllegalArgumentException("员工不存在");
        if (isAdmin(exists.roleIds) && mapper.countAdmins() <= 1) {
            throw new IllegalArgumentException("系统需保留至少一个管理员账号");
        }
        mapper.delete(id);
        tokenStore.revokeAll(id);
    }

    /**
     * 销售岗位自动落到对应销售组（部门名 = 销售一组 / 销售二组 / 销售部），
     * 这样在员工管理里新增的销售也会出现在「销售团队」，不必两个地方各建一次。
     * 能力等级与配额只给默认值，后续可在销售团队页调整。
     */
    private void applySalesDefaults(EmployeeRow row) {
        if (row.position == null || !row.position.startsWith("销售")) {
            // 调离销售岗位就把销售组清掉，团队页不再显示这个已转岗的人
            row.salesGroup = "";
            return;
        }
        String deptName = row.departmentId == null ? null : mapper.deptName(row.departmentId);
        if (deptName == null) return;
        if (!deptName.startsWith("销售")) return;
        row.salesGroup = deptName;
        if ("销售总监".equals(row.position)) {
            row.salesLevel = row.salesLevel == null ? "S" : row.salesLevel;
            row.clueCapacity = row.clueCapacity == null ? 8 : row.clueCapacity;
        } else if ("销售组长".equals(row.position)) {
            row.salesLevel = row.salesLevel == null ? "A" : row.salesLevel;
            row.clueCapacity = row.clueCapacity == null ? 6 : row.clueCapacity;
        } else {
            row.salesLevel = row.salesLevel == null ? "B" : row.salesLevel;
            row.clueCapacity = row.clueCapacity == null ? 5 : row.clueCapacity;
        }
    }

    /**
     * 数据范围：优先用管理员在员工管理里选的值，留空则按岗位推导。
     * 组长带团队看本组；销售顾问只看自己；总监与职能岗看全公司。
     */
    private String scopeFor(String raw, String position) {
        String value = raw == null ? "" : raw.trim().toUpperCase();
        if ("ALL".equals(value) || "GROUP".equals(value) || "SELF".equals(value)) return value;
        String pos = position == null ? "" : position;
        if (pos.contains("组长") || pos.contains("主管") || pos.contains("经理")) return "GROUP";
        if (pos.startsWith("销售") && !pos.contains("总监")) return "SELF";
        return "ALL";
    }

    /** 新增/编辑员工时的校验；手机号同时是登录账号，必须唯一且格式正确 */
    private void validate(EmployeeRequest request, Long excludeId) {
        if (request.empNo() == null || request.empNo().isBlank()) {
            throw new IllegalArgumentException("员工编号不能为空");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (request.deptId() == null) {
            throw new IllegalArgumentException("请选择所属部门");
        }
        if (request.roleIds() == null || request.roleIds().isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个角色");
        }
        String phone = SmsService.normalizePhone(request.phone());
        if (phone.isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空，员工需用手机号登录");
        }
        if (!SmsService.isPhone(phone)) {
            throw new IllegalArgumentException("请输入正确的 11 位手机号");
        }
        if (mapper.countByPhone(phone, excludeId) > 0) {
            throw new IllegalArgumentException("该手机号已被其他员工使用");
        }
    }

    private void replaceRoles(Long employeeId, List<Long> roleIds) {
        mapper.clearRoles(employeeId);
        if (roleIds == null) return;
        for (Long roleId : roleIds) {
            if (roleId != null) mapper.insertRole(employeeId, roleId);
        }
    }

    private Map<Long, String> roleNameMap() {
        Map<Long, String> map = new LinkedHashMap<>();
        for (RoleBrief role : mapper.allRoles()) map.put(role.id, role.name);
        return map;
    }

    private List<Long> parseIds(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter((s) -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }

    private boolean isAdmin(String roleIds) {
        return isAdminRoleIds(parseIds(roleIds));
    }

    private boolean isAdminRoleIds(List<Long> roleIds) {
        return mapper.allRoles().stream()
                .anyMatch((role) -> ADMIN_ROLE_CODE.equals(role.code) && roleIds.contains(role.id));
    }

    private EmployeeView toView(EmployeeRow row, Map<Long, String> roleNames) {
        List<Long> ids = parseIds(row.roleIds);
        List<String> names = ids.stream()
                .map((id) -> roleNames.getOrDefault(id, "未知角色"))
                .toList();
        return new EmployeeView(
                row.id,
                row.employeeNo,
                row.name,
                row.position == null ? "" : row.position,
                row.departmentId,
                row.deptName,
                ids,
                names,
                row.phone,
                row.email,
                row.status,
                row.createdAt,
                row.dataScope == null ? "" : row.dataScope
        );
    }
}
