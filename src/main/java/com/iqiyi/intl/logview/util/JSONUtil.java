package com.iqiyi.intl.logview.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class JSONUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONUtil.class);

    private JSONUtil() {

    }

    public static String toJSONString(Object obj) {
        try {
            return JSONObject.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullListAsEmpty);
        } catch (Exception e) {
            LOGGER.error("toJSONString internal error happened", e);
        }
        return null;
    }

    public static JSONObject readToJSON(URL url) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(url, JSONObject.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public static JSONObject toJSONObject(Object javaObject) {
        return (JSONObject) JSONObject.toJSON(javaObject);
    }

    public static JSONArray toJSONArray(Object javaObject) {
        return (JSONArray) JSONObject.toJSON(javaObject);
    }
}
