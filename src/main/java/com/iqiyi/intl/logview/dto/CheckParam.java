package com.iqiyi.intl.logview.dto;

import lombok.Data;

import java.util.List;

@Data
public class CheckParam {

    private String userName;

    private String url;

    private Integer urlMatch;

    private Boolean useReg;

    private String reg;

    private List<List<TypeParam>> rules;
}
