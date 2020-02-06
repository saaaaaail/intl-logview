package com.iqiyi.intl.logview.model;

import lombok.Data;

@Data
public class Type {

    private Long id;

    private Long ruleId;

    private String type;

    private String value;

    private Integer empty;

    private Integer match;

    private Boolean useReg;

    private String reg;
}
