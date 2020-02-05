package com.iqiyi.intl.logview.aspect;


import com.iqiyi.intl.logview.base.Result;
import com.iqiyi.intl.logview.util.JSONUtil;

import javax.servlet.http.HttpServletRequest;

public class LogHelper {

    public static final String LOG_SPLIT = " | ";

    private static final String ERROR_PREFIX = " THROWS EXCEPTION ";

    private static final String METHOD_PREFIX = "METHOD START";

    private static final String METHOD_END = "METHOD END";

    private static final String METHOD_RETURN = "METHOD RETURN";

    private static final String CACHE = "CACHE SIZE";

    public static String getLog(HttpServletRequest request, String signature, String reqPara, String respString, long costTime) {
        String ip = (request.getHeader("X-Real-IP") != null) ? request
                .getHeader("X-Real-IP") : request.getRemoteAddr();
        String userAgent = request.getHeader("USER-AGENT");
        return getLogString(ip, request.getRequestURI(), signature, reqPara, respString, userAgent, costTime);
    }

    public static String getLogString(String ip, String action, String signature,
                                      String reqPara, String respString, String userAgent, long costTime) {
        StringBuffer sb = new StringBuffer();
        sb.append(ip).append(LOG_SPLIT).append(userAgent).append(LOG_SPLIT).append(action).append(LOG_SPLIT)
                .append(signature).append(LOG_SPLIT).append(reqPara).append(LOG_SPLIT).append(respString).append(LOG_SPLIT).append(costTime);
        return sb.toString();
    }

    public static String getBusinessBeforeLog(String className, String methodName, Object[] args) {
        StringBuffer sb = getBussinessCommon(className, methodName);
        sb.append(METHOD_PREFIX).append(LOG_SPLIT);
        sb.append(JSONUtil.toJSONString(args));
        return sb.toString();
    }

    public static String getBusinessReturnLog(String className, String methodName, Boolean isReturnValue, String rtv, long costTime) {
        StringBuffer sb = getBussinessCommon(className, methodName);

        if (isReturnValue) {
            sb.append(METHOD_RETURN).append(LOG_SPLIT);
            sb.append(rtv).append(LOG_SPLIT);
        } else
            sb.append(METHOD_END).append(LOG_SPLIT);

        sb.append(costTime);
        return sb.toString();
    }

    public static String getRedisCacheLog(String className, String methodName, Integer paramSize) {
        StringBuffer sb = getBussinessCommon(className, methodName);
        sb.append(CACHE).append(LOG_SPLIT);
        sb.append(paramSize);
        return sb.toString();
    }

    private static StringBuffer getBussinessCommon(String className, String methodName) {
        StringBuffer sb = new StringBuffer();
        sb.append(className).append(LOG_SPLIT);
        sb.append(methodName).append(LOG_SPLIT);
        return sb;
    }

    public static String getExceptionLog(String className, String methodName, Exception e) {
        StringBuffer sb = getBussinessCommon(className, methodName);
        sb.append(ERROR_PREFIX).append(LOG_SPLIT);
        sb.append(e.getMessage());
        return sb.toString();
    }

    public static String buildParaInfo(String[] paramNames, Object[] args) {
        StringBuilder para = new StringBuilder();
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if ((args[i] != null) && (!isResultPara(args[i]))) {
                    para.append(paramNames[i]).append("=").append(args[i].toString()).append(",");
                } else if (!isResultPara(args[i])) {
                    para.append(paramNames[i]).append("=").append("null").append(",");
                }
            }
            if (para.length() > 0) {
                para.deleteCharAt(para.length() - 1);
            }
        }
        return para.toString();
    }

    private static boolean isResultPara(Object o) {
        return o instanceof Result;
    }
}
