package com.iqiyi.intl.logview;

import com.iqiyi.intl.logview.websocket.WebSocketServer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @program: intl-logview
 * @description:
 * @author: yangfan
 * @create: 2019/12/17 10:40
 */

@SpringBootApplication
@MapperScan("com.iqiyi.intl.logview.mapper")
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext a = SpringApplication.run(Application.class, args);
        WebSocketServer.setApplicationContext(a);
    }
}
