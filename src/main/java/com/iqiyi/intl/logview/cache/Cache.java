package com.iqiyi.intl.logview.cache;

import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: intl-logview
 * @description:
 * @author: yangfan
 * @create: 2019/12/17 20:30
 */

@Component("localCache")
public class Cache {

    public static final Map<String,Object> map = new ConcurrentHashMap<>();

    public void add(String key,Object value){
        map.put(key,value);
    }

    public Object get(String key){
        return map.get(key);
    }

    public void remove(String key){
        map.remove(key);
    }

    public void clear(){map.clear();}
}
