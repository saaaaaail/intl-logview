package com.iqiyi.intl.logview.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: kongwenqiang
 * Date: 2017/3/20
 * Time: 下午2:45
 * Mail: kongwenqiang@qiyi.com
 * Description:
 */
public class WebUtil {

    private static final Logger logger = LoggerFactory.getLogger(WebUtil.class);

    public static final String CHARSET = "UTF-8";

    // 签名key
    @Value(value = "${kefuSignKey}")
    private String kefuSignKey;

    // 允许调用接口的IP段
    protected static String[] allowedIps = {
            "10.",
            "192.168.",
            "127.0.0.1"
    };

    /**
     * 验证IP
     *
     * @return
     */
    public static boolean isValidIp(HttpServletRequest httpServletRequest) {
        String ip = getUserIp(httpServletRequest);
        for (String allowedIp : allowedIps) {
            if (ip.startsWith(allowedIp)) {
                return true;
            }
        }
        // 用户的ip不在允许的ip列表中
        logger.warn("[IP access denied] [ip:{}]", ip);
        return false;
    }

    /**
     * 获取用户ip
     *
     * @return
     */
    public static final String getUserIp(HttpServletRequest servletRequest) {
        String ip = servletRequest.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(ip)) {
            String[] ips = StringUtils.split(ip, ',');
            if (ips != null) {
                for (String tmpip : ips) {
                    if (StringUtils.isBlank(tmpip))
                        continue;
                    tmpip = tmpip.trim();
                    if (isIPAddr(tmpip) && !tmpip.startsWith("10.") && !tmpip.startsWith("192.168.") && !"127.0.0.1".equals(tmpip)) {
                        return tmpip.trim();
                    }
                }
            }
        }
        ip = servletRequest.getHeader("x-real-ip");
        if (isIPAddr(ip)) {
            return ip;
        }
        ip = servletRequest.getRemoteAddr();
        if (ip.indexOf('.') == -1) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    public static boolean isIPAddr(String addr) {
        if (StringUtils.isEmpty(addr))
            return false;
        String[] ips = StringUtils.split(addr, '.');
        if (ips.length != 4)
            return false;
        try {
            int ipa = Integer.parseInt(ips[0]);
            int ipb = Integer.parseInt(ips[1]);
            int ipc = Integer.parseInt(ips[2]);
            int ipd = Integer.parseInt(ips[3]);
            return ipa >= 0 && ipa <= 255 && ipb >= 0 && ipb <= 255 && ipc >= 0
                    && ipc <= 255 && ipd >= 0 && ipd <= 255;
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * 验签
     *
     * @param params
     * @return
     */
    public static final boolean validateSignWithKey(Map<String, String> params, String key) {
        return StringUtils.equals(params.get("sign"), signMessage(params, key));
    }

    /**
     * 将传入的参数集合生成签名串
     *
     * @param params 支付所需的参数
     * @return MD5加密后的签名串
     */
    public static String signMessage(Map<String, String> params, String key) {
        //把拼接后的字符串再与安全校验码 连接起来
        String toSignStr = new StringBuilder(createTextString(params)).append(key).toString();
        String md5sign = EncodeUtils.MD5(toSignStr, CHARSET);
        logger.info("[WebUtil:signMessage] [toSignStr:{}] [md5sign:{}]", toSignStr, md5sign);
        return md5sign;
    }


    /**
     * 生成签名串，去掉参数sign和sign_type
     */
    public static String createTextString(Map<String, String> params) {
        List<String> filterList = Lists.newArrayList("sign", "sign_type");
        Map<String, String> map = paraFilter(params, filterList);
        return createLinkString(map);
    }

    /**
     * 功能：除去数组中的空值和签名参数
     *
     * @param params 签名参数组
     * @return 去掉空值与签名参数后的新签名参数组
     */
    private static Map<String, String> paraFilter(Map<String, String> params, List<String> filterList) {
        Map<String, String> resultMap = Maps.newHashMap();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (StringUtils.isBlank(entry.getValue()) || filterList.contains(entry.getKey())) {
                continue;
            }
            resultMap.put(entry.getKey(), entry.getValue());
        }
        return resultMap;
    }

    /**
     * 功能：把数组所有元素排序，并按照“参数=参数值”的模式用“&”字符拼接成字符串
     *
     * @param params 需要排序并参与字符拼接的参数组
     * @return 拼接后字符串
     */
    public static String createLinkString(Map<String, String> params) {
        return createLinkStringGuava(params);
    }

    public static String createLinkStringGuava(Map<String, String> params) {
        Preconditions.checkNotNull(params, "params to sign can not be null");

        final Map<String, String> keySortedParams = Maps.newTreeMap();
        keySortedParams.putAll(params);

        return Joiner.on(DelimiterChars.AMPERSAND).withKeyValueSeparator(DelimiterChars.EQUALS).useForNull("null").join(keySortedParams);
    }

    /**
     * 把对象转为form表单需要的map格式
     *
     * @param o
     * @return
     */
    public static Map<String, String> objectToMapForm(Object o) {
        Map result = new HashMap();
        ObjectMapper m = new ObjectMapper();
        Map<String, Object> mappedObject = m.convertValue(o, Map.class);
        mappedObject.forEach((k, v) -> {
            if (v != null) {
                if (v instanceof List) {
                    ObjectMapper m1 = new ObjectMapper();
                    List list = (List) v;
                    for (int i = 0; i < list.size(); i++) {
                        Map<String, Object> objectMap = m1.convertValue(list.get(i), Map.class);
                        final int num = i;
                        objectMap.forEach((k1, v1) -> {
                            if (v1 != null) {
                                String key = String.format(k + "[%d]." + k1, num);
                                String value = v1.toString();
                                result.put(key, value);
                            }
                        });
                    }
                } else {
                    result.put(k, v.toString());
                }
            }

        });
        return result;
    }
}
