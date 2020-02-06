package com.iqiyi.intl.logview.mapper;

import com.iqiyi.intl.logview.model.Check;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckMapper {

    @Insert({"insert into `logview_check`(`username`,`url`,`url_match`,`use_reg`,`reg`)" +
            " values(#{check.userName},#{check.url},#{check.urlMatch},#{check.useReg},#{check.reg})"})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(@Param("check") Check check);
}
