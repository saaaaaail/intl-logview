package com.iqiyi.intl.logview.enums;

public enum  MatchEnums {

    BLURRY(0,"模糊"),
    EXACT(1,"精确"),
    ;


    private Integer code;
    private String name;

    MatchEnums(Integer code,String name){
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
