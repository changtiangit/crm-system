package com.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.demo.pojo.Employee;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
    // 封装常用查询与更新方法
    default List<Employee> selectAll() {
        return selectList(null);
    }
    default int update(Employee employee) {
        return updateById(employee);
    }
    default List<Employee> selectByName(String name) {
        return selectList(Wrappers.<Employee>lambdaQuery().like(Employee::getName, name));
    }
    // 原 XML 中的自定义 SQL 仍然可以继续使用
    List<Employee> selectByCondition(Employee employee);
}