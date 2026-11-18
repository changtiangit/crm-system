package com.crm.common;

import java.util.List;

/**
 * 当前登录用户的数据范围（行级权限）。
 *
 * <p>功能权限回答「能进哪个页面」，数据范围回答「页面里能看到谁的数据」：
 * <ul>
 *   <li>ALL   全公司：管理员、销售总监、财务 / 运营 / 售后等职能岗</li>
 *   <li>GROUP 本组：销售组长本人 + 直属组员负责的客户、线索、订单</li>
 *   <li>SELF  仅本人：销售顾问只看到自己负责的数据</li>
 * </ul>
 * owners 为 null 表示不限制；否则列出可以看到的负责人姓名。
 */
public record DataScope(
        Long employeeId,
        String employeeName,
        String position,
        String department,
        String type,
        String label,
        List<String> owners
) {
    public static final String ALL = "ALL";
    public static final String GROUP = "GROUP";
    public static final String SELF = "SELF";

    public boolean isAll() {
        return ALL.equals(type);
    }

    public boolean isGroup() {
        return GROUP.equals(type);
    }

    public boolean isSelf() {
        return SELF.equals(type);
    }

    /** 这条数据是否在可见范围内（owner 为空表示待分配，不属于任何个人） */
    public boolean canSee(String owner) {
        if (isAll()) return true;
        if (owner == null || owner.isBlank()) return false;
        return owners != null && owners.contains(owner.trim());
    }

    /** 组长可以处理待分配的公共线索，普通销售不行 */
    public boolean canManage(String owner) {
        if (canSee(owner)) return true;
        return isGroup() && (owner == null || owner.isBlank());
    }

    /** 拒绝时的统一提示，写清归属与当前范围，便于用户理解为什么看不到 */
    public String denyMessage(String owner) {
        String who = owner == null || owner.isBlank() ? "公海待分配数据" : "「" + owner + "」";
        return "该数据由" + who + "负责，你的数据范围是「" + label + "」，无权查看或操作";
    }
}
