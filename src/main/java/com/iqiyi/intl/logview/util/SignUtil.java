package com.iqiyi.intl.logview.util;

import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author yangli
 * @date 2019/9/6
 */
public class SignUtil {

    @SuppressWarnings("unchecked")
    public static String sign(HttpServletRequest request, String secretKey) {
        Map<String, String> params = new HashMap<String, String>();
        for (Enumeration<String> en = request.getParameterNames(); en.hasMoreElements(); ) {
            String key = en.nextElement();
            params.put(key, request.getParameter(key));
        }
        return sign(params, secretKey);
    }

    public static String sign(Map<String, String> params, String secretKey) {
        SortedMap<String, String> sortedParams = new TreeMap<String, String>(params);
        sortedParams.remove("sign");
        StringBuilder sb = new StringBuilder();
        for (String key : sortedParams.keySet()) {
            String val = sortedParams.get(key);
            sb.append(key).append("=").append(StringUtils.defaultIfEmpty(val, "")).append("|");
        }
        return md5(sb.append(secretKey).toString());
    }

    public static final String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes());
    }
}
