package com.crm.function;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FunctionMapper {
    @Select("SELECT code, name, module FROM permissions ORDER BY sort_order, id")
    List<FunctionView> findAll();
}
