package com.crm.product;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper {
    String COLUMNS = "id, code, name, category, spec, unit, purchase_price, sale_price, stock, status, remark, tech_note, created_at";

    @Select("SELECT " + COLUMNS + " FROM products ORDER BY code")
    List<ProductRow> findAll();

    @Select("SELECT " + COLUMNS + " FROM products WHERE id = #{id}")
    ProductRow findById(@Param("id") Long id);

    /** 商品编码唯一性校验，excludeId 用于编辑时排除自身 */
    @Select("SELECT COUNT(*) FROM products WHERE code = #{code} AND id <> IFNULL(#{excludeId}, 0)")
    int countByCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    @Select("SELECT DISTINCT category FROM products ORDER BY category")
    List<String> categories();

    @Insert("INSERT INTO products(code, name, category, spec, unit, purchase_price, sale_price, stock, status, remark) " +
            "VALUES(#{code}, #{name}, #{category}, #{spec}, #{unit}, #{purchasePrice}, #{salePrice}, #{stock}, #{status}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductRow row);

    @Update("UPDATE products SET code=#{code}, name=#{name}, category=#{category}, spec=#{spec}, unit=#{unit}, " +
            "purchase_price=#{purchasePrice}, sale_price=#{salePrice}, stock=#{stock}, status=#{status}, remark=#{remark} " +
            "WHERE id=#{id}")
    int update(ProductRow row);

    @Delete("DELETE FROM products WHERE id = #{id}")
    int delete(@Param("id") Long id);

    /** 库存增减：由入库/出库量算好新库存后直接写回 */
    @Update("UPDATE products SET stock = #{stock} WHERE id = #{id}")
    int updateStock(@Param("id") Long id, @Param("stock") Integer stock);

    /** 上下架：1 上架 / 0 下架 */
    @Update("UPDATE products SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 技术/维修资料：售后角色单独维护，不影响商品其它字段 */
    @Update("UPDATE products SET tech_note = #{techNote} WHERE id = #{id}")
    int updateTechNote(@Param("id") Long id, @Param("techNote") String techNote);
}
