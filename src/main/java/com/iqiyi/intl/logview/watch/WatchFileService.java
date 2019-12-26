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
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
        }

        //清屏
        clearMsg(host);

        FilterParams params = socketMessage.getParams();
        String pattern = socketMessage.getPattern();
        pointer = firstReadFile(host,params,pattern);
        //筛选按钮可用
        enableFilterBtnMsg(host);

        //开始实时读取文件
        startMonitor(host,sessionMap,params,pattern);

    }

    public void startMonitor(Session session,Map<Session, SocketMessage> sessionMap,FilterParams params,String pattern) throws IOException {
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
                            if (Constants.WATCH_FILE.equals(pollEvent.context().toString())){
                                readFile(sessionMap,params,pattern);
                            }
                        }
                        watchKey.reset();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    closeWatchService(session);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    public void readFile(Map<Session, SocketMessage> sessionMap,FilterParams params,String pattern) throws IOException {
        RandomAccessFile file = null;
        List<String> result = new ArrayList<>();
        try {
            file = new RandomAccessFile(Constants.WATCH_FILE_PATH,"r");
            file.seek(pointer);
            String msgline = null;
            while ((msgline = file.readLine())!=null){
                String msg = new String(msgline.getBytes(StandardCharsets.ISO_8859_1),StandardCharsets.UTF_8).trim();
                if (StringUtils.isNotEmpty(msg)){
                    result.add(msg);
                }
            }
            pointer = file.getFilePointer();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            if (file!=null){
                file.close();
            }
        }

        for (String msg:result){
            sendMessageToAll(sessionMap,msg,params,pattern);
        }
    }

    /**
     * 首先读取历史记录
     * @param session
     * @return
     */
    private Long firstReadFile(Session session, FilterParams params, String pattern){
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
            return generateMsg(msg,null, TypeEnums.MESSAGE_OPERATE.getCode());
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
