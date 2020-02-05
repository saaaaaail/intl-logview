package com.iqiyi.intl.logview.aspect;

import java.util.Collection;
import java.util.Map;

public class BusinessHelper {

    public static final Boolean IS_RETURN_VALUE = true;

    public static int getReturnValueSize(Object rtv) {
        if (rtv == null)
            return 0;
        else if (rtv instanceof Map)
            return ((Map) rtv).size();
        else if (rtv instanceof Collection)
            return ((Collection) rtv).size();
        else
            return 1;
    }

    public static String getShortClzName(String clzFullName) {
        int cutIndex = clzFullName.lastIndexOf(".");
        return clzFullName.substring(cutIndex + 1, clzFullName.length());
    }


}
