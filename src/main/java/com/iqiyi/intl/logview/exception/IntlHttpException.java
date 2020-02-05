package com.iqiyi.intl.logview.exception;

public class IntlHttpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    Integer code = 555;

    public IntlHttpException(String message) {
        super(message);
    }

    public IntlHttpException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public IntlHttpException(String message, Throwable cause) {
        super(message, cause);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
