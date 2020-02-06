package com.iqiyi.intl.logview.service;

import com.iqiyi.intl.logview.exception.IntlRuntimeException;
import com.iqiyi.intl.logview.mapper.CheckMapper;
import com.iqiyi.intl.logview.model.Check;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CheckService {

    @Resource
    private CheckMapper checkMapper;

    public void insert(Check check){
        int i = checkMapper.insert(check);
        if (i<=0){throw new IntlRuntimeException("insert check error"); }
    }
}
