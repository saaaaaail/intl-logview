package com.iqiyi.intl.logview.dto;

import lombok.Data;

import java.util.List;

@Data
public class RuleParam {

    private List<TypeParam> filter;

    private List<TypeParam> rule;
}
