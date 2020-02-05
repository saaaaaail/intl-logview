package com.iqiyi.intl.logview.util;

import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by zouran on 2019/10/09 19:48
 */
public class CorsUtils {

    public static void addCorsHeader(HttpServletRequest request, MultiValueMap<String, String> headers) {
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Credentials", "true");
        headers.add("Access-Control-Allow-Origin", request.getHeader("origin"));
        headers.add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, P00001");
    }
}
