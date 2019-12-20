package com.iqiyi.intl.logview.enums;

public enum LogType {

    T_20(20,""),
    T_21(21,"")

    ;

    private Integer code;
    private String name;

    LogType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
