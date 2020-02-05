package com.iqiyi.intl.logview.base;

import com.alibaba.fastjson.annotation.JSONField;
import org.springframework.http.HttpStatus;

public class Result<T> {

    @JSONField(ordinal = 1, name = "request_id")
    private String requestId;
    @JSONField(ordinal = 2)
    private Long timestamp;
    @JSONField(ordinal = 3)
    private Integer code;
    @JSONField(ordinal = 4)
    private String message;
    @JSONField(ordinal = 5)
    private T data;

    @JSONField(serialize = false)
    private Integer httpStatus = 200;

    public Result() {
        super();
        this.timestamp = System.currentTimeMillis();
    }

    public void setFailResult(Integer code, String message) {
        this.setFailResult(HttpStatus.INTERNAL_SERVER_ERROR.value(), code, message);
    }

    public void setFailResult(Integer httpStatus, Integer code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public void setSuccessResult(T rst) {
        this.setSuccessResult(200, 0, rst);
    }

    public void setSuccessResult(Integer httpStatus, Integer code, T rst) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.data = rst;
    }

    public void setSuccessResult(Integer httpStatus, T rst) {
        this.setSuccessResult(httpStatus, 0, rst);
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public void setResult(Integer code, T rst, String message) {
        this.code = code;
        this.message = message;
        this.data = rst;
    }
}
