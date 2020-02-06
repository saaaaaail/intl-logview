package com.iqiyi.intl.logview.service;

import com.iqiyi.intl.logview.mapper.TypeMapper;
import com.iqiyi.intl.logview.model.Type;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TypeService {

    @Resource
    TypeMapper typeMapper;

    public void batchInsert(List<Type> types){
        typeMapper.batchInsert(types);
    }
}
