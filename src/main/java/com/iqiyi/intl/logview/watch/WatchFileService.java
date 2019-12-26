package com.iqiyi.intl.logview.watch;

import com.iqiyi.intl.logview.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

/**
 * @program: intl-logview
 * @description:
 * @author: yangfan
 * @create: 2019/12/25 16:00
 */

@Service
@Slf4j
public class WatchFileService {

    @Autowired
    @Qualifier("localCache")
    private Cache cache;



}
