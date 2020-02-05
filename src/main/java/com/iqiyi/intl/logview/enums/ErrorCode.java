package com.iqiyi.intl.logview.enums;

public enum ErrorCode {

    INVALID_PARA(400, "参数缺失"),
    SYSTEM_ERROR_CODE(500, "系统内部错误"),
    NPE_ERROR_CODE(5001, "NPE错误");


    private String name;
    private int index;

    // 构造方法
    private ErrorCode(int index, String name) {
        this.name = name;
        this.index = index;
    }

    // 普通方法
    public static String getName(int index) {
        for (ErrorCode c : ErrorCode.values()) {
            if (c.getIndex() == index) {
                return c.name;
            }
        }
        return null;
    }

    public static ErrorCode getByIndex(int index) {
        for (ErrorCode c : ErrorCode.values()) {
            if (c.getIndex() == index) {
                return c;
            }
        }
        return null;
    }

    // get set 方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}

