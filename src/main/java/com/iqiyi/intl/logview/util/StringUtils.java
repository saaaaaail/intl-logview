package com.iqiyi.intl.logview.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by zhuxh on 16/7/26.
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {

    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static Boolean isExistNullOrEmpty(String... texts) {
        if (texts == null) return true;
        else {
            for (String obj : texts) {
                if (obj == null) return true;
                if (StringUtils.isBlank(obj)) return true;
            }
        }
        return false;
    }

    public static String parseObjargs(Object[] args) {
        if (args == null || args.length == 0)
            return "";

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null)
                continue;

            sb.append(args[i].toString());
            if (i != args.length - 1)
                sb.append(",");
        }

        return sb.toString();
    }

    public static String parseUrlArgs(Map<String, String> params) {
        String str = "";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            str = String.format("%s%s=%s&", str, entry.getKey(), entry.getValue(), "&");
        }

        if (str.length() > 0)
            str = str.substring(0, str.length() - 1);

        return str;
    }

    public static int defaultInteger(String str, Integer defaultInt) {
        if (isEmpty(str) && defaultInt == null)
            defaultInt = 0;

        int iRet;
        String ret = isEmpty(str) ? defaultInt + "" : str;
        try {
            iRet = Integer.parseInt(ret);
        } catch (Exception e) {
            return defaultInt;
        }
        return iRet;
    }

    public static String joinExcludeEmpty(String[] strArray, String separator) {
        List<String> array = new ArrayList<String>();
        for (String str : strArray) {
            if (isNotEmpty(str)) {
                array.add(str);
            }
        }
        return join(array, separator);
    }

    public static String defaultStr(String str, String defaultStr) {
        return isEmpty(str) ? defaultStr : str.trim();
    }

    public static String defaultEmptyStr(Object obj) {
        if (obj == null)
            return "";
        return defaultStr(obj.toString(), "");
    }


    public static String addUnderlineForUpperCase(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append("_");
                ch = Character.toLowerCase(ch);
            }
            result.append(ch);
        }
        return result.toString();
    }

    public static String replaceStr(String str, String replaceStr, int num) {
        if (str == null || str.length() < num) {
            return str;
        }
        String endStr = IntStream.range(0, num).mapToObj(i -> replaceStr).collect(Collectors.joining(""));
        return str.substring(0, str.length() - num) + endStr;
    }
}
