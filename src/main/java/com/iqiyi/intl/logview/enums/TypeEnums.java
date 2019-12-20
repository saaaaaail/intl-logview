package com.iqiyi.intl.logview.enums;

/**
 * @program: intl-logview
 * @description:
 * @author: yangfan
 * @create: 2019/12/19 16:46
 */

public enum  TypeEnums {

    PAUSE_OPERATE(0,"暂停操作"),
    MESSAGE_OPERATE(1,"发消息操作"),
    WRONG_MEG_OPERATE(2,"错误消息操作")
    ;
    private Integer code;
    private String name;

    TypeEnums(Integer code, String name) {
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
