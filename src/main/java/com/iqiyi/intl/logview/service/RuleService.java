package com.iqiyi.intl.logview.service;

import com.iqiyi.intl.logview.exception.IntlRuntimeException;
import com.iqiyi.intl.logview.mapper.RuleMapper;
import com.iqiyi.intl.logview.model.Rule;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RuleService {

    @Resource
    RuleMapper ruleMapper;

    public void insert(Rule rule){
        int i = ruleMapper.insert(rule);
        if (i<=0){
            throw new IntlRuntimeException("insert rule error");
        }
    }
}
