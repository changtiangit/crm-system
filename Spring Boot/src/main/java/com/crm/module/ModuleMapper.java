package com.crm.module;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ModuleMapper {
    @Select("SELECT id,module_name AS module,owner,scope_owner AS scopeOwner,payload FROM crm_module_records WHERE module_name=#{module} ORDER BY id")
    List<ModuleRecord> list(String module);

    @Select("SELECT id,module_name AS module,owner,scope_owner AS scopeOwner,payload FROM crm_module_records WHERE module_name=#{module} AND id=#{id}")
    ModuleRecord find(@Param("module") String module, @Param("id") Long id);

    @Insert("INSERT INTO crm_module_records(module_name,owner,scope_owner,payload) VALUES(#{module},#{owner},#{scopeOwner},CAST(#{payload} AS JSON))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ModuleRecord row);

    @Update("UPDATE crm_module_records SET owner=#{owner},scope_owner=#{scopeOwner},payload=CAST(#{payload} AS JSON) WHERE module_name=#{module} AND id=#{id}")
    int update(ModuleRecord row);

    @Delete("DELETE FROM crm_module_records WHERE module_name=#{module} AND id=#{id}")
    int delete(@Param("module") String module, @Param("id") Long id);

    @Select("SELECT COUNT(*) FROM crm_module_records WHERE module_name=#{module}")
    int count(String module);

    @Select("SELECT COUNT(*) FROM crm_module_seed_state WHERE module_name=#{module}")
    int isSeeded(String module);

    @Insert("INSERT IGNORE INTO crm_module_seed_state(module_name) VALUES(#{module})")
    int markSeeded(String module);

    @Select("SELECT owner FROM crm_customers WHERE name=#{customer} LIMIT 1")
    String customerOwner(String customer);
}
