package com.iqiyi.intl.logview.mapper;

import com.iqiyi.intl.logview.model.Type;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TypeMapper {

    @Insert({"<script>",
            "insert into `logview_type`(`rule_id`,`type`,`value`,`empty`,`match`,`use_reg`,`reg`) values",
            "<foreach collection='types' item='type' index='index' separator=','>",
            "(#{type.ruleId},#{type.type},#{type.value},#{type.empty},#{type.match},#{type.useReg},#{type.reg})",
            "</foreach>",
            "</script>"})
    int batchInsert(@Param("types") List<Type> types);
}
