package com.iqiyi.intl.logview.watch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iqiyi.intl.logview.cache.Cache;
import com.iqiyi.intl.logview.constant.Constants;
import com.iqiyi.intl.logview.dto.CheckParam;
import com.iqiyi.intl.logview.dto.TypeParam;
import com.iqiyi.intl.logview.enums.MatchEnums;
import com.iqiyi.intl.logview.enums.TypeEnums;
import com.iqiyi.intl.logview.service.RulesService;
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
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    private RulesService rulesService;

    private boolean isPause = false;

    public void startProcess(Session host, Map<Session, SocketMessage> sessionMap,SocketMessage socketMessage) throws IOException {
        log.info("校验是否第一次读取文件");
        String watchServiceKey = Constants.WATCH_SERVICE_KEY + host.getId();
        String poolKey = Constants.THREAD_POOL+host.getId();
        String pointerKey = Constants.POINTER_KEY + host.getId();
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
        }

        //清屏
        clearMsg(host);
        log.info("清屏");
        //恢复暂停操作
        recoverPause(host);

        CheckParam checkParam = parseCheckStr(host);
        Long pointer = firstReadFile(host,checkParam);
        cache.add(pointerKey,pointer);//更新指针

        //筛选按钮可用
        enableFilterBtnMsg(host);
        log.info("筛选按钮可用");
        //开始实时读取文件
        startMonitor(host,sessionMap,checkParam);

    }

    public void startMonitor(Session session,Map<Session, SocketMessage> sessionMap,CheckParam checkParam) throws IOException {
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
                                readFile(session,sessionMap,checkParam);
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

    public void readFile(Session session,Map<Session, SocketMessage> sessionMap,CheckParam checkParam) throws IOException {
        RandomAccessFile file = null;
        List<String> result = new ArrayList<>();
        String pointerKey = Constants.POINTER_KEY + session.getId();
        Long pointer = (Long) cache.get(pointerKey);
        try {
            file = new RandomAccessFile(Constants.WATCH_FILE_PATH,"r");
            file.seek(pointer);
            String msgline = null;
            while ((msgline = file.readLine())!=null){
                String msg = new String(msgline.getBytes(StandardCharsets.ISO_8859_1),StandardCharsets.UTF_8).trim();
                if (StringUtils.isNotEmpty(msg)&&containsIp(msg)){
                    result.add(msg);
                }
            }
            pointer = file.getFilePointer();
            cache.add(pointerKey,pointer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            if (file!=null){
                log.info("一次读取文件流关闭");
                file.close();
            }
        }

        sendMessage(session,result,checkParam);

    }

    /**
     * 首先读取历史记录
     * @param session
     * @return
     */
    private Long firstReadFile(Session session,CheckParam checkParam){
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
                        if (StringUtils.isNotEmpty(msgline)&&containsIp(msgline)) {
                            result.add(msgline);
                        }
                    }
                }
                if (point==0){
                    file.seek(0L);
                    String msg = null;
                    if ((msg = file.readLine())!=null){
                        String msgline = new String(msg.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
                        if (StringUtils.isNotEmpty(msgline)&&containsIp(msgline)){
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

            sendMessage(session,result,checkParam);

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

    public void sendMessage(Session session, List<String> msgs,CheckParam checkParam){
//        String filterParamsKey = Constants.FILTER_PARAMS_KEY + session.getId();
//        SocketMessage o = (SocketMessage)cache.get(filterParamsKey);
        List<SocketMessage> splitSckMsg = splitMessage(msgs);
        log.info(JSONObject.toJSONString(splitSckMsg));
        for (int i=splitSckMsg.size()-1;i>=0;i--) {
            SocketMessage sckMsg = splitSckMsg.get(i);
            SocketMessage socketMessage = parseMessage(sckMsg,checkParam);
            try {
                session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private List<SocketMessage> splitMessage(List<String> msgs){
        List<SocketMessage> splitSckMsg = new ArrayList<>();
        for (String msg : msgs) {
            msg = msg.trim();
            Pattern postPattern = Pattern.compile("\"(POST)[ ]/");
            Matcher postMatcher = postPattern.matcher(msg);
            if (postMatcher.find()){
                Pattern bodyPattern = Pattern.compile("<rb>\"msg=.*\"</rb>");
                Matcher bodyMatcher = bodyPattern.matcher(msg);
                if (bodyMatcher.find()){
                    String preMsg = msg.substring(0, bodyMatcher.start()+9);
                    String msgBody = msg.substring(bodyMatcher.start()+9, bodyMatcher.end()-6);
                    String postMsg = msg.substring(bodyMatcher.end()-6);
                    msgBody = msgBody.replace("\\t","").replace("\\n","").replace("\\","");
                    log.info(msgBody);
                    if (isJsonArray(msgBody)){
                        JSONArray jsonArray = JSONObject.parseArray(msgBody);
                        String groupId = null;
                        if (jsonArray.size()!=1){
                            groupId = UUID.randomUUID().toString().replace("-","");
                        }
                        for (Object o:jsonArray){
                            String tmpMsg = preMsg +"["+JSONObject.toJSONString(o)+"]"+postMsg;
                            splitSckMsg.add(generateMsg(tmpMsg,null,null,groupId));
                        }
                        continue;
                    }
                    if (isJsonObject(msgBody)){
                        JSONObject jsonObject = JSONObject.parseObject(msgBody);
                        String tmpMsg = preMsg + "["+JSONObject.toJSONString(jsonObject)+"]"+postMsg;
                        splitSckMsg.add(generateMsg(tmpMsg,null,null,null));
                        continue;
                    }
                }
            }
            splitSckMsg.add(generateMsg(msg,null,null,null));
        }
        return splitSckMsg;
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
        SocketMessage socketMessage = generateMsg("heart",null,TypeEnums.HEART_MSG_OPERATE.getCode(),null);
        session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
    }

    public void clearMsg(Session session){
        SocketMessage socketMessage = generateMsg(null,null,TypeEnums.CLEAR_MSG_OPERATE.getCode(),null);
        try {
            session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void coverCheckUserName(Session session, String userName){
        String checkUserNameKey = Constants.CHECK_USERNAME_KEY + session.getId();
        cache.add(checkUserNameKey,userName);
    }

    public void clearCheckUserName(Session session){
        String checkUserNameKey = Constants.CHECK_USERNAME_KEY + session.getId();
        Object o = cache.get(checkUserNameKey);
        if (o!=null){
            cache.remove(checkUserNameKey);
        }
    }

    public void clearPointer(Session session){
        String pointerKey = Constants.POINTER_KEY + session.getId();
        Object o = cache.get(pointerKey);
        if (o!=null){
            cache.remove(pointerKey);
        }
    }

    private void recoverPause(Session session){
        isPause=false;
        SocketMessage socketMessage = generateMsg("0",null,TypeEnums.PAUSE_OPERATE.getCode(),null);
        try {
            session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void enableFilterBtnMsg(Session session){
        SocketMessage socketMessage = generateMsg(null,null,TypeEnums.ENABLE_FILTER_BTN_OPERATE.getCode(),null);
        try {
            session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CheckParam parseCheckStr(Session session){
        String checkUserNameKey = Constants.CHECK_USERNAME_KEY + session.getId();
        String userName = (String) cache.get(checkUserNameKey);
        if(StringUtils.isEmpty(userName)){
            userName = Constants.DEFAULT_USER;
        }
        String checkStr = rulesService.selectByUserName(userName);
        CheckParam checkParam = JSONObject.parseObject(checkStr, CheckParam.class);
        return checkParam;
    }

    private SocketMessage parseMessage(SocketMessage sckmsg,CheckParam checkParam){
        String msg = sckmsg.getMsg();
        Set<String> errSet = new LinkedHashSet<>();
        Set<String> lackSet = new LinkedHashSet<>();
        Set<String> nullSet = new LinkedHashSet<>();
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
            Pattern pattern = Pattern.compile("GET (/\\w+)+\\?");
            Matcher matcher = pattern.matcher(msg);
            if (!matcher.find()){
                errSet.add("/\\w+\\?此正则匹配失败");
                sckmsg.setType(TypeEnums.NOT_CHECK_MSG_OPERATE.getCode());
                return sckmsg;
            }else{
                String uri = msg.substring(matcher.start()+4,matcher.end()-1);
                sckmsg.setUrl(uri);
            }
            sckmsg.setMethod("GET");
            kvMap = getCheck(msg,matcher.end());
        }else if (postMatcher.find()){
            sckmsg.setMethod("POST");
            Pattern urlPattern1 = Pattern.compile("POST (/\\w+)+\\?");
            Matcher matcher = urlPattern1.matcher(msg);
            if (matcher.find()){
                String uri = msg.substring(matcher.start()+5,matcher.end()-1);
                sckmsg.setUrl(uri);
            }else {
                Pattern urlPattern2 = Pattern.compile("POST (/\\w+)+ ");
                Matcher matcher1 = urlPattern2.matcher(msg);
                if (matcher1.find()){
                    String uri = msg.substring(matcher1.start()+5,matcher1.end()-1);
                    sckmsg.setUrl(uri);
                }
            }
            kvMap = postCheck(msg);
        }

        //校验前构建消息体
        sckmsg.setIp(msg.substring(0,msg.indexOf(" ")));
        Pattern timePattern = Pattern.compile("\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}]");
        Matcher timeMatcher = timePattern.matcher(msg);
        if (timeMatcher.find()){
            String time = msg.substring(timeMatcher.start()+1,timeMatcher.end()-1);
            Long timestamp = parseTimestamp(time);
            sckmsg.setTime(timestamp);
        }
        JSONObject params = new JSONObject();
        for (Map.Entry<String, String> entry : kvMap.entrySet()) {
            params.put(entry.getKey(),entry.getValue());
        }
        sckmsg.setParams(params);

        //判断url是否需要校验
        String matchUrl = checkParam.getUrl();
        if (checkParam.getUseReg()&&StringUtils.isNotEmpty(checkParam.getReg())){
            Pattern pattern = Pattern.compile(checkParam.getReg());
            Matcher matcher = pattern.matcher(sckmsg.getUrl());
            if (checkParam.getUrlMatch().equals(MatchEnums.BLURRY.getCode())){
                if (!matcher.find()){
                    sckmsg.setType(TypeEnums.NOT_CHECK_MSG_OPERATE.getCode());
                    return sckmsg;
                }
            }else if (checkParam.getUrlMatch().equals(MatchEnums.EXACT.getCode())){
                if (!matcher.matches()){
                    sckmsg.setType(TypeEnums.NOT_CHECK_MSG_OPERATE.getCode());
                    return sckmsg;
                }
            }
        }else {
            if (checkParam.getUrlMatch().equals(MatchEnums.BLURRY.getCode())){
                if (!sckmsg.getUrl().contains(matchUrl)){
                    sckmsg.setType(TypeEnums.NOT_CHECK_MSG_OPERATE.getCode());
                    return sckmsg;
                }
            }else if (checkParam.getUrlMatch().equals(MatchEnums.EXACT.getCode())){
                if (!sckmsg.getUrl().equals(matchUrl)){
                    sckmsg.setType(TypeEnums.NOT_CHECK_MSG_OPERATE.getCode());
                    return sckmsg;
                }
            }
        }

        List<Map<String,String>> error = new ArrayList<>();
        List<List<TypeParam>> rules = checkParam.getRules();
        boolean isCorrect = true;
        for (int i=0;i<rules.size();i++){
            Map<String,String> errorMap = new HashMap<>();
            List<TypeParam> typeParams = rules.get(i);
            for (TypeParam typeParam : typeParams) {
                String key = typeParam.getType();
                Object o = params.get(key);
                if (o==null){
                    if ("ntwk".equals(key)&&params.get("net_work")!=null){
                        o = params.get("net_work");
                    }else if ("net_work".equals(key)&&params.get("ntwk")!=null){
                        o = params.get("ntwk");
                    }else {
                        errorMap.put(typeParam.getType(),"校验的url中无指定的字段");
                        continue;
                    }
                }
                if (typeParam.getUseReg()&&StringUtils.isNotEmpty(typeParam.getReg())){
                    Pattern valuePattern = Pattern.compile(typeParam.getReg());
                    Matcher valueMatcher = valuePattern.matcher(o.toString());
                    if (typeParam.getMatch().equals(MatchEnums.BLURRY.getCode())){
                        if (!valueMatcher.find()){
                            errorMap.put(typeParam.getType(),"值不匹配");
                            continue;
                        }
                    }else if (typeParam.getMatch().equals(MatchEnums.EXACT.getCode())){
                        if (!valueMatcher.matches()){
                            errorMap.put(typeParam.getType(),"值不匹配");
                            continue;
                        }
                    }
                }else {
                    if (typeParam.getEmpty()==1&&StringUtils.isEmpty(o.toString())){
                        errorMap.put(typeParam.getType(),"值为空");
                        continue;
                    }
                    if (StringUtils.isNotEmpty(typeParam.getValue())){
                        if (typeParam.getMatch().equals(MatchEnums.BLURRY.getCode())){
                            if (isLong(typeParam.getValue())||!o.toString().contains(typeParam.getValue())){
                                errorMap.put(typeParam.getType(),"值不匹配");
                                continue;
                            }
                        }else if (typeParam.getMatch().equals(MatchEnums.EXACT.getCode())){
                            if (!o.toString().equals(typeParam.getValue())){
                                errorMap.put(typeParam.getType(),"值不匹配");
                                continue;
                            }
                        }
                    }
                }
            }
            if (isCorrect&&errorMap.size()!=0){
                isCorrect=false;
            }
            error.add(errorMap);
        }

        sckmsg.setError(error);
        if (isCorrect){
            sckmsg.setType(TypeEnums.RIGHT_MSG_OPERATE.getCode());
        }else {
            sckmsg.setType(TypeEnums.WRONG_MEG_OPERATE.getCode());
        }
        return sckmsg;
//        //判断是否需要校验
//        String logTypeId = kvMap.get("t");
//        if (StringUtils.isEmpty(logTypeId)||StringUtils.isNotEmpty(logTypeId)&&!"20".equals(logTypeId)&&!"21".equals(logTypeId)){
//            sckmsg.setType(TypeEnums.NOT_CHECK_MSG_OPERATE.getCode());
//            return sckmsg;
//        }
//        //判断缺少公共字段
//        for (String com : comList) {
//            if (kvMap.get(com)==null){
//                if (com.equals("ntwk")&&kvMap.get("net_work")!=null){
//                    continue;
//                }
//                lackSet.add(com);
//            }
//        }
//
//        ArrayList<String> tList = null;
//        switch (logTypeId){
//            case "20":
//                tList = new ArrayList<>(Arrays.asList(Constants.t_20_params.split(",")));
//                break;
//            case "21":
//                tList = new ArrayList<>(Arrays.asList(Constants.t_21_params.split(",")));
//                break;
//            default:
//                tList = new ArrayList<>();
//                break;
//        }
//        for (String t : tList) {
//            if (kvMap.get(t)==null){
//                lackSet.add(t);
//            }
//        }
//
//
//        if (!CollectionUtils.isEmpty(lackSet)){
//            String err = "缺少"+StringUtils.join(lackSet.toArray(new String[0])," , ") +"字段";
//            errSet.add(err);
//        }
//
//        if (!CollectionUtils.isEmpty(nullSet)){
//            String err = StringUtils.join(nullSet.toArray(new String[0])," , ") + "值为null";
//            errSet.add(err);
//        }
//
//        if (CollectionUtils.isEmpty(errSet)){
//            sckmsg.setType(TypeEnums.RIGHT_MSG_OPERATE.getCode());
//            return sckmsg;
//        }else {
//            sckmsg.setType(TypeEnums.WRONG_MEG_OPERATE.getCode());
//            sckmsg.setError(StringUtils.join(errSet.toArray(new String[0])," ; "));
//            return sckmsg;
//        }
    }


    private Map<String,String> postCheck(String msg){
        Map<String,String> kvMap = new HashMap<>();
        msg = msg.replace("\\t","").replace("\\n","").replace("\\","");
        Pattern bodyPattern = Pattern.compile("<rb>\"msg=\\[\\{.*}]\"</rb>");
        Matcher bodyMatcher = bodyPattern.matcher(msg);
        if (bodyMatcher.find()) {
            String preMsg = msg.substring(0, bodyMatcher.start()+9);
            String msgBody = msg.substring(bodyMatcher.start()+9, bodyMatcher.end()-6);
            String postMsg = msg.substring(bodyMatcher.end()-6);
            //清空body里面的换行符制表符右斜杠
            msgBody = msgBody.replace("\\t","").replace("\\n","").replace("\\","");
            log.info(msgBody);
            if (isJsonArray(msgBody)){
                JSONArray jsonArray = JSONObject.parseArray(msgBody);
                Set<Map.Entry<String, Object>> entries = ((JSONObject)jsonArray.get(0)).entrySet();
                for (Map.Entry<String, Object> entry : entries) {
                    if (StringUtils.isEmpty(entry.getValue().toString())){
                        kvMap.put(entry.getKey(),"");
                    }else {
                        kvMap.put(entry.getKey(),entry.getValue().toString());
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
                    if (StringUtils.isEmpty(kv[1])){
                        kvMap.put(kv[0],"");
                    }
                    kvMap.put(kv[0],kv[1]);
                }else {
                    if (StringUtils.isNotEmpty(kv[0])){
                        kvMap.put(kv[0],"");
                    }
                }
            }
        }
        return kvMap;
    }


    private Map<String,String> getCheck(String msg,Integer matchEnd){
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
                    kvMap.put(kv[0],"");
                }
                kvMap.put(kv[0],kv[1]);
            }else {
                if (StringUtils.isNotEmpty(kv[0])){
                    kvMap.put(kv[0],"");
                }
            }
        }
        return kvMap;
    }

    private SocketMessage generateMsg(String msg,List<Map<String,String>> error,Integer type,String groupId){
        SocketMessage socketMessage =new SocketMessage();
        socketMessage.setMsg(msg);
        socketMessage.setError(error);
        socketMessage.setType(type);
        socketMessage.setGroupId(groupId);
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

    private Long parseTimestamp(String time){
        LocalDateTime localDateTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        long timeStamp = localDateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
        return timeStamp;
    }

    private boolean isLong(String num){
        try{
            long l = Long.parseLong(num);
        }catch (Exception e){
            return false;
        }
        return true;
    }

    private boolean containsIp(String msg){
        String ipStr = Constants.SRC_TARGET_IP;
        String[] ips = ipStr.split(",");
        for (String ip : ips) {
            if (msg.contains(ip)&&msg.startsWith(ip)){return true;}
        }
        return false;
    }
}
