package com.iqiyi.intl.logview.aspect;


import com.alibaba.fastjson.JSONObject;
import com.iqiyi.intl.logview.base.Result;
import com.iqiyi.intl.logview.util.JSONUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.UUID;

@Aspect
@Component
public class ApiCommonLogAspect {

    private static final Logger API_LOGGER = LoggerFactory.getLogger("api");
    private final String[] jsonpQueryParamNames = new String[]{"callback"};

    private static final String REQUEST_KEY = "request_id";

    @Pointcut(value = "execution(* com.iqiyi.intl.logview.controller.*.*(..))")
    public void apiMethods() {
    }

    @Around("apiMethods() && (args(webResult,..))")
    public void around(ProceedingJoinPoint joinPoint, Result<JSONObject> webResult) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        Signature oriSignature = joinPoint.getSignature();
        long start = System.currentTimeMillis();
        String businessId = getUUID();
        MDC.put(REQUEST_KEY, businessId);

        webResult.setRequestId(businessId);
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((CodeSignature) oriSignature).getParameterNames();
        String paraInfo = LogHelper.buildParaInfo(paramNames, args);
        String rst = null;
        boolean isException = false;
        try {
            joinPoint.proceed();
            rst = JSONUtil.toJSONString(webResult);
            rst = jsonpConvert(rst, request, response);
            response.setStatus(webResult.getHttpStatus());
            PrintWriter writer = response.getWriter();
            writer.write(rst);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            isException = true;
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            String info = LogHelper.getLog(request, oriSignature.getDeclaringTypeName() + "." + oriSignature.getName(), paraInfo, rst, cost);
            API_LOGGER.info(info);
            if (!isException) {
                MDC.remove(REQUEST_KEY);
            }
        }
    }

    private String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    //拼接jsonp格式，设置response的contenttype类型
    private String jsonpConvert(String rst, HttpServletRequest request, HttpServletResponse response) {

        StringBuilder stringBuilder = new StringBuilder();
        //如果不存在callback这个请求参数，直接返回，不需要处理为jsonp
        if (ObjectUtils.isEmpty(request.getParameter("callback"))) {
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            return rst;
        } else {
            response.setContentType("application/javascript;charset=UTF-8");
            //按设定的请求参数(JsonAdvice构造方法中的this.jsonpQueryParamNames = new String[]{"callback"};)，处理返回结果为jsonp格式
            for (String name : this.jsonpQueryParamNames) {
                String value = request.getParameter(name);
                if (value != null) {
                    stringBuilder.append("/**/").append(value).append("(").append(rst).append(")");
                    return stringBuilder.toString();
                }
            }
        }
        return rst;
    }
}
