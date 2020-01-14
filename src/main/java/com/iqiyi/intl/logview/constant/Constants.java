package com.iqiyi.intl.logview.constant;

/**
 * @program: intl-logview
 * @description:
 * @author: yangfan
 * @create: 2019/12/17 11:23
 */

public class Constants {
    //10.3.33.72
    public static final String SRC_TARGET_IP = "10.5.171.26";
    //accesslog.txt
    //access.log
    public static final String WATCH_FILE = "accesslog.txt";

    ///usr/local/nginx/logs/logview/
    public static final String WATCH_FILE_CATEGORY = "E:/test/";
    ///usr/local/nginx/logs/logview/access.log
    //E:/test/accesslog.txt
    public static final String WATCH_FILE_PATH = "E:/test/accesslog.txt";

    public static final String WATCH_SERVICE_KEY = "watch_service_";

    public static final String FILTER_PARAMS_KEY = "filter_params_";

    public static final String SCHEDULED_POOL = "scheduled_pool_";

    public static final String THREAD_POOL = "thread_pool_";

    public static final String commonParams = "bstp,t,hu,ce,de,dfp,v,p1,u,pu,stime,rn,mod,lang,mkey,ntwk";

    public static final String t_20_params = "t,c1,rpage,block,position,qpid,rseat,rank,r_area,bkt,e,ext";

    public static final String t_21_params = "t,c1,rpage,block,position,itemlist,rank,r_area,bkt,e,ext";
}
