package com.iqiyi.intl.logview.util;

import com.alibaba.fastjson.JSONObject;
import com.iqiyi.intl.logview.exception.IntlHttpException;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * httpClient定制类，单例注入到spring，destory的时候调用close方法;
 * 内部线程清除过期空闲连接;外部使用方调用doGet,doPost
 */
@Component
public class HttpUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger("api_client");

    private volatile Object lock = new Object();
    private volatile CloseableHttpClient httpClient;
    private volatile boolean isClose = true;
    private volatile String mimeType = "application/json";
    private volatile String charset = "UTF-8";

    /**
     * 超时值
     */
    private int timeout = 5000;

    /**
     * 最大连接数
     */
    private int maxTotal = 2000;
    /**
     * 单Url最大连接数
     */
    private int maxPerRoute = 2000;
    /**
     * 扫描连接池间隔
     */
    private int scanTime = 5000;
    /**
     * 多久空闲的连接将被清除
     */
    private int idleTime = 30;

    private CookieStore cookieStore = new BasicCookieStore();

    /**
     * httpClient初始化
     */
    private void initHttpClient() {
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxTotal);
        cm.setDefaultMaxPerRoute(maxPerRoute);
        //接入rover的埋点工具
//        httpClient = TracingHttpClientBuilder.create(httpTracing)
//                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
//                .setConnectionManager(cm).setDefaultCookieStore(cookieStore).build();
        // 不允许重试
        httpClient = HttpClients.custom()
               .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                .setConnectionManager(cm).setDefaultCookieStore(cookieStore).build();
        isClose = false;
        // 清除过期和空闲连接线程
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!isClose) {
                        Thread.sleep(scanTime);
                        cm.closeExpiredConnections();
                        cm.closeIdleConnections(idleTime, TimeUnit.MILLISECONDS);
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("initHttpClient catch InterruptedException", e);
                }
            }
        });
        t.setDaemon(true);
        t.setName("ClearHttpThread");
        t.start();
    }

    /**
     * 获取httpClient单例
     */
    private CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (lock) {
                if (httpClient == null) {
                    initHttpClient();
                }
            }
        }
        return httpClient;
    }

    public String doGet(String url, Map<String, String> params) {
        return doGet(url, null, params);
    }

    public String doGet(String url, Header[] headers, Map<String, String> params) {
        String strResponse = null;
        try {
            long start = System.currentTimeMillis();

            List<NameValuePair> nvps = new ArrayList<>();
            if (params != null) {
                Set<String> keySet = params.keySet();
                for (String key : keySet) {
                    nvps.add(new BasicNameValuePair(key, params.get(key)));
                }
            }

            if (!CollectionUtils.isEmpty(params)) {
                url = url + "?" + EntityUtils.toString(new UrlEncodedFormEntity(nvps, "UTF-8"));
            }

            HttpGet httpGet = new HttpGet(url);
            RequestConfig config = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).setConnectionRequestTimeout(timeout)
                    .build();
            httpGet.setConfig(config);

            if (headers != null && headers.length > 0) {
                httpGet.setHeaders(headers);
            }

            CloseableHttpResponse response = getHttpClient().execute(httpGet);
            int code = response.getStatusLine().getStatusCode();
            long cost = System.currentTimeMillis() - start;
            if (code == 200 || code == 201) {
                strResponse = EntityUtils.toString(response.getEntity(), charset);
                LOGGER.info("doGet method successfully get result, url is: " + url + " ,doGet response is: " + strResponse + ", cost:" + cost);
            } else {
                throw new IntlHttpException("doGet method response code expects 2xx, but got " + code + ", url is: " + url + ", response text is: " + EntityUtils.toString(response.getEntity(), charset) + ", cost:" + cost);
            }
            response.close();
        } catch (Exception e) {
            LOGGER.error("doGet method catch Exception" + ", url is: " + url, e);
            throw new IntlHttpException("doGet method throw Exception" + ", url is: " + url, e);
        }
        return strResponse;

    }

    public String doGet(String url) {
        return this.doGet(url, null);
    }

    public void storeCookies(Cookie[] cookies, String domain) {
        if (cookies.length > 0) {
            for (Cookie cookie : cookies) {
                BasicClientCookie bCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
                cookieStore.addCookie(bCookie);
                bCookie.setDomain(domain);
                cookieStore.addCookie(bCookie);
            }
        }
    }

    public String doPost(String data, String url) {
        long start = System.currentTimeMillis();
        HttpPost httpPost = new HttpPost(url);
        RequestConfig config = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).setConnectionRequestTimeout(timeout)
                .build();
        httpPost.setConfig(config);
        String strResponse = null;
        try {
            ContentType contentType = ContentType.create(getMimeType(), getCharset());
            httpPost.setEntity(new StringEntity(data, contentType));
            CloseableHttpResponse response = getHttpClient().execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            strResponse = response.getEntity() == null ? null : EntityUtils.toString(response.getEntity());
            long cost = System.currentTimeMillis() - start;
            LOGGER.info("doPost1 method successfully get result, url is: " + url + "," + "body is:" + data +
                    ", doPost1 response code is:" + code + ", content is: " + strResponse + ", cost:" + cost);
            response.close();
        } catch (Exception e) {
            throw new IntlHttpException("doPost1 method throw Exception url is: " + url + ",body is:" + JSONObject.toJSONString(data), e);
        }
        return strResponse;
    }

    public String doPost(Map<String, String> params, String url) {
        long start = System.currentTimeMillis();
        HttpPost httpPost = new HttpPost(url);
        RequestConfig config = RequestConfig.custom().setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout).build();
        httpPost.setConfig(config);
        String strResponse = null;
        try {
            List<NameValuePair> nvps = new ArrayList<>();
            Set<String> keySet = params.keySet();
            for (String key : keySet) {
                nvps.add(new BasicNameValuePair(key, params.get(key)));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, getCharset()));
            CloseableHttpResponse response = getHttpClient().execute(httpPost);
            int code = response.getStatusLine().getStatusCode();
            long cost = System.currentTimeMillis() - start;
            if ((code >= 200) & (code < 300)) {
                strResponse = response.getEntity() == null ? null : EntityUtils.toString(response.getEntity(), charset);
            } else {
                throw new IntlHttpException("doPost2 method response code expects 2xx, but got " + code + ", url is: " + url + ", response text is: " + EntityUtils.toString(response.getEntity(), charset) + ", cost:" + cost);
            }
            LOGGER.info("doPost2 method successfully get result, url is: " + url + ",body is:" + JSONObject.toJSONString(params) +
                    ", doPost2 response code is:" + code + ", content is: " + strResponse + ", cost:" + cost);
            response.close();
        } catch (Exception e) {
            throw new IntlHttpException("doPost2 method throw Exception url is: " + url + ",body is:" + JSONObject.toJSONString(params), e);
        }
        return strResponse;
    }

    /**
     * 关闭httpClient实例
     */
    public void close() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                httpClient = null;
                isClose = true;
                throw new IntlHttpException("close error", e);
            }
            httpClient = null;
        }
        isClose = true;
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setClose(boolean isClose) {
        this.isClose = isClose;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public void setMaxPerRoute(int maxPerRoute) {
        this.maxPerRoute = maxPerRoute;
    }

    public void setScanTime(int scanTime) {
        this.scanTime = scanTime;
    }

    public void setIdleTime(int idleTime) {
        this.idleTime = idleTime;
    }

    public String getMimeType() {
        if (mimeType != null && mimeType.length() > 0) {
            return mimeType;
        } else {
            return "text/plain";
        }
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getCharset() {
        if (charset != null && charset.length() > 0) {
            return charset;
        } else {
            return "UTF-8";
        }
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

}
