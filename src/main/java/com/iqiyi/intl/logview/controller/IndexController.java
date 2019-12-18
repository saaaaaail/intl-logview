package com.iqiyi.intl.logview.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * @program: intl-logview
 * @description:
 * @author: yangfan
 * @create: 2019/12/17 16:04
 */

@Slf4j
@RestController
public class IndexController {

    @RequestMapping(value = "/index",method = RequestMethod.GET)
    public ModelAndView index(HttpServletRequest request) throws UnknownHostException {

        ModelAndView modelAndView =new ModelAndView("logview");

        String wsUrl = "ws://"+ InetAddress.getLocalHost().getHostAddress()+":"+request.getServerPort()+request.getContextPath()+"/logview";
        log.info("wsUrl:{}",wsUrl);
        modelAndView.addObject("webSocketUrl",wsUrl);
        return modelAndView;
    }
}
