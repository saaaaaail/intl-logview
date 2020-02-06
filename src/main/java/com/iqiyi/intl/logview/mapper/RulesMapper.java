package com.iqiyi.intl.logview.mapper;

import com.iqiyi.intl.logview.model.Rules;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

@Repository
public interface RulesMapper {

    @Insert({"insert into `logview_rules`(`username`,`check_str`) values (#{userName},#{checkStr})"})
    int insert(@Param("userName") String userName,@Param("checkStr") String checkStr);

    @Update({"update `logview_rules` set `check_str`=#{checkStr} where `username`=#{userName}"})
    int update(@Param("userName") String userName,@Param("checkStr") String checkStr);

    @Select({"select * from `logview_rules` where `username`=#{userName}"})
    @Results({@Result(property = "id",column = "id"),
            @Result(property = "userName",column = "username"),
            @Result(property = "checkStr",column = "check_str")
            })
    Rules selectByUsername(@Param("userName") String userName);
}
