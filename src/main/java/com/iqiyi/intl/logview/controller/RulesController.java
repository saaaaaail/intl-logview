package com.iqiyi.intl.logview.controller;

import com.alibaba.fastjson.JSONObject;
import com.iqiyi.intl.logview.base.Result;
import com.iqiyi.intl.logview.dto.CheckParam;
import com.iqiyi.intl.logview.exception.IntlRuntimeException;
import com.iqiyi.intl.logview.service.RulesService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@CrossOrigin
public class RulesController {

    @Resource
    RulesService rulesService;

    @RequestMapping(value = "rules-config",method = RequestMethod.POST)
    public void rulesConfig(Result<String> result,@RequestBody CheckParam checkParam){
        if (StringUtils.isEmpty(checkParam.getUserName())){
            throw new IntlRuntimeException("userName为空！");
        }
        String checkStr = JSONObject.toJSONString(checkParam);
        String s = rulesService.selectByUserName(checkParam.getUserName());
        if (StringUtils.isNotEmpty(s)){
            rulesService.update(checkParam.getUserName(),checkStr);
        }else {
            rulesService.insert(checkParam.getUserName(),checkStr);
        }
        result.setSuccessResult(checkParam.getUserName());
    }
}
