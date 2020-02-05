package com.iqiyi.intl.logview.watch;

import com.alibaba.fastjson.JSONObject;
import com.iqiyi.intl.logview.cache.Cache;
import com.iqiyi.intl.logview.constant.Constants;
import com.iqiyi.intl.logview.enums.TypeEnums;
import com.iqiyi.intl.logview.websocket.FilterParams;
import com.iqiyi.intl.logview.websocket.SocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.websocket.Session;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @program: intl-logview
 * @description: 采用定时线程实时监听文件变化
 * @author: yangfan
 * @create: 2019/12/17 11:16
 */

@Service
@Slf4j
public class PoolMonitorService {

    @Autowired
    @Qualifier("localCache")
    private Cache cache;

    private Long pointer = 0L;

    private boolean isPause = false;

    public void readFileScheduledStart(Session host, Map<Session, SocketMessage> sessionMap,SocketMessage socketMessage) {
        log.info("校验是否第一次读取文件");
        String poolKey = Constants.SCHEDULED_POOL+host.getId();
        synchronized (this){

            Object o = cache.get(poolKey);
            if (o!=null){
                log.info("非第一次读取文件");
                return;
            }

            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
            cache.add(poolKey,scheduledExecutorService);
        }

        readFileScheduled(host,sessionMap,socketMessage);
    }

    public void readFileScheduledWithFilter(Session host, Map<Session, SocketMessage> sessionMap,SocketMessage socketMessage){
        String poolKey = Constants.SCHEDULED_POOL + host.getId();
        synchronized (this){
            if (cache.get(poolKey)!=null){
                closePool(host);
            }
            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
            cache.add(poolKey,scheduledExecutorService);
        }
        //清屏
        clearMsg(host);
        readFileScheduled(host,sessionMap,socketMessage);
    }

    private void readFileScheduled(Session host, Map<Session, SocketMessage> sessionMap,SocketMessage socketMessage){
        String poolKey = Constants.SCHEDULED_POOL + host.getId();
        FilterParams params = socketMessage.getParams();
        String pattern = socketMessage.getPattern();

        //读历史文件
        pointer = firstReadFile(host,params,pattern);
        //筛选按钮可用
        enableFilterBtnMsg(host);

        log.info("开始读取文件");
        ((ScheduledExecutorService)cache.get(poolKey)).scheduleWithFixedDelay(() -> {
            if (!isPause){
                RandomAccessFile file = null;
                List<String> result = new ArrayList<>();
                try {
                    file = new RandomAccessFile(Constants.WATCH_FILE_PATH,"r");

                    file.seek(pointer);
                    String msgline1 = null;
                    while ((msgline1 = file.readLine()) != null) {
                        if (StringUtils.isNotEmpty(msgline1)){
                            String msg = new String(msgline1.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
                            result.add(msg);
                        }
                    }
                    pointer=file.getFilePointer();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    try {
                        if (file!=null){file.close();}
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                for (String msg:result){
                    sendMessageToAll(sessionMap, msg,params,pattern);
                }
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    public void pauseThread(Session host) throws IOException {
        String poolKey = Constants.SCHEDULED_POOL + host.getId();
        Object o = cache.get(poolKey);
        if (o==null){return;}
        SocketMessage socketMessage = new SocketMessage();
        socketMessage.setType(TypeEnums.PAUSE_OPERATE.getCode());
        if (isPause){
            socketMessage.setMsg("0");
            isPause =false;
        }else {
            socketMessage.setMsg("1");
            isPause =true;
        }
        host.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
    }

    public void closePool(Session session){
        String poolKey = Constants.SCHEDULED_POOL + session.getId();
        Object o1 = cache.get(poolKey);
        if (o1!=null){
            isPause=false;
            cache.remove(poolKey);
            ((ScheduledExecutorService)o1).shutdown();
            log.info("关闭用户的线程池");
        }
    }

    public void sendMessageToAll(Map<Session, SocketMessage> sessionMap, String msg,FilterParams params,String pattern){
        if (filterMessage(msg,params,pattern)){
            SocketMessage socketMessage = parseMessage(msg);
            sessionMap.forEach((session, message) -> {
                try {
                    session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void sendMessage(Session session, String msg,FilterParams params,String pattern){
        if (filterMessage(msg,params,pattern)){
            SocketMessage socketMessage = parseMessage(msg);
            try {
                session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void clearMsg(Session session){
        SocketMessage socketMessage = generateMsg(null,null,TypeEnums.CLEAR_MSG_OPERATE.getCode());
        try {
            session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void enableFilterBtnMsg(Session session){
        SocketMessage socketMessage = generateMsg(null,null,TypeEnums.ENABLE_FILTER_BTN_OPERATE.getCode());
        try {
            session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 首先读取历史记录
     * @param session
     * @return
     */
    private Long firstReadFile(Session session,FilterParams params,String pattern){
        RandomAccessFile file = null;
        List<String> result = new ArrayList<>();
        Long point = 0L;
        try {
            file = new RandomAccessFile(Constants.WATCH_FILE_PATH,"r");
            Long fileLength = file.length();
            point = fileLength-1;
            int tmp =-1;
            while (point>=0){
                tmp = file.read();
                if (tmp == '\n'){
                    String msg = null;
                    if ((msg = file.readLine())!=null){
                        String msgline = new String(msg.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
                        if (StringUtils.isNotEmpty(msgline)) {
                            result.add(msgline);
                        }

                    }
                }
                if (point==0){
                    file.seek(0L);
                    String msg = null;
                    if ((msg = file.readLine())!=null){
                        String msgline = new String(msg.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
                        if (StringUtils.isNotEmpty(msgline)){
                            result.add(msgline);
                            //log.info(msgline);

                        }
                    }
                    point--;
                }else {
                    point--;
                    file.seek(point);
                }
                if (result.size()>=300){
                    break;
                }
            }

            for (int i=result.size()-1;i>=0;i--){
                sendMessage(session,result.get(i),params,pattern);
                //log.info(result.get(i));
            }
            return fileLength;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (file!=null){
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }


    private boolean filterMessage(String msg,FilterParams params,String pattern){
        Map<String,String> kvMap = new HashMap<>();
        if (StringUtils.isNotEmpty(pattern)){
            Pattern patternUri = Pattern.compile(pattern);
            Matcher matcher = patternUri.matcher(msg);
            if (matcher.find()) {
                return true;
            }
            return false;
        }
        Pattern patternUri = Pattern.compile("/\\w+\\?");
        Matcher matcher = patternUri.matcher(msg);
        if (!matcher.find()){
            return false;
        }
        String startParams = msg.substring(matcher.end());
        String paramsStr = startParams.substring(0,startParams.indexOf(' '));
        String[] kvParams = paramsStr.split("&");
        for (String param : new ArrayList<>(Arrays.asList(kvParams))) {
            String[] kv = param.split("=");
            if (kv.length==2){
                kvMap.put(kv[0],kv[1]);
            }
        }
        if (StringUtils.isNotEmpty(params.getUri())){
            if (!msg.contains(params.getUri())){
                return false;
            }
        }
        if (StringUtils.isNotEmpty(params.getBstp())){
            if (kvMap.get("bstp")==null||kvMap.get("bstp")!=null&&!kvMap.get("bstp").equals(params.getBstp())){
                return false;
            }
        }
        if (StringUtils.isNotEmpty(params.getT())){
            if (kvMap.get("t")==null||kvMap.get("t")!=null&&!kvMap.get("t").equals(params.getT())){
                return false;
            }
        }
        if (StringUtils.isNotEmpty(params.getRpage())){
            if (kvMap.get("rpage")==null||kvMap.get("rpage")!=null&&!kvMap.get("rpage").equals(params.getRpage())){
                return false;
            }
        }
        if (StringUtils.isNotEmpty(params.getU())){
            if (kvMap.get("u")==null||kvMap.get("u")!=null&&!kvMap.get("u").equals(params.getU())){
                return false;
            }
        }
        return true;
    }


    private SocketMessage parseMessage(String msg){
        Set<String> errSet = new LinkedHashSet<>();
        Set<String> lackSet = new LinkedHashSet<>();
        Set<String> nullSet = new LinkedHashSet<>();
        msg = msg.trim();
        //公共字段
        String[] comParam = Constants.commonParams.split(",");
        ArrayList<String> comList = new ArrayList<>(Arrays.asList(comParam));

        List<String> allParamList = new ArrayList<>();
        allParamList.addAll(Arrays.asList(Constants.t_20_params.split(",")));
        allParamList.addAll(Arrays.asList(Constants.t_21_params.split(",")));
        //所有字段
        allParamList = allParamList.stream().distinct().collect(Collectors.toList());
        //
        Map<String,String> kvMap = new HashMap<>();
        Pattern pattern = Pattern.compile("/\\w+\\?");
        Matcher matcher = pattern.matcher(msg);
        if (!matcher.find()){
            errSet.add("/\\w+\\?此正则匹配失败");
        }else {
            //判断值空
            String startParams = msg.substring(matcher.end());
            String paramsStr = startParams.substring(0,startParams.indexOf(' '));
            String[] kvParams = paramsStr.split("&");
            //System.out.println(JSONObject.toJSONString(kvParams));
            for (String param : new ArrayList<>(Arrays.asList(kvParams))) {
                String[] kv = param.split("=");
                if (kv.length==2){
                    kvMap.put(kv[0],kv[1]);
                }else {
                    if (StringUtils.isNotEmpty(kv[0])&&(allParamList.contains(kv[0])||kv[0].equals("net_work"))){
                        kvMap.put(kv[0],"null");
                        nullSet.add(kv[0]+"字段值为null");
                    }
                }
            }
            //判断缺少公共字段
            for (String com : comList) {
                if (kvMap.get(com)==null){
                    if (com.equals("ntwk")&&kvMap.get("net_work")!=null){
                        continue;
                    }
                    lackSet.add(com);
                }
            }
        }

        String logTypeId = kvMap.get("t");
        if (StringUtils.isNotEmpty(logTypeId)){
            ArrayList<String> tList = null;
            switch (logTypeId){
                case "20":
                    tList = new ArrayList<>(Arrays.asList(Constants.t_20_params.split(",")));
                    break;
                case "21":
                    tList = new ArrayList<>(Arrays.asList(Constants.t_21_params.split(",")));
                    break;
                default:
                    tList = new ArrayList<>();
                    break;
            }
            for (String t : tList) {
                if (kvMap.get(t)==null){
                    lackSet.add(t);
                }
            }
        }

        if (!CollectionUtils.isEmpty(lackSet)){
            String err = "缺少"+StringUtils.join(lackSet.toArray(new String[0])," , ") +"字段";
            errSet.add(err);
        }

        if (!CollectionUtils.isEmpty(nullSet)){
            String err = StringUtils.join(nullSet.toArray(new String[0])," , ") + "值为null";
            errSet.add(err);
        }


        if (CollectionUtils.isEmpty(errSet)){
            return generateMsg(msg,null,TypeEnums.NOT_CHECK_MSG_OPERATE.getCode());
        }else {
            return generateMsg(msg,StringUtils.join(errSet.toArray(new String[0])," ; "),TypeEnums.WRONG_MEG_OPERATE.getCode());
        }

    }

    private SocketMessage generateMsg(String msg,String error,Integer type){
        SocketMessage socketMessage =new SocketMessage();
        socketMessage.setMsg(msg);
        socketMessage.setError(error);
        socketMessage.setType(type);
        return socketMessage;
    }

}
