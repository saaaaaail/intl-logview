package com.iqiyi.intl.logview.aspect;

import com.iqiyi.intl.logview.util.JSONUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Aspect
@Component
public class ApiBusinessLogAspect {

    private final static Logger BUSINESS_LOGGER = LoggerFactory.getLogger("business");

    private static List<String> ignoreMethods = new ArrayList<>(Arrays.asList());
    private static List<String> ignoreClasses = new ArrayList<>(Arrays.asList());

    @Pointcut("execution(* com.iqiyi.intl.logview.service.*Service+.*(..))")
    public void serviceMethods() {
    }

    @Pointcut("execution(* com.iqiyi.intl.logview.repository.*Repository+.*(..))")
    public void daoMethods() {
    }

    /**
     * service 环绕
     **/
    @Around("serviceMethods()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundCore(joinPoint);
    }

    @Around("daoMethods()")
    public Object aroundDao(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundCore(joinPoint);
    }

    private Object aroundCore(ProceedingJoinPoint joinPoint) throws Throwable {
        Long now = System.currentTimeMillis();

        Class targetClass = joinPoint.getTarget().getClass();
        String clzName = BusinessHelper.getShortClzName(targetClass.getName());

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method proxyMethod = methodSignature.getMethod();
        String methodName = proxyMethod.getName();
        Object[] args = joinPoint.getArgs();

        if ((!ignoreClasses.contains(clzName.trim())) && (!ignoreMethods.contains(clzName.trim() + "-" + methodName.trim()))) {
            String beforeLog = LogHelper.getBusinessBeforeLog(clzName, methodName, args);
            BUSINESS_LOGGER.info(beforeLog);
        }

        Boolean isReturnValue = false;
        String returnType = proxyMethod.getGenericReturnType().toString();
        if (!"void".equals(returnType)) {
            isReturnValue = BusinessHelper.IS_RETURN_VALUE;
        }

        Object rtv = null;
        try {
            rtv = joinPoint.proceed();
            return rtv;
        } catch (Exception e) {
            String exceptionLog = LogHelper.getExceptionLog(clzName, methodName, e);
            BUSINESS_LOGGER.error(exceptionLog);
            throw e;
        } finally {
            if ((!ignoreClasses.contains(clzName.trim())) && (!ignoreMethods.contains(clzName.trim() + "-" + methodName.trim()))) {
                Long cost = System.currentTimeMillis() - now;
                String afterLog = LogHelper.getBusinessReturnLog(clzName, methodName, isReturnValue, JSONUtil.toJSONString(rtv), cost);
                BUSINESS_LOGGER.info(afterLog);
            }
        }
    }

}
