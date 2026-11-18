package com.crm.common;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 解析当前登录员工的数据范围。
 *
 * <p>优先级：员工表手工配置的 data_scope > 按岗位推导。
 * 推导规则与行业常见做法一致（全部 / 本部门及下属 / 本人）：
 * 管理员与销售总监看全公司，销售组长看本组，销售顾问只看本人，职能岗（财务/运营/售后等）看全公司。
 */
@Service
public class DataScopeService {
    private final DataScopeMapper mapper;

    public DataScopeService(DataScopeMapper mapper) {
        this.mapper = mapper;
    }

    public DataScope of(Long employeeId) {
        if (employeeId == null) {
            return new DataScope(null, "", "", "", DataScope.SELF, "仅本人负责数据", List.of());
        }
        ScopeEmployeeRow row = mapper.employee(employeeId);
        if (row == null) {
            return new DataScope(employeeId, "", "", "", DataScope.SELF, "仅本人负责数据", List.of());
        }

        String type = resolveType(row, mapper.isAdmin(employeeId) > 0);
        List<String> owners = switch (type) {
            case DataScope.ALL -> null;
            case DataScope.GROUP -> {
                List<String> list = mapper.groupMemberNames(employeeId);
                yield list.isEmpty() ? List.of(row.name) : list;
            }
            default -> List.of(row.name);
        };
        return new DataScope(employeeId, row.name, nz(row.position), nz(row.deptName), type, labelOf(type, owners), owners);
    }

    private String resolveType(ScopeEmployeeRow row, boolean admin) {
        if (admin) return DataScope.ALL;
        String configured = nz(row.dataScope).trim().toUpperCase();
        if (DataScope.ALL.equals(configured) || DataScope.GROUP.equals(configured) || DataScope.SELF.equals(configured)) {
            return configured;
        }
        String position = nz(row.position);
        if (position.contains("总监") || position.contains("管理员")) return DataScope.ALL;
        if (position.contains("组长") || position.contains("主管") || position.contains("经理")) return DataScope.GROUP;
        if (position.startsWith("销售")) return DataScope.SELF;
        // 财务 / 运营 / 售后等职能岗需要跨客户协作，默认全公司
        return DataScope.ALL;
    }

    private String labelOf(String type, List<String> owners) {
        return switch (type) {
            case DataScope.ALL -> "全公司数据";
            case DataScope.GROUP -> "本组数据（" + (owners == null ? 0 : owners.size()) + " 人）";
            default -> "仅本人负责数据";
        };
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
