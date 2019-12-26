package com.iqiyi.intl.logview.websocket;

import lombok.Data;

/**
 * @program: intl-logview
 * @description:
 * @author: yangfan
 * @create: 2019/12/20 15:33
 */

@Data
public class FilterParams {
    private String uri;
    private String bstp;
    private String t;
    private String rpage;
    private String u;
}
