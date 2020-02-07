package com.iqiyi.intl.logview.enums;

/**
 * @program: intl-logview
 * @description:
 * @author: yangfan
 * @create: 2019/12/19 16:46
 */

public enum  TypeEnums {

    PAUSE_OPERATE(0,"暂停操作"),
    NOT_CHECK_MSG_OPERATE(1,"未检查的操作"),
    WRONG_MEG_OPERATE(2,"错误消息操作"),
    RIGHT_MSG_OPERATE(3,"正确消息操作"),
    ENABLE_FILTER_BTN_OPERATE(4,"筛选按钮可用操作"),
    CLEAR_MSG_OPERATE(5,"清屏操作"),
    HEART_MSG_OPERATE(6,"心跳操作"),
    CHECK_WRONG_MSG_OPERATE(7,"用户名匹配不正确操作"),
    CHECK_RIGHT_MSG_OPERATE(8,"用户名匹配正确操作"),
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
