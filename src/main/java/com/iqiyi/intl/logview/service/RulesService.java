package com.iqiyi.intl.logview.service;

import com.iqiyi.intl.logview.mapper.RulesMapper;
import com.iqiyi.intl.logview.exception.IntlRuntimeException;
import com.iqiyi.intl.logview.model.Rules;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class RulesService {

    @Resource
    RulesMapper rulesMapper;

    public void insert(String userName,String checkStr){
        int i = rulesMapper.insert(userName, checkStr);
        if (i<=0){
            throw new IntlRuntimeException("插入校验规则失败");
        }
    }

    public String selectByUserName(String userName){
        Rules rules = rulesMapper.selectByUsername(userName);

        if (rules!=null&& StringUtils.isNotEmpty(rules.getCheckStr())){
            return rules.getCheckStr();
        }
        return null;
    }

    public void update(String userName,String checkStr){
        int i = rulesMapper.update(userName, checkStr);
        if (i<=0){
            throw new IntlRuntimeException("更新校验规则失败");
        }
    }
}
