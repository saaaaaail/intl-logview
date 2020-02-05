package com.iqiyi.intl.logview.exception;

import com.iqiyi.intl.logview.base.Result;
import com.iqiyi.intl.logview.enums.ErrorCode;
import com.iqiyi.intl.logview.util.CorsUtils;
import com.iqiyi.intl.logview.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@Component
@ControllerAdvice
public class GlobalErrorHandler {

    private static final Logger ERROR_LOGGER = LoggerFactory.getLogger(GlobalErrorHandler.class);

    private static final String REQUEST_KEY = "request_id";

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<String> exceptionHandler(HttpServletRequest request, Exception e) {
        Result result = new Result();
        if (MDC.get(REQUEST_KEY) != null) {
            result.setRequestId(MDC.get(REQUEST_KEY));
        }
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json;charset=UTF-8");
        CorsUtils.addCorsHeader(request, headers);
        try {
            if (e instanceof MissingServletRequestParameterException) {
                ERROR_LOGGER.error("url:" + request.getRequestURI() + ", query string:" + request.getQueryString(), e);
                result.setFailResult(ErrorCode.INVALID_PARA.getIndex(), ErrorCode.INVALID_PARA.getName());
                return new ResponseEntity<>(JSONUtil.toJSONString(result), headers, HttpStatus.BAD_REQUEST);
            } else if (e instanceof NullPointerException) {
                ERROR_LOGGER.error("url:" + request.getRequestURI() + ", query string:" + request.getQueryString(), e);
                result.setFailResult(ErrorCode.NPE_ERROR_CODE.getIndex(), ErrorCode.NPE_ERROR_CODE.getName());
                return new ResponseEntity<>(JSONUtil.toJSONString(result), headers, HttpStatus.INTERNAL_SERVER_ERROR);
            } else if (e instanceof MethodArgumentNotValidException) {
                ERROR_LOGGER.error("url:" + request.getRequestURI() + ", query string:" + request.getQueryString(), e);
                String paraError = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors().stream().map(a -> a.getDefaultMessage()).collect(Collectors.joining(","));
                result.setFailResult(ErrorCode.INVALID_PARA.getIndex(), ErrorCode.INVALID_PARA.getName() + ":" + paraError);
                return new ResponseEntity<>(JSONUtil.toJSONString(result), headers, HttpStatus.BAD_REQUEST);
            } else if (e instanceof BindException) {
                ERROR_LOGGER.error("url:" + request.getRequestURI() + ", query string:" + request.getQueryString(), e);
                String paraError = ((BindException) e).getBindingResult().getFieldErrors().stream().map(a -> a.getDefaultMessage()).collect(Collectors.joining(","));
                result.setFailResult(ErrorCode.INVALID_PARA.getIndex(), ErrorCode.INVALID_PARA.getName() + ":" + paraError);
                return new ResponseEntity<>(JSONUtil.toJSONString(result), headers, HttpStatus.BAD_REQUEST);
            } else {
                ERROR_LOGGER.error("url:" + request.getRequestURI() + ", query string:" + request.getQueryString() + ", msg:" + e.getMessage(), e);
                result.setFailResult(ErrorCode.SYSTEM_ERROR_CODE.getIndex(), e.getMessage() == null ? "No message available" : e.getMessage());
                return new ResponseEntity<>(JSONUtil.toJSONString(result), headers, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } finally {
            MDC.remove(REQUEST_KEY);
        }
    }


}

