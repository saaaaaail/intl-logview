package com.iqiyi.intl.logview.websocket;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @program: intl-logview
 * @description:
 * @author: yangfan
 * @create: 2019/12/17 10:51
 */

@Data
public class SocketMessage {

    private String msg;

    private String ip;

    private Long time;

    private String url;

    private String method;

    private JSONObject params;

    private String groupId;

    private Integer type;

    private List<Map<String,String>> error;

}
