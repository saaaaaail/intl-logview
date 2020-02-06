package com.iqiyi.intl.logview.model;

import com.iqiyi.intl.logview.dto.TypeParam;
import lombok.Data;

import java.util.List;

@Data
public class Check {

    private Long id;

    private String userName;

    private String url;

    private Integer urlMatch;

    private Boolean useReg;

    private String reg;

    private List<List<TypeParam>> rules;
}
