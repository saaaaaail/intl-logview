package com.iqiyi.intl.logview.mapper;

import com.iqiyi.intl.logview.model.Rule;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleMapper {

    @Insert({"insert into `logview_rule`(`check_id`) values" +
            "(#{rule.checkId})"})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(@Param("rule") Rule rule);
}
