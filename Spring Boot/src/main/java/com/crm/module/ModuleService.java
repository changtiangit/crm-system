package com.crm.module;

import com.crm.common.DataScope;
import com.crm.common.ForbiddenException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ModuleService {
    private static final Set<String> MODULES = Set.of(
            "pool-customers", "contacts", "followups", "todos", "quotes", "returns",
            "shipments", "receipts", "receivables", "tickets", "revisits");
    /** 待办是「私人物品」，不受数据范围影响：谁的账谁的活，管理员也不替他看 */
    private static final String TODOS = "todos";
    private final ModuleMapper mapper;
    private final ModuleSyncService sync;
    private final ObjectMapper json;

    public ModuleService(ModuleMapper mapper, ModuleSyncService sync, ObjectMapper json) {
        this.mapper = mapper;
        this.sync = sync;
        this.json = json;
    }

    public List<Map<String, Object>> list(String module, DataScope scope) {
        checkModule(module);
        if (TODOS.equals(module)) return myTodos(module, scope);
        return mapper.list(module).stream().filter(row -> "pool-customers".equals(module) || scope.isAll() || scope.canSee(row.scopeOwner))
                .map(this::toMap).toList();
    }

    /**
     * 首次进入某个模块时写入演示数据。
     * 待办按「人」播种（seed_state 记 todos@姓名），因为每个岗位该干的事不一样，
     * 前端会按岗位画像准备不同的一套模板；其他模块仍按模块播种一次。
     */
    @Transactional
    public List<Map<String, Object>> seed(String module, List<Map<String, Object>> rows, DataScope scope) {
        checkModule(module);
        if (TODOS.equals(module)) {
            String me = scope.employeeName();
            if (me == null || me.isBlank()) throw new ForbiddenException("请先登录后再操作待办");
            if (mapper.markSeeded(TODOS + "@" + me) > 0 && myTodos(module, scope).isEmpty()) {
                for (Map<String, Object> data : rows) {
                    data.put("owner", me);
                    save(module, null, data);
                }
            }
            return myTodos(module, scope);
        }
        if (mapper.markSeeded(module) > 0) {
            if (mapper.count(module) == 0) {
                for (Map<String, Object> data : rows) save(module, null, data);
            }
        }
        return list(module, new DataScope(null, "", "", "", DataScope.ALL, "全公司数据", null));
    }

    public Map<String, Object> create(String module, Map<String, Object> data, DataScope scope) {
        checkModule(module);
        // 待办的负责人永远是提交人，前端传谁的都不算
        if (TODOS.equals(module)) data.put("owner", scope.employeeName());
        else applyOwner(data, scope);
        Map<String, Object> saved = save(module, null, data);
        if ("receipts".equals(module)) sync.afterReceiptChanged(str(data.get("orderNo")));
        return saved;
    }

    public Map<String, Object> update(String module, Long id, Map<String, Object> data, DataScope scope) {
        checkModule(module);
        ModuleRecord old = mapper.find(module, id);
        if (old == null) throw new IllegalArgumentException("记录不存在");
        checkAccess(module, old.owner, scope);
        if (TODOS.equals(module)) data.put("owner", old.owner);
        else applyOwner(data, scope);
        save(module, id, data);
        data.put("id", id);
        if ("receipts".equals(module)) {
            // 单号被改过时，旧订单的应收也要重算，否则两边都不准
            String oldOrderNo = str(toMap(old).get("orderNo"));
            sync.afterReceiptChanged(oldOrderNo);
            String newOrderNo = str(data.get("orderNo"));
            if (newOrderNo != null && !newOrderNo.equals(oldOrderNo)) sync.afterReceiptChanged(newOrderNo);
        }
        return data;
    }

    public void delete(String module, Long id, DataScope scope) {
        checkModule(module);
        ModuleRecord row = mapper.find(module, id);
        if (row == null) throw new IllegalArgumentException("记录不存在");
        checkAccess(module, row.owner, scope);
        String receiptOrderNo = "receipts".equals(module) ? str(toMap(row).get("orderNo")) : null;
        mapper.delete(module, id);
        // 收款单删了，对应应收与订单状态要退回去
        if (receiptOrderNo != null) sync.afterReceiptChanged(receiptOrderNo);
    }

    /** 只属于当前账号的待办（含历史遗留的、记在别人名下的数据一律不返回） */
    private List<Map<String, Object>> myTodos(String module, DataScope scope) {
        String me = scope.employeeName();
        if (me == null || me.isBlank()) return List.of();
        return mapper.list(module).stream()
                .filter(row -> me.equals(row.owner == null ? null : row.owner.trim()))
                .map(this::toMap).toList();
    }

    /** 写操作的归属校验：待办只认本人，其他模块按数据范围 */
    private void checkAccess(String module, String owner, DataScope scope) {
        if (TODOS.equals(module)) {
            String me = scope.employeeName();
            if (me == null || !me.equals(owner == null ? null : owner.trim()))
                throw new ForbiddenException("只能操作自己的待办");
            return;
        }
        checkOwner(owner, scope);
    }

    private Map<String, Object> save(String module, Long id, Map<String, Object> data) {
        try {
            ModuleRecord row = new ModuleRecord();
            row.id = id;
            row.module = module;
            row.owner = data.get("owner") == null ? "" : String.valueOf(data.get("owner"));
            String customer = data.get("customer") == null ? null : String.valueOf(data.get("customer"));
            row.scopeOwner = "pool-customers".equals(module) ? "" :
                    (customer == null ? row.owner : mapper.customerOwner(customer));
            if (row.scopeOwner == null) row.scopeOwner = row.owner;
            row.payload = json.writeValueAsString(data);
            if (id == null) mapper.insert(row); else mapper.update(row);
            data.put("id", row.id);
            return data;
        } catch (Exception e) {
            throw new IllegalArgumentException("数据格式无法保存", e);
        }
    }

    private Map<String, Object> toMap(ModuleRecord row) {
        try {
            Map<String, Object> data = json.readValue(row.payload, new TypeReference<>() {});
            data.put("id", row.id);
            return data;
        } catch (Exception e) {
            throw new IllegalStateException("读取业务记录失败", e);
        }
    }

    private void applyOwner(Map<String, Object> data, DataScope scope) {
        Object owner = data.get("owner");
        if (scope.isAll()) return;
        if (owner == null || String.valueOf(owner).isBlank()) data.put("owner", scope.employeeName());
        else checkOwner(String.valueOf(owner), scope);
    }

    private void checkOwner(String owner, DataScope scope) {
        if (!scope.canManage(owner)) throw new ForbiddenException(scope.denyMessage(owner));
    }

    private String str(Object value) { return value == null ? null : String.valueOf(value); }

    private void checkModule(String module) {
        if (!MODULES.contains(module)) throw new IllegalArgumentException("未知模块");
    }
}
