package com.iqiyi.intl.logview.websocket;

import lombok.Data;

/**
 * @program: intl-logview
 * @description:
 * @author: yangfan
 * @create: 2019/12/17 10:51
 */

@Data
public class SocketMessage {

    private String msg;

    private String user;

    private Integer type;

    private String error;
}
