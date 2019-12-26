package com.iqiyi.intl.logview.websocket;

import com.alibaba.fastjson.JSONObject;
import com.iqiyi.intl.logview.watch.PoolMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: intl-logview
 * @description:
 * @author: yangfan
 * @create: 2019/12/17 10:56
 */
@Component
@ServerEndpoint("/logview")
@Slf4j
public class WebSocketServer {

    private static ApplicationContext applicationContext;

    PoolMonitorService poolMonitorService;

    private static Map<Session, SocketMessage> sessionMap = new ConcurrentHashMap<>();

    /**
     * 客户端建立连接
     * @param session
     */
    @OnOpen
    public void onOpen(Session session){
        poolMonitorService = applicationContext.getBean(PoolMonitorService.class);
        sessionMap.put(session,new SocketMessage());
    }

    /**
     * 消息交互
     */
    @OnMessage
    public void onMessage(Session session,String jsonStr) throws IOException {
        log.info("接收到客户端的消息");
        SocketMessage socketMessage = JSONObject.parseObject(jsonStr, SocketMessage.class);
        if (socketMessage.getMsg().equals("pause")){
            poolMonitorService.pauseThread(session);
        }else if (socketMessage.getMsg().equals("watch")){
            sessionMap.put(session,socketMessage);
            poolMonitorService.readFileScheduledStart(session,sessionMap,socketMessage);
        }else if (socketMessage.getMsg().equals("rewatch")){
            poolMonitorService.readFileScheduledWithFilter(session,sessionMap,socketMessage);
        }
    }

    /**
     * 关闭连接
     * @param session
     */
    @OnClose
    public void onClose(Session session) {
        sessionMap.remove(session);
        poolMonitorService.closePool(session);
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 抛通信异常
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session,Throwable error){
        error.printStackTrace();
    }

    public static void setApplicationContext(ApplicationContext context){
        applicationContext = context;
    }


}
