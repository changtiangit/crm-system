package com.crm.product;

import com.crm.common.AuthInterceptor;
import com.crm.common.ForbiddenException;
import com.crm.common.PermissionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    private final PermissionService permissions;

    public ProductController(ProductService service, PermissionService permissions) {
        this.service = service;
        this.permissions = permissions;
    }

    /** 商品列表：没有「查看商品进价」权限的账号（销售组长 / 顾问）拿不到 purchasePrice */
    @GetMapping
    public List<ProductView> list(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        require(userId, "product:view");
        return service.list(permissions.has(userId, PermissionService.PRODUCT_COST));
    }

    /** 分类下拉选项（商品分类来自已有数据，新增时也可直接输入新分类） */
    @GetMapping("/categories")
    public List<String> categories(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        require(userId, "product:view");
        return service.categories();
    }

    @PostMapping
    public ProductView create(@RequestBody ProductService.ProductRequest request,
                              @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        require(userId, "product:add");
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ProductView update(@PathVariable Long id, @RequestBody ProductService.ProductRequest request,
                              @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        require(userId, "product:edit");
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id,
                                     @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        require(userId, "product:delete");
        service.delete(id);
        return Map.of("message", "商品已删除");
    }

    /** 库存增减：{ "delta": 10 } 入库 10 件，{ "delta": -3 } 出库 3 件 */
    @PatchMapping("/{id}/stock")
    public ProductView adjustStock(@PathVariable Long id, @RequestBody StockRequest body,
                                   @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        require(userId, "product:stock");
        return service.adjustStock(id, body.delta());
    }

    /** 上架 / 下架：{ "status": 0 } 下架，{ "status": 1 } 上架 */
    @PatchMapping("/{id}/status")
    public ProductView changeStatus(@PathVariable Long id, @RequestBody StatusRequest body,
                                    @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        require(userId, "product:offline");
        return service.changeStatus(id, body.status());
    }

    /** 技术 / 维修资料：售后角色单独维护 */
    @PatchMapping("/{id}/tech")
    public ProductView updateTechNote(@PathVariable Long id, @RequestBody TechRequest body,
                                      @RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        require(userId, "product:tech");
        return service.updateTechNote(id, body.techNote());
    }

    private void require(Long userId, String permission) {
        if (!permissions.has(userId, permission)) throw new ForbiddenException("当前账号没有该商品操作权限");
    }

    public record StockRequest(Integer delta) {
    }

    public record StatusRequest(Integer status) {
    }

    public record TechRequest(String techNote) {
    }
}
