package com.iqiyi.intl.logview.watch;

import com.alibaba.fastjson.JSONObject;
import com.iqiyi.intl.logview.cache.Cache;
import com.iqiyi.intl.logview.constant.Constants;
import com.iqiyi.intl.logview.websocket.SocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.websocket.Session;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @program: intl-logview
 * @description:
 * @author: yangfan
 * @create: 2019/12/17 11:16
 */

@Service
@Slf4j
public class WatchService {

    @Autowired
    @Qualifier("localCache")
    private Cache cache;

    private Long pointer;

    private boolean isPause = false;

    public void readFileSchedules(Session host,Map<Session, SocketMessage> sessionMap) throws IOException {
        log.info("校验是否第一次读取文件");
        synchronized (this){
            String visitsKey = Constants.VISIT_COUNT+host.getId();
            Integer visitCount = (Integer)cache.get(visitsKey);
            if (visitCount!=null){
                return;
            }
            pointer=0L;
            cache.add(visitsKey,1);
        }


        RandomAccessFile file = new RandomAccessFile(Constants.WATCH_FILE_PATH,"r");
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        log.info("开始读取文件");
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!isPause){
                try {

                    file.seek(pointer);
                    StringBuilder msg = new StringBuilder();
                    String msgline = null;
                    while ((msgline = file.readLine()) != null) {
                        msg.append(new String(msgline.getBytes(StandardCharsets.ISO_8859_1)));
                    }
                    if (StringUtils.isNotEmpty(msg)){
                        //log.info(msg.toString());
                        sendMessage(sessionMap, msg.toString(), 1);
                    }
                    pointer=file.getFilePointer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
        String poolKey = Constants.SCHEDULED_POOL+host.getId();
        cache.add(poolKey,scheduledExecutorService);
    }


    public void pauseThread(Session host) throws IOException {
        String visitsKey = Constants.VISIT_COUNT+host.getId();
        Integer visitCount = (Integer)cache.get(visitsKey);
        if (visitCount==null){return;}
        SocketMessage socketMessage = new SocketMessage();
        socketMessage.setType(0);
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
        Object o = cache.get(poolKey);
        if (o!=null){
            isPause=false;
            cache.remove(poolKey);
            ((ScheduledExecutorService)o).shutdown();
            log.info("关闭用户的线程池");
        }
    }

    public void sendMessage(Map<Session, SocketMessage> sessionMap,String msg,Integer type){
        sessionMap.forEach((session, socketMessage) -> {
            socketMessage.setMsg(msg);
            socketMessage.setType(type);
            if (socketMessage.getUser()==null){
                socketMessage.setUser(UUID.randomUUID().toString().replace("-",""));
            }
            try {
                session.getBasicRemote().sendText(JSONObject.toJSONString(socketMessage));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private Long getLineNum(RandomAccessFile file,Integer endline){
        Integer allCountLine=0;
        Long point = 0L;
        try {
            String msgline = null;
            file.seek(point);
            while (((msgline = file.readLine()) != null)) {
                allCountLine++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("总行数:{}",allCountLine);
        if (allCountLine<=endline){return 0L;}

        Integer startCountLine = allCountLine-endline;
        log.info("跳过的行数:{}",startCountLine);
        allCountLine = 0;
        try{
            String msgline = null;
            file.seek(point);
            while (((msgline = file.readLine()) != null)) {
                allCountLine++;
                if (allCountLine>startCountLine){
                    break;
                }
            }
            point = file.getFilePointer();
        }catch (IOException e){
            e.printStackTrace();
        }
        log.info("起始pointer:{}",point);

        return point;
    }

}
