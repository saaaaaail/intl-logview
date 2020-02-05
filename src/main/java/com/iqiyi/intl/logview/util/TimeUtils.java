package com.iqiyi.intl.logview.util;

/**
 * @author zyk
 * @date 2019/09/17
 **/
public class TimeUtils {

    /**
     * 转换毫秒为"mm:ss"字符串，到秒
     *
     * @param time
     * @return
     */
    public static String convertTimeMillis2String(Long time) {
        StringBuilder stringBuilder = new StringBuilder();
        long mm = time / (1000 * 60);
        if (mm < 10) {
            stringBuilder.append("0");
        }
        stringBuilder.append(mm);
        stringBuilder.append(":");
        long ss = (time % (1000 * 60)) / 1000;
        if (ss < 10) {
            stringBuilder.append("0");
        }
        stringBuilder.append(ss);
        return stringBuilder.toString();
    }


    /**
     * 转换毫秒为"mm:ss:SSS"字符串,到毫秒
     *
     * @param time
     * @return
     */
    public static String convertTimeMillis2StringWithMillisecond(Long time) {
        StringBuilder stringBuilder = new StringBuilder();
        long mm = time / (1000 * 60);
        if (mm < 10) {
            stringBuilder.append("0");
        }
        stringBuilder.append(mm);
        stringBuilder.append(":");
        long ss = (time % (1000 * 60)) / 1000;
        if (ss < 10) {
            stringBuilder.append("0");
        }
        stringBuilder.append(ss);
        stringBuilder.append(":");

        long sss = (time % (1000 * 60)) % 1000;
        if (sss < 10) {
            stringBuilder.append("00");
        } else if (sss < 100) {
            stringBuilder.append("0");
        }
        stringBuilder.append(sss);

        return stringBuilder.toString();
    }
}
