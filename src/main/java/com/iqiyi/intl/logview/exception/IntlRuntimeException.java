package com.iqiyi.intl.logview.exception;

public class IntlRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    Integer code = 599;

    public IntlRuntimeException(String message) {
        super(message);
    }

    public IntlRuntimeException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public IntlRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
