package com.crm.business;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BusinessMapper {
    /**
     * 线索列表。owners 为 null 表示不限制（全公司）；
     * 组长额外能看到「待分配」的公共线索，方便拉进本组。
     */
    @Select("<script>SELECT id,name,contact,phone,source,intent,status,owner,assign_time AS assignTime,assign_by AS assignBy,create_time AS createTime " +
            "FROM crm_clues " +
            "<where>" +
            "<if test='owners != null'>owner IN <foreach item='o' collection='owners' open='(' separator=',' close=')'>#{o}</foreach>" +
            "<if test='includeUnassigned'> OR owner = ''</if></if>" +
            "</where> ORDER BY create_time DESC,id DESC</script>")
    List<BusinessRow> clues(@Param("owners") List<String> owners, @Param("includeUnassigned") boolean includeUnassigned);

    @Insert("INSERT INTO crm_clues(name,contact,phone,source,intent,status,owner,assign_time,assign_by) " +
            "VALUES(#{name},#{contact},#{phone},#{source},#{intent},#{status},#{owner},IF(#{owner}='',NULL,NOW()),'')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertClue(BusinessRow row);

    @Update("UPDATE crm_clues SET name=#{name},contact=#{contact},phone=#{phone},source=#{source},intent=#{intent},status=#{status},owner=#{owner} WHERE id=#{id}")
    int updateClue(BusinessRow row);

    @Delete("DELETE FROM crm_clues WHERE id=#{id}")
    int deleteClue(Long id);

    @Select("<script>SELECT id,name,industry,level,contact,phone,owner,create_time AS createTime FROM crm_customers " +
            "<where><if test='owners != null'>owner IN <foreach item='o' collection='owners' open='(' separator=',' close=')'>#{o}</foreach></if></where> " +
            "ORDER BY create_time DESC,id DESC</script>")
    List<BusinessRow> customers(@Param("owners") List<String> owners);

    @Insert("INSERT INTO crm_customers(name,industry,level,contact,phone,owner) VALUES(#{name},#{industry},#{level},#{contact},#{phone},#{owner})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCustomer(BusinessRow row);

    @Update("UPDATE crm_customers SET name=#{name},industry=#{industry},level=#{level},contact=#{contact},phone=#{phone},owner=#{owner} WHERE id=#{id}")
    int updateCustomer(BusinessRow row);

    @Delete("DELETE FROM crm_customers WHERE id=#{id}")
    int deleteCustomer(Long id);

    @Select("<script>SELECT id,name,customer,amount,stage,expect_date AS expectDate,owner FROM crm_opportunities " +
            "<where><if test='owners != null'>owner IN <foreach item='o' collection='owners' open='(' separator=',' close=')'>#{o}</foreach></if></where> " +
            "ORDER BY id DESC</script>")
    List<BusinessRow> opportunities(@Param("owners") List<String> owners);

    @Insert("INSERT INTO crm_opportunities(name,customer,amount,stage,expect_date,owner) VALUES(#{name},#{customer},#{amount},#{stage},#{expectDate},#{owner})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOpportunity(BusinessRow row);

    @Update("UPDATE crm_opportunities SET name=#{name},customer=#{customer},amount=#{amount},stage=#{stage},expect_date=#{expectDate},owner=#{owner} WHERE id=#{id}")
    int updateOpportunity(BusinessRow row);

    @Delete("DELETE FROM crm_opportunities WHERE id=#{id}")
    int deleteOpportunity(Long id);

    @Select("<script>SELECT id,order_no AS orderNo,customer,contact,phone,amount,status,order_date AS orderDate,owner FROM crm_orders " +
            "<where><if test='owners != null'>owner IN <foreach item='o' collection='owners' open='(' separator=',' close=')'>#{o}</foreach></if></where> " +
            "ORDER BY order_date DESC,id DESC</script>")
    List<BusinessRow> orders(@Param("owners") List<String> owners);

    @Insert("INSERT INTO crm_orders(order_no,customer,contact,phone,amount,status,order_date,owner) VALUES(#{orderNo},#{customer},#{contact},#{phone},#{amount},#{status},#{orderDate},#{owner})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(BusinessRow row);

    @Update("UPDATE crm_orders SET order_no=#{orderNo},customer=#{customer},contact=#{contact},phone=#{phone},amount=#{amount},status=#{status},order_date=#{orderDate},owner=#{owner} WHERE id=#{id}")
    int updateOrder(BusinessRow row);

    @Delete("DELETE FROM crm_orders WHERE id=#{id}")
    int deleteOrder(Long id);

    /** 删除订单前先拿到单号，明细是按单号挂的 */
    @Select("SELECT order_no FROM crm_orders WHERE id=#{id}")
    String orderNoOf(Long id);

    @Select("SELECT status FROM crm_orders WHERE order_no=#{orderNo}")
    String orderStatusByNo(@Param("orderNo") String orderNo);

    @Update("UPDATE crm_orders SET status=#{status} WHERE order_no=#{orderNo}")
    int updateOrderStatusByNo(@Param("orderNo") String orderNo, @Param("status") String status);

    /**
     * 下单扣库存。库存不足时按 0 兜底而不是报错：
     * 销售现场常先接单再补货，生产看板用 0 库存来提示缺货更贴近实际。
     */
    @Update("UPDATE products SET stock=GREATEST(stock-#{qty},0) WHERE name=#{product}")
    int reduceStock(@Param("product") String product, @Param("qty") int qty);

    @Delete("DELETE FROM crm_order_items WHERE order_no=#{orderNo}")
    int deleteOrderItems(@Param("orderNo") String orderNo);

    @Insert("INSERT INTO crm_order_items(order_no,product,category,qty,price,cost,amount,sort_no) " +
            "VALUES(#{orderNo},#{product},#{category},#{qty},#{price},#{cost},#{amount},#{sortNo})")
    int insertOrderItem(OrderItemRow item);

    /** 订单明细一次性查回来，在 Service 里按单号归组，避免逐单查询 */
    @Select("SELECT id,order_no AS orderNo,product,category,qty,price,cost,amount,sort_no AS sortNo " +
            "FROM crm_order_items ORDER BY order_no,sort_no,id")
    List<OrderItemRow> orderItems();

    /**
     * 商品进价对照表：销售没有成本字段权限，录单时前端拿不到进价，
     * 由后端按商品名补齐明细成本，保证管理端的毛利口径不受影响。
     */
    @Select("SELECT name AS product, purchase_price AS cost FROM products")
    List<OrderItemRow> productCosts();

    // ---------------- 归属校验：改/删之前先确认这条数据在不在当前人的范围里 ----------------

    @Select("SELECT owner FROM crm_clues WHERE id=#{id}")
    String clueOwner(@Param("id") Long id);

    @Select("SELECT owner FROM crm_customers WHERE id=#{id}")
    String customerOwner(@Param("id") Long id);

    @Select("SELECT owner FROM crm_opportunities WHERE id=#{id}")
    String opportunityOwner(@Param("id") Long id);

    @Select("SELECT owner FROM crm_orders WHERE id=#{id}")
    String orderOwner(@Param("id") Long id);
}
