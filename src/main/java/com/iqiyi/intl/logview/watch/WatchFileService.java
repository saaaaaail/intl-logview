package com.iqiyi.intl.logview.watch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iqiyi.intl.logview.cache.Cache;
import com.iqiyi.intl.logview.constant.Constants;
import com.iqiyi.intl.logview.enums.TypeEnums;
import com.iqiyi.intl.logview.websocket.FilterParams;
import com.iqiyi.intl.logview.websocket.SocketMessage;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
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
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @program: intl-logview
 * @description: 采用 jdk7 watchService 实现实时文件监听
 * @author: yangfan
 * @create: 2019/12/25 16:00
 */

@Service
@Slf4j
public class WatchFileService {

    @Autowired
    @Qualifier("localCache")
    private Cache cache;

    private Integer count = 0;

    private Long pointer = 0L;

    private boolean isPause = false;

    public void startProcess(Session host, Map<Session, SocketMessage> sessionMap,SocketMessage socketMessage) throws IOException {
        log.info("校验是否第一次读取文件");
        String watchServiceKey = Constants.WATCH_SERVICE_KEY + host.getId();
        String filterParamsKey = Constants.FILTER_PARAMS_KEY + host.getId();
        String poolKey = Constants.THREAD_POOL+host.getId();
        synchronized (this){
            Object o1 = cache.get(watchServiceKey);
            Object o2 = cache.get(poolKey);
            if (o1!=null){
                log.info("非第一次读取");
                closeWatchService(host);
                closePool(host);
            }
            WatchService watchService = FileSystems.getDefault().newWatchService();
            ExecutorService executorService = Executors.newFixedThreadPool(1);
            cache.add(poolKey,executorService);
            cache.add(watchServiceKey,watchService);
            cache.add(filterParamsKey,socketMessage);
        }

        //清屏
        clearMsg(host);
        log.info("清屏");
        //恢复暂停操作
        recoverPause(host);

        pointer = firstReadFile(host);
        //筛选按钮可用
        enableFilterBtnMsg(host);
        log.info("筛选按钮可用");
        //开始实时读取文件
        startMonitor(host,sessionMap);

    }

    public void startMonitor(Session session,Map<Session, SocketMessage> sessionMap) throws IOException {
        String poolKey = Constants.THREAD_POOL+session.getId();
        String watchServiceKey = Constants.WATCH_SERVICE_KEY + session.getId();

        WatchService watchService = (WatchService) cache.get(watchServiceKey);
        Path path = Paths.get(Constants.WATCH_FILE_CATEGORY);
        path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        Object o = cache.get(poolKey);
        ((ExecutorService)o).submit(() -> {
            try {
                WatchKey watchKey = null;
                while ((watchKey = watchService.take())!=null){
                    if (!isPause){
                        for (WatchEvent<?> pollEvent : watchKey.pollEvents()) {
                            log.info("Constants.WATCH_FILE:"+Constants.WATCH_FILE+" pollEvent.context():"+pollEvent.context());
                            if (Constants.WATCH_FILE.equals(pollEvent.context().toString())){
                                log.info("readFile:access.log");
                                readFile(session,sessionMap);
                            }
                        }
                    }
                    watchKey.reset();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                log.info("线程结束！");
                try {
                    closeWatchService(session);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    public void readFile(Session session,Map<Session, SocketMessage> sessionMap) throws IOException {
        RandomAccessFile file = null;
        List<String> result = new ArrayList<>();
        try {
            file = new RandomAccessFile(Constants.WATCH_FILE_PATH,"r");
            file.seek(pointer);
            String msgline = null;
            while ((msgline = file.readLine())!=null){
                String msg = new String(msgline.getBytes(StandardCharsets.ISO_8859_1),StandardCharsets.UTF_8).trim();
                if (StringUtils.isNotEmpty(msg)&&msg.contains(Constants.SRC_TARGET_IP)&&msg.startsWith(Constants.SRC_TARGET_IP)){
                    result.add(msg);
                }
            }
            pointer = file.getFilePointer();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            if (file!=null){
                log.info("一次读取文件流关闭");
                file.close();
            }
        }

        sendMessage(session,result);

    }

    /**
     * 首先读取历史记录
     * @param session
     * @return
     */
    private Long firstReadFile(Session session){
        RandomAccessFile file = null;
        List<String> result = new ArrayList<>();
        Long point = 0L;
        try {
            file = new RandomAccessFile(Constants.WATCH_FILE_PATH,"r");
            log.info("start randomAccessFile");
            Long fileLength = file.length();
            point = fileLength-1;
            int tmp =-1;
            while (point>=0){
                tmp = file.read();
                if (tmp == '\n'){
                    String msg = null;
                    if ((msg = file.readLine())!=null){
                        String msgline = new String(msg.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
                        if (StringUtils.isNotEmpty(msgline)&&msg.contains(Constants.SRC_TARGET_IP)&&msgline.startsWith(Constants.SRC_TARGET_IP)) {
                            result.add(msgline);
                        }
                    }
                }
                if (point==0){
                    file.seek(0L);
                    String msg = null;
                    if ((msg = file.readLine())!=null){
                        String msgline = new String(msg.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
                        if (StringUtils.isNotEmpty(msgline)&&msg.contains(Constants.SRC_TARGET_IP)&&msgline.startsWith(Constants.SRC_TARGET_IP)){
                            result.add(msgline);
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

            sendMessage(session,result);

            log.info("end randomAccessFile");
            return fileLength;
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

    public void closeWatchService(Session session) throws IOException {
        String watchServiceKey = Constants.WATCH_SERVICE_KEY+session.getId();
        Object o = cache.get(watchServiceKey);
        if (o!=null){
            isPause = false;
            cache.remove(watchServiceKey);
            ((WatchService)o).close();
            log.info("关闭监听器！");
        }
    }

    public void closePool(Session session){
        String poolKey = Constants.THREAD_POOL+session.getId();
        Object o = cache.get(poolKey);
        if (o!=null){
            cache.remove(poolKey);
            ((ExecutorService)o).shutdown();
            log.info("关闭线程！");
        }
    }

//    public void sendMessageToAll(Session session,Map<Session, SocketMessage> sessionMap, String msg){
//        String filterParamsKey = Constants.FILTER_PARAMS_KEY + session.getId();
//        SocketMessage o = (SocketMessage)cache.get(filterParamsKey);
//        if (filterMessage(msg,o.getParams(),o.getPattern())){
//            List<SocketMessage> socketMessages = parseMessage(msg);
//            sessionMap.forEach((sess, message) -> {
//                try {
//                    for (SocketMessage socketMessage : socketMessages) {
//                        sess.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });
//        }
//    }

    public void sendMessage(Session session, List<String> msgs){
        String filterParamsKey = Constants.FILTER_PARAMS_KEY + session.getId();
        SocketMessage o = (SocketMessage)cache.get(filterParamsKey);
        List<String> splitMsg = splitMessage(msgs);
        log.info(JSONObject.toJSONString(splitMsg));
        for (int i=splitMsg.size()-1;i>=0;i--) {
            String msg = splitMsg.get(i);
            if (filterMessage(msg,o.getParams(),o.getPattern())){
                List<SocketMessage> socketMessages = parseMessage(msg);
                try {
                    for (SocketMessage socketMessage : socketMessages) {
                        session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private List<String> splitMessage(List<String> msgs){
        List<String> splitMsg = new ArrayList<>();
        for (String msg : msgs) {
            Pattern postPattern = Pattern.compile("\"(POST)[ ]/");
            Matcher postMatcher = postPattern.matcher(msg);
            if (postMatcher.find()){
                Pattern bodyPattern = Pattern.compile("body:\"msg=\\[\\{.*}]\"");
                Matcher bodyMatcher = bodyPattern.matcher(msg);
                if (bodyMatcher.find()){
                    String preMsg = msg.substring(0, bodyMatcher.start()+10);
                    String msgBody = msg.substring(bodyMatcher.start()+10, bodyMatcher.end()-1);
                    String postMsg = msg.substring(bodyMatcher.end()-1);
                    msgBody = msgBody.replace("\\t","").replace("\\n","").replace("\\","");
                    log.info(msgBody);
                    if (isJsonArray(msgBody)){
                        JSONArray jsonArray = JSONObject.parseArray(msgBody);
                        for (Object o:jsonArray){
                            String tmpMsg = preMsg +"["+JSONObject.toJSONString(o)+"]"+postMsg;
                            splitMsg.add(tmpMsg);
                        }
                        continue;
                    }
                }
            }
            splitMsg.add(msg);
        }
        return splitMsg;
    }

    public void pauseMonitor(Session session) throws IOException {
        String watchServiceKey = Constants.WATCH_SERVICE_KEY+session.getId();
        Object o = cache.get(watchServiceKey);
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
        session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
    }

    public void heartBeat(Session session) throws IOException {
        SocketMessage socketMessage = generateMsg("heart",null,TypeEnums.HEART_MSG_OPERATE.getCode());
        session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
    }

    public void clearMsg(Session session){
        SocketMessage socketMessage = generateMsg(null,null,TypeEnums.CLEAR_MSG_OPERATE.getCode());
        try {
            session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void recoverPause(Session session){
        isPause=false;
        SocketMessage socketMessage = generateMsg("0",null,TypeEnums.PAUSE_OPERATE.getCode());
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



    private boolean filterMessage(String msg,FilterParams params,String patternStr){
        if (StringUtils.isNotEmpty(patternStr)){
            Pattern patternUri = Pattern.compile(patternStr);
            Matcher matcher = patternUri.matcher(msg);
            if (matcher.find()) {
                return true;
            }
            return false;
        }

        msg = msg.trim();
        //公共字段
        String[] comParam = Constants.commonParams.split(",");
        List<String> comList = new ArrayList<>(Arrays.asList(comParam));

        List<String> allParamList = new ArrayList<>();
        allParamList.addAll(comList);
        allParamList.addAll(Arrays.asList(Constants.t_20_params.split(",")));
        allParamList.addAll(Arrays.asList(Constants.t_21_params.split(",")));
        //所有字段
        allParamList = allParamList.stream().distinct().collect(Collectors.toList());
        Map<String,String> kvMap = new HashMap<>();
        Pattern getPattern = Pattern.compile("\"(GET)[ ]/");
        Matcher getMatcher = getPattern.matcher(msg);
        Pattern postPattern = Pattern.compile("\"(POST)[ ]/");
        Matcher postMatcher = postPattern.matcher(msg);
        if (getMatcher.find()){
            Pattern pattern = Pattern.compile("/\\w+\\?");
            Matcher matcher = pattern.matcher(msg);
            if (!matcher.find()){
                return false;
            }
            kvMap = getCheck(msg,matcher.end(),allParamList,null);
        }else if (postMatcher.find()){
            kvMap = postCheck(msg,allParamList,null);
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



    private List<SocketMessage> parseMessage(String msg){
        List<SocketMessage> socketMsgs = new ArrayList<>();
        Set<String> errSet = new LinkedHashSet<>();
        Set<String> lackSet = new LinkedHashSet<>();
        Set<String> nullSet = new LinkedHashSet<>();
        msg = msg.trim();
        //公共字段
        String[] comParam = Constants.commonParams.split(",");
        List<String> comList = new ArrayList<>(Arrays.asList(comParam));

        List<String> allParamList = new ArrayList<>();
        allParamList.addAll(comList);
        allParamList.addAll(Arrays.asList(Constants.t_20_params.split(",")));
        allParamList.addAll(Arrays.asList(Constants.t_21_params.split(",")));
        //所有字段
        allParamList = allParamList.stream().distinct().collect(Collectors.toList());
        Map<String,String> kvMap = new HashMap<>();
        Pattern getPattern = Pattern.compile("\"(GET)[ ]/");
        Matcher getMatcher = getPattern.matcher(msg);
        Pattern postPattern = Pattern.compile("\"(POST)[ ]/");
        Matcher postMatcher = postPattern.matcher(msg);
        if (getMatcher.find()){
            Pattern pattern = Pattern.compile("/\\w+\\?");
            Matcher matcher = pattern.matcher(msg);
            if (!matcher.find()){
                errSet.add("/\\w+\\?此正则匹配失败");
                SocketMessage socketMessage = generateMsg(msg, StringUtils.join(errSet.toArray(new String[0]), " ; "), TypeEnums.WRONG_MEG_OPERATE.getCode());
                socketMsgs.add(socketMessage);
                return socketMsgs;
            }
            kvMap = getCheck(msg,matcher.end(),allParamList,nullSet);
        }else if (postMatcher.find()){
            kvMap = postCheck(msg,allParamList,nullSet);
        }

        //判断是否需要校验
        String logTypeId = kvMap.get("t");
        if (StringUtils.isEmpty(logTypeId)||StringUtils.isNotEmpty(logTypeId)&&!"20".equals(logTypeId)&&!"21".equals(logTypeId)){
            SocketMessage socketMessage = generateMsg(msg, null, TypeEnums.NOT_CHECK_MSG_OPERATE.getCode());
            socketMsgs.add(socketMessage);
            return socketMsgs;
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


        if (!CollectionUtils.isEmpty(lackSet)){
            String err = "缺少"+StringUtils.join(lackSet.toArray(new String[0])," , ") +"字段";
            errSet.add(err);
        }

        if (!CollectionUtils.isEmpty(nullSet)){
            String err = StringUtils.join(nullSet.toArray(new String[0])," , ") + "值为null";
            errSet.add(err);
        }

        if (CollectionUtils.isEmpty(errSet)){
            socketMsgs.add(generateMsg(msg,null, TypeEnums.RIGHT_MSG_OPERATE.getCode()));
            return socketMsgs;
        }else {
            socketMsgs.add(generateMsg(msg,StringUtils.join(errSet.toArray(new String[0])," ; "),TypeEnums.WRONG_MEG_OPERATE.getCode()));
            return socketMsgs;
        }
    }


    private Map<String,String> postCheck(String msg,List<String> allParamList,Set<String> nullSet){
        Map<String,String> kvMap = new HashMap<>();

        Pattern bodyPattern = Pattern.compile("body:\"msg=\\[\\{.*}]\"");
        Matcher bodyMatcher = bodyPattern.matcher(msg);
        if (bodyMatcher.find()) {
            String preMsg = msg.substring(0, bodyMatcher.start()+10);
            String msgBody = msg.substring(bodyMatcher.start()+10, bodyMatcher.end()-1);
            String postMsg = msg.substring(bodyMatcher.end()-1);
            //清空body里面的换行符制表符右斜杠
            msgBody = msgBody.replace("\\t","").replace("\\n","").replace("\\","");
            log.info(msgBody);
            if (isJsonArray(msgBody)){
                JSONArray jsonArray = JSONObject.parseArray(msgBody);
                Set<Map.Entry<String, Object>> entries = ((JSONObject)jsonArray.get(0)).entrySet();
                for (Map.Entry<String, Object> entry : entries) {
                    kvMap.put(entry.getKey(),entry.getValue().toString());
                    if (StringUtils.isEmpty(entry.getValue().toString())&&allParamList.contains(entry.getKey())&&nullSet!=null){
                        nullSet.add(entry.getKey());
                    }
                }
            }
        }

        //判断param参数
        Pattern pattern = Pattern.compile("/\\w+\\?");
        Matcher matcher = pattern.matcher(msg);
        if (matcher.find()){
            String startParams = msg.substring(matcher.end());
            String paramsStr = startParams.substring(0,startParams.indexOf(' '));
            String[] kvParams = paramsStr.split("&");
            for (String param : new ArrayList<>(Arrays.asList(kvParams))) {
                String[] kv = param.split("=");
                if (kv.length==2){
                    if (StringUtils.isEmpty(kv[1])&&nullSet!=null){
                        nullSet.add(kv[0]);
                    }
                    kvMap.put(kv[0],kv[1]);
                }else {
                    if (StringUtils.isNotEmpty(kv[0])&&(allParamList.contains(kv[0])||kv[0].equals("net_work"))){
                        kvMap.put(kv[0],"null");
                        if (nullSet!=null){
                            nullSet.add(kv[0]);
                        }
                    }
                }
            }
        }
        return kvMap;
    }


    private Map<String,String> getCheck(String msg,Integer matchEnd,List<String> allParamList,Set<String> nullSet){
        Map<String,String> kvMap = new HashMap<>();

        //判断值空
        String startParams = msg.substring(matchEnd);
        String paramsStr = startParams.substring(0,startParams.indexOf(' '));
        String[] kvParams = paramsStr.split("&");
        //System.out.println(JSONObject.toJSONString(kvParams));
        for (String param : new ArrayList<>(Arrays.asList(kvParams))) {
            String[] kv = param.split("=");
            if (kv.length==2){
                if (StringUtils.isEmpty(kv[1])){
                    nullSet.add(kv[0]);
                }
                kvMap.put(kv[0],kv[1]);
            }else {
                if (StringUtils.isNotEmpty(kv[0])&&(allParamList.contains(kv[0])||kv[0].equals("net_work"))){
                    kvMap.put(kv[0],"null");
                    if (!CollectionUtils.isEmpty(nullSet)){
                        nullSet.add(kv[0]);
                    }
                }
            }
        }
        return kvMap;
    }

    private SocketMessage generateMsg(String msg,String error,Integer type){
        SocketMessage socketMessage =new SocketMessage();
        socketMessage.setMsg(msg);
        socketMessage.setError(error);
        socketMessage.setType(type);
        return socketMessage;
    }

    private boolean isJsonObject(String json){
        try{
            JSONObject.parseObject(json);
        }catch (Exception e){
            return false;
        }
        return true;
    }

    private boolean isJsonArray(String json){
        try{
            JSONObject.parseArray(json);
        }catch (Exception e){
            return false;
        }
        return true;
    }



}
