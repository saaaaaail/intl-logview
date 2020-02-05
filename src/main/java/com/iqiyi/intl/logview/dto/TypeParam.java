package com.iqiyi.intl.logview.dto;

import lombok.Data;

@Data
public class TypeParam {

    private String type;

    private String value;

    private Integer empty;

    private Integer match;

    private Boolean useReg;

    private String reg;
}
