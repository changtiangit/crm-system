package com.crm.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProductMapper mapper;

    public ProductService(ProductMapper mapper) {
        this.mapper = mapper;
    }

    public record ProductRequest(
            String code,
            String name,
            String category,
            String spec,
            String unit,
            BigDecimal purchasePrice,
            BigDecimal salePrice,
            Integer stock,
            Integer status,
            String remark
    ) {
    }

    /**
     * 商品列表。canSeeCost 为 false 时不下发进价：
     * 销售只负责报价与成交，进价属于成本数据，接口层直接抹掉，避免抓包也能拿到。
     */
    @Transactional(readOnly = true)
    public List<ProductView> list(boolean canSeeCost) {
        List<ProductView> views = new ArrayList<>();
        for (ProductRow row : mapper.findAll()) {
            views.add(toView(row, canSeeCost));
        }
        return views;
    }

    /** 分类下拉用：库里已有的全部分类 */
    @Transactional(readOnly = true)
    public List<String> categories() {
        return mapper.categories();
    }

    @Transactional
    public ProductView create(ProductRequest request) {
        ProductRow row = fromRequest(request, null);
        mapper.insert(row);
        return toView(mapper.findById(row.id));
    }

    @Transactional
    public ProductView update(Long id, ProductRequest request) {
        if (mapper.findById(id) == null) throw new IllegalArgumentException("商品不存在或已被删除");
        ProductRow row = fromRequest(request, id);
        row.id = id;
        mapper.update(row);
        return toView(mapper.findById(id));
    }

    /** 库存增减：delta 正数入库、负数出库，出库不能超过现有库存 */
    @Transactional
    public ProductView adjustStock(Long id, Integer delta) {
        ProductRow row = mapper.findById(id);
        if (row == null) throw new IllegalArgumentException("商品不存在或已被删除");
        if (delta == null || delta == 0) throw new IllegalArgumentException("请输入调整数量");
        int current = row.stock == null ? 0 : row.stock;
        int next = current + delta;
        if (next < 0) throw new IllegalArgumentException("出库数量不能超过当前库存（当前 " + current + "）");
        mapper.updateStock(id, next);
        return toView(mapper.findById(id));
    }

    /** 上架 / 下架 */
    @Transactional
    public ProductView changeStatus(Long id, Integer status) {
        if (mapper.findById(id) == null) throw new IllegalArgumentException("商品不存在或已被删除");
        if (status == null || (status != 0 && status != 1)) throw new IllegalArgumentException("商品状态只能是上架或下架");
        mapper.updateStatus(id, status);
        return toView(mapper.findById(id));
    }

    /** 技术/维修资料维护 */
    @Transactional
    public ProductView updateTechNote(Long id, String techNote) {
        if (mapper.findById(id) == null) throw new IllegalArgumentException("商品不存在或已被删除");
        String note = trim(techNote);
        if (note.length() > 500) throw new IllegalArgumentException("技术/维修资料不能超过 500 个字符");
        mapper.updateTechNote(id, note);
        return toView(mapper.findById(id));
    }

    @Transactional
    public void delete(Long id) {
        if (mapper.findById(id) == null) throw new IllegalArgumentException("商品不存在或已被删除");
        mapper.delete(id);
    }

    private ProductRow fromRequest(ProductRequest request, Long excludeId) {
        if (request == null) throw new IllegalArgumentException("提交的数据为空");
        String code = trim(request.code());
        String name = trim(request.name());
        String category = trim(request.category());
        if (code.isEmpty()) throw new IllegalArgumentException("商品编码不能为空");
        if (code.length() > 32) throw new IllegalArgumentException("商品编码不能超过 32 个字符");
        if (name.isEmpty()) throw new IllegalArgumentException("商品名称不能为空");
        if (name.length() > 100) throw new IllegalArgumentException("商品名称不能超过 100 个字符");
        if (category.isEmpty()) throw new IllegalArgumentException("请选择商品分类");
        if (mapper.countByCode(code, excludeId) > 0) throw new IllegalArgumentException("商品编码 " + code + " 已存在");

        BigDecimal purchase = request.purchasePrice() == null ? BigDecimal.ZERO : request.purchasePrice();
        BigDecimal sale = request.salePrice() == null ? BigDecimal.ZERO : request.salePrice();
        if (purchase.signum() < 0 || sale.signum() < 0) throw new IllegalArgumentException("价格不能为负数");
        if (sale.compareTo(new BigDecimal("9999999.99")) > 0) throw new IllegalArgumentException("价格超出可录入范围");

        int stock = request.stock() == null ? 0 : request.stock();
        if (stock < 0) throw new IllegalArgumentException("库存不能为负数");

        ProductRow row = new ProductRow();
        row.code = code;
        row.name = name;
        row.category = category;
        row.spec = trim(request.spec());
        row.unit = trim(request.unit()).isEmpty() ? "个" : trim(request.unit());
        row.purchasePrice = purchase;
        row.salePrice = sale;
        row.stock = stock;
        row.status = request.status() == null ? 1 : request.status();
        row.remark = trim(request.remark());
        return row;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private ProductView toView(ProductRow row) {
        return toView(row, true);
    }

    private ProductView toView(ProductRow row, boolean canSeeCost) {
        return new ProductView(
                row.id,
                row.code,
                row.name,
                row.category,
                row.spec,
                row.unit,
                canSeeCost ? row.purchasePrice : null,
                row.salePrice,
                row.stock,
                row.status,
                row.remark,
                row.techNote,
                row.createdAt == null ? null : row.createdAt.format(TS)
        );
    }
}
