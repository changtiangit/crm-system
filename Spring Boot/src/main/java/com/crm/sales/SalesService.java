package com.crm.sales;

import com.crm.business.BusinessRow;
import com.crm.common.DataScope;
import com.crm.common.DataScopeService;
import com.crm.common.ForbiddenException;
import com.crm.common.PasswordHasher;
import com.crm.sms.SmsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 销售团队与线索分配。
 *
 * 分配规则（智能分配）：
 *   1. 待分配池按「意向高 → 低、先到先分」排序；
 *   2. 每位销售每次被打分：剩余容量 × 能力权重（S 1.2 / A 1.1 / B 1.0 / C 0.8）；
 *   3. 每条线索都分给当前分数最高的人，分完即扣减其剩余容量，再取下一条。
 * 这样能力强的销售容量更大、权重更高，会自然拿到更多线索；新人容量小、权重低，只接少量。
 */
@Service
public class SalesService {
    private static final String DEFAULT_PASSWORD = "123456";
    private static final Set<String> POSITIONS = Set.of("销售总监", "销售组长", "销售顾问");
    private static final Map<String, Double> LEVEL_WEIGHT = Map.of(
            "S", 1.2, "A", 1.1, "B", 1.0, "C", 0.8);

    private final SalesMapper mapper;
    private final DataScopeService scopeService;

    public SalesService(SalesMapper mapper, DataScopeService scopeService) {
        this.mapper = mapper;
        this.scopeService = scopeService;
    }

    public record MemberRequest(
            String empNo,
            String name,
            String phone,
            String grp,
            String position,
            String level,
            Integer capacity,
            Long leaderId,
            Integer status
    ) {
    }

    public record AssignRequest(List<Long> ids, String owner, Boolean force) {
    }

    public record AutoAssignRequest(List<Long> ids, String grp) {
    }

    // ---------------- 团队 ----------------

    @Transactional(readOnly = true)
    public Map<String, Object> team(Long userId) {
        DataScope scope = scopeService.of(userId);
        List<SalesMemberRow> members = decorate(mapper.members());
        // 组长只看本组、销售只看自己：团队页不把别人的负载与业绩摊开
        if (!scope.isAll()) {
            members = members.stream().filter((m) -> scope.owners().contains(m.name)).toList();
        }

        Map<String, List<SalesMemberRow>> grouped = new LinkedHashMap<>();
        members.forEach((m) -> grouped.computeIfAbsent(
                m.grp == null || m.grp.isBlank() ? "未分组" : m.grp, (k) -> new ArrayList<>()).add(m));

        List<Map<String, Object>> groups = new ArrayList<>();
        grouped.forEach((name, list) -> {
            SalesMemberRow leader = list.stream()
                    .filter((m) -> "销售组长".equals(m.position) || "销售总监".equals(m.position))
                    .findFirst().orElse(null);
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("name", name);
            g.put("leaderName", leader == null ? "" : leader.name);
            g.put("leaderPosition", leader == null ? "" : leader.position);
            g.put("members", list);
            g.put("capacity", list.stream().mapToInt((m) -> nz(m.capacity)).sum());
            g.put("inHand", list.stream().mapToInt((m) -> nz(m.inHand)).sum());
            g.put("remaining", list.stream().mapToInt((m) -> nz(m.remaining)).sum());
            g.put("converted", list.stream().mapToInt((m) -> nz(m.converted)).sum());
            groups.add(g);
        });

        int capacity = members.stream().mapToInt((m) -> nz(m.capacity)).sum();
        int inHand = members.stream().mapToInt((m) -> nz(m.inHand)).sum();
        int pending = mapper.pendingCount();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("members", members.size());
        stats.put("capacity", capacity);
        stats.put("inHand", inHand);
        stats.put("remaining", Math.max(0, capacity - inHand));
        stats.put("pending", pending);
        stats.put("monthNew", mapper.monthNewCount());
        stats.put("converted", members.stream().mapToInt((m) -> nz(m.converted)).sum());
        stats.put("loadRate", rate(inHand, capacity));
        stats.put("groups", groups.size());
        stats.put("topLevel", members.stream().filter((m) -> "S".equals(m.level)).count());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("members", members);
        result.put("groups", groups);
        result.put("stats", stats);
        // 最近动态同样按范围过滤，避免看到别组的分配记录
        result.put("recent", mapper.recentLogs().stream()
                .filter((log) -> scope.isAll() || scope.owners().contains(log.toOwner))
                .toList());
        result.put("dataScope", scope);
        return result;
    }

    /** 依据线索表实时补齐每位销售的负载与转化指标 */
    private List<SalesMemberRow> decorate(List<SalesMemberRow> members) {
        Map<String, ClueStatRow> stats = mapper.clueStats().stream()
                .collect(Collectors.toMap((s) -> s.owner, (s) -> s, (a, b) -> a));
        for (SalesMemberRow m : members) {
            ClueStatRow s = stats.get(m.name);
            m.total = s == null ? 0 : nz(s.total);
            m.inHand = s == null ? 0 : nz(s.inHand);
            m.converted = s == null ? 0 : nz(s.converted);
            m.monthNew = s == null ? 0 : nz(s.monthNew);
            m.capacity = nz(m.capacity);
            m.remaining = Math.max(0, m.capacity - m.inHand);
            m.convRate = rate(m.converted, m.total);
            m.loadRate = rate(m.inHand, m.capacity);
            m.weight = BigDecimal.valueOf(LEVEL_WEIGHT.getOrDefault(m.level, 0.9))
                    .setScale(2, RoundingMode.HALF_UP);
            m.assignable = nz(m.status) == 1 && m.remaining > 0 && POSITIONS.contains(m.position);
        }
        return members;
    }

    // ---------------- 分配 ----------------

    /** 手动分配 / 转派：指定一个人，可强制超过配额 */
    @Transactional
    public Map<String, Object> assign(AssignRequest request, Long operatorId) {
        if (request == null || request.ids() == null || request.ids().isEmpty()) {
            throw new IllegalArgumentException("请先选择要分配的线索");
        }
        if (request.owner() == null || request.owner().isBlank()) {
            throw new IllegalArgumentException("请选择接收线索的销售");
        }
        SalesMemberRow target = decorate(mapper.members()).stream()
                .filter((m) -> m.name.equals(request.owner().trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("「" + request.owner() + "」不在销售团队里，请先在团队页添加"));

        DataScope scope = scopeService.of(operatorId);
        requireAssignScope(scope);
        if (!scope.canSee(target.name)) {
            throw new ForbiddenException("「" + target.name + "」不在你的数据范围（" + scope.label() + "）内，只能把线索分给本组成员");
        }

        if (nz(target.status) != 1) throw new IllegalArgumentException("该销售已停用，不能接收线索");

        boolean force = Boolean.TRUE.equals(request.force());
        long need = request.ids().stream()
                .map(mapper::clueById).filter((c) -> c != null && !target.name.equals(c.owner)).count();
        if (!force && target.remaining < need) {
            throw new IllegalArgumentException(String.format(
                    "%s 剩余容量只有 %d 条，本次要分配 %d 条；可调整配额、改派他人，或确认强制分配",
                    target.name, target.remaining, need));
        }

        String operator = operatorName(operatorId);
        int assigned = 0;
        for (Long id : request.ids()) {
            BusinessRow clue = mapper.clueById(id);
            if (clue == null) continue;
            // 别组的线索不能通过接口「抢」过来分给自己人
            if (!scope.canManage(clue.owner)) throw new ForbiddenException(scope.denyMessage(clue.owner));
            if (target.name.equals(clue.owner)) continue;
            String status = (clue.status == null || clue.status.isBlank() || "待分配".equals(clue.status))
                    ? "待跟进" : clue.status;
            mapper.assignClue(id, target.name, status, operator);
            mapper.insertLog(id, clue.name, clue.owner == null ? "" : clue.owner, target.name, operator, "手动");
            assigned++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assigned", assigned);
        result.put("owner", target.name);
        result.put("force", force);
        result.put("remaining", Math.max(0, target.remaining - assigned));
        return result;
    }

    /** 智能分配：按剩余容量 × 能力权重逐条择优 */
    @Transactional
    public Map<String, Object> autoAssign(AutoAssignRequest request, Long operatorId) {
        List<BusinessRow> pending = mapper.pendingClues();
        if (request != null && request.ids() != null && !request.ids().isEmpty()) {
            Set<Long> wanted = Set.copyOf(request.ids());
            pending = pending.stream().filter((c) -> wanted.contains(c.id)).toList();
        }
        if (pending.isEmpty()) {
            throw new IllegalArgumentException("待分配池里没有线索，无需分配");
        }

        String grp = request == null || request.grp() == null ? "" : request.grp().trim();
        DataScope scope = scopeService.of(operatorId);
        requireAssignScope(scope);
        List<SalesMemberRow> pool = decorate(mapper.members()).stream()
                .filter((m) -> Boolean.TRUE.equals(m.assignable))
                .filter((m) -> scope.isAll() || scope.owners().contains(m.name))
                .filter((m) -> grp.isEmpty() || grp.equals(m.grp))
                .toList();
        if (pool.isEmpty()) {
            throw new IllegalArgumentException(scope.isAll()
                    ? (grp.isEmpty()
                        ? "销售团队当前没有可接线索的人：请确认成员在职且还有剩余容量"
                        : "「" + grp + "」当前没有可接线索的销售")
                    : "你的数据范围（" + scope.label() + "）内没有可接线索的销售");
        }

        Map<Long, Integer> remaining = new LinkedHashMap<>();
        pool.forEach((m) -> remaining.put(m.id, m.remaining));
        String operator = operatorName(operatorId);

        List<Map<String, Object>> details = new ArrayList<>();
        int assigned = 0;
        List<BusinessRow> queue = new ArrayList<>(pending);

        // 保底轮：线索够分时每人先拿 1 条，优先给新人、从意向最低的线索里取，
        // 保证新人也有练手机会，同时不占用高意向线索。
        if (queue.size() >= pool.size() && pool.size() > 1) {
            List<SalesMemberRow> warmOrder = new ArrayList<>(pool);
            warmOrder.sort(Comparator.comparingDouble((SalesMemberRow m) -> m.weight.doubleValue())
                    .thenComparing(Comparator.comparingInt((SalesMemberRow m) -> m.inHand).reversed()));
            for (SalesMemberRow m : warmOrder) {
                BusinessRow clue = queue.remove(queue.size() - 1);
                String status = (clue.status == null || clue.status.isBlank() || "待分配".equals(clue.status))
                        ? "待跟进" : clue.status;
                mapper.assignClue(clue.id, m.name, status, operator);
                mapper.insertLog(clue.id, clue.name, clue.owner == null ? "" : clue.owner, m.name, operator, "智能");
                remaining.put(m.id, remaining.get(m.id) - 1);
                assigned++;
                details.add(detailOf(clue, m));
            }
        }

        for (BusinessRow clue : queue) {
            SalesMemberRow pick = pool.stream()
                    .filter((m) -> remaining.getOrDefault(m.id, 0) > 0)
                    .max(Comparator
                            .comparingDouble((SalesMemberRow m) -> remaining.get(m.id) * m.weight.doubleValue())
                            .thenComparingDouble((m) -> m.weight.doubleValue())
                            .thenComparingInt((m) -> -m.inHand))
                    .orElse(null);
            if (pick == null) break;

            String status = (clue.status == null || clue.status.isBlank() || "待分配".equals(clue.status))
                    ? "待跟进" : clue.status;
            mapper.assignClue(clue.id, pick.name, status, operator);
            mapper.insertLog(clue.id, clue.name, clue.owner == null ? "" : clue.owner, pick.name, operator, "智能");
            remaining.put(pick.id, remaining.get(pick.id) - 1);
            assigned++;

            details.add(detailOf(clue, pick));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assigned", assigned);
        result.put("left", Math.max(0, pending.size() - assigned));
        result.put("details", details);
        result.put("operator", operator);

        Map<String, Integer> byOwner = new LinkedHashMap<>();
        details.forEach((d) -> byOwner.merge((String) d.get("owner"), 1, Integer::sum));
        result.put("byOwner", byOwner);
        return result;
    }

    private Map<String, Object> detailOf(BusinessRow clue, SalesMemberRow member) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("clueName", clue.name);
        d.put("intent", clue.intent);
        d.put("owner", member.name);
        d.put("grp", member.grp);
        d.put("level", member.level);
        return d;
    }

    // ---------------- 成员维护 ----------------

    @Transactional
    public Map<String, Object> createMember(MemberRequest req, Long operatorId) {
        requireAllScope(operatorId, "新增团队成员");
        if (req == null) throw new IllegalArgumentException("参数不能为空");
        String empNo = trim(req.empNo());
        String name = trim(req.name());
        if (empNo.isEmpty()) throw new IllegalArgumentException("工号不能为空");
        if (name.isEmpty()) throw new IllegalArgumentException("姓名不能为空");
        if (mapper.countEmpNo(empNo) > 0) throw new IllegalArgumentException("工号已存在");
        String phone = SmsService.normalizePhone(req.phone());
        if (!SmsService.isPhone(phone)) throw new IllegalArgumentException("请输入正确的 11 位手机号（同时作为登录账号）");
        if (mapper.countPhone(phone) > 0) throw new IllegalArgumentException("该手机号已被其他员工使用");

        String grp = trim(req.grp());
        Long deptId = mapper.deptIdByName(grp);
        if (deptId == null) throw new IllegalArgumentException("所属销售组「" + grp + "」不存在，请先在部门管理里创建");

        SalesMemberRow row = new SalesMemberRow();
        row.empNo = empNo;
        row.name = name;
        row.phone = phone;
        row.grp = grp;
        row.position = POSITIONS.contains(trim(req.position())) ? trim(req.position()) : "销售顾问";
        row.level = normalizeLevel(req.level());
        row.capacity = req.capacity() == null ? 5 : Math.max(0, req.capacity());
        row.status = req.status() == null ? 1 : req.status();
        row.leaderId = validLeader(req.leaderId(), null);
        row.departmentId = deptId;
        row.passwordHash = PasswordHasher.hash(DEFAULT_PASSWORD);
        mapper.insertMember(row);

        Long roleId = mapper.roleIdByCode(roleCodeOf(grp));
        if (roleId != null) mapper.insertMemberRole(row.id, roleId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.id);
        result.put("empNo", row.empNo);
        result.put("name", row.name);
        result.put("password", DEFAULT_PASSWORD);
        return result;
    }

    @Transactional
    public SalesMemberRow updateMember(Long id, MemberRequest req, Long operatorId) {
        requireAllScope(operatorId, "调整团队成员");
        SalesMemberRow exists = mapper.memberById(id);
        if (exists == null) throw new IllegalArgumentException("团队成员不存在");
        String grp = trim(req.grp());
        if (mapper.deptIdByName(grp) == null) {
            throw new IllegalArgumentException("所属销售组「" + grp + "」不存在，请先在部门管理里创建");
        }
        exists.position = POSITIONS.contains(trim(req.position())) ? trim(req.position()) : exists.position;
        exists.level = normalizeLevel(req.level());
        exists.capacity = req.capacity() == null ? exists.capacity : Math.max(0, req.capacity());
        exists.grp = grp;
        exists.leaderId = validLeader(req.leaderId(), id);
        exists.status = req.status() == null ? exists.status : req.status();

        mapper.updateMember(exists);
        // 组别可能变了：先清掉旧的销售组角色，再按新组补上，保证菜单权限跟着组走
        mapper.clearSalesRoles(id);
        Long roleId = mapper.roleIdByCode(roleCodeOf(grp));
        if (roleId != null) mapper.insertMemberRole(id, roleId);
        return mapper.memberById(id);
    }

    // ---------------- 工具 ----------------

    /** 分配线索是管理动作：销售顾问只能接线索，不能给他人分配 */
    private void requireAssignScope(DataScope scope) {
        if (scope.isSelf()) {
            throw new ForbiddenException("销售顾问不能分配线索，请联系组长或运营");
        }
    }

    /** 团队编制（新增成员、调组、改配额）属于管理动作，组长不能自行扩编 */
    private void requireAllScope(Long operatorId, String action) {
        DataScope scope = scopeService.of(operatorId);
        if (!scope.isAll()) {
            throw new ForbiddenException("你的数据范围是「" + scope.label() + "」，只有管理员或销售总监可以" + action);
        }
    }

    private Long validLeader(Long leaderId, Long selfId) {
        if (leaderId == null) return null;
        if (selfId != null && leaderId.equals(selfId)) throw new IllegalArgumentException("不能把自己设为直属组长");
        if (mapper.employeeName(leaderId) == null) throw new IllegalArgumentException("直属组长不存在");
        return leaderId;
    }

    private String roleCodeOf(String grp) {
        if (grp == null) return "SALES";
        if (grp.contains("一")) return "SALES_G1";
        if (grp.contains("二")) return "SALES_G2";
        return "SALES";
    }

    private String normalizeLevel(String level) {
        String v = trim(level).toUpperCase();
        return LEVEL_WEIGHT.containsKey(v) ? v : "B";
    }

    private String operatorName(Long operatorId) {
        if (operatorId == null) return "系统";
        String name = mapper.employeeName(operatorId);
        return name == null ? "系统" : name;
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /** 百分比：保留一位小数，容量为 0 时返回 0 */
    private static BigDecimal rate(int part, int whole) {
        if (whole <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part * 100.0 / whole).setScale(1, RoundingMode.HALF_UP);
    }
}
