package com.crm.sales;

import com.crm.common.AuthInterceptor;
import com.crm.common.ForbiddenException;
import com.crm.common.PermissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 销售团队与线索分配 */
@RestController
@RequestMapping("/api/sales")
public class SalesController {
    private final SalesService service;
    private final PermissionService permissions;

    public SalesController(SalesService service, PermissionService permissions) {
        this.service = service;
        this.permissions = permissions;
    }

    /** 团队视图按当前账号的数据范围收敛：总监看全部，组长看本组，销售看自己 */
    @GetMapping("/team")
    public Map<String, Object> team(@RequestAttribute(AuthInterceptor.USER_ID) Long operatorId) {
        require(operatorId, "sales:team:view");
        return service.team(operatorId);
    }

    @PostMapping("/team")
    public Map<String, Object> createMember(@RequestBody SalesService.MemberRequest request,
                                            @RequestAttribute(AuthInterceptor.USER_ID) Long operatorId) {
        require(operatorId, "sales:team:edit");
        return service.createMember(request, operatorId);
    }

    @PutMapping("/team/{id}")
    public SalesMemberRow updateMember(@PathVariable Long id,
                                       @RequestBody SalesService.MemberRequest request,
                                       @RequestAttribute(AuthInterceptor.USER_ID) Long operatorId) {
        require(operatorId, "sales:team:edit");
        return service.updateMember(id, request, operatorId);
    }

    /** 手动分配 / 转派：只能分给本组成员 */
    @PostMapping("/clues/assign")
    public Map<String, Object> assign(@RequestBody SalesService.AssignRequest request,
                                      @RequestAttribute(AuthInterceptor.USER_ID) Long operatorId) {
        require(operatorId, "clue:assign");
        return service.assign(request, operatorId);
    }

    /** 按能力与剩余容量智能分配：候选池同样限定在本组 */
    @PostMapping("/clues/auto-assign")
    public Map<String, Object> autoAssign(@RequestBody SalesService.AutoAssignRequest request,
                                          @RequestAttribute(AuthInterceptor.USER_ID) Long operatorId) {
        require(operatorId, "clue:assign");
        return service.autoAssign(request, operatorId);
    }

    private void require(Long userId, String code) {
        if (!permissions.has(userId, code)) throw new ForbiddenException("当前账号没有该销售操作权限");
    }
}
