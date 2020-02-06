package com.iqiyi.intl.logview.controller;

import com.alibaba.fastjson.JSONObject;
import com.iqiyi.intl.logview.base.Result;
import com.iqiyi.intl.logview.dto.CheckParam;
import com.iqiyi.intl.logview.dto.TypeParam;
import com.iqiyi.intl.logview.exception.IntlRuntimeException;
import com.iqiyi.intl.logview.model.Check;
import com.iqiyi.intl.logview.model.Rule;
import com.iqiyi.intl.logview.model.Type;
import com.iqiyi.intl.logview.service.CheckService;
import com.iqiyi.intl.logview.service.RuleService;
import com.iqiyi.intl.logview.service.RulesService;
import com.iqiyi.intl.logview.service.TypeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class RulesController {

    @Resource
    RulesService rulesService;

    @Resource
    CheckService checkService;

    @Resource
    RuleService ruleService;

    @Resource
    TypeService typeService;

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
