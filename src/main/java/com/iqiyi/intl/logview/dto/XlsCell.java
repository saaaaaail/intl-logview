package com.iqiyi.intl.logview.dto;

/**
 * Created by yangli on 2019/6/25.
 */
public class XlsCell {

    private String value;

    private boolean isWarning;

    public XlsCell(String value) {
        this.value = value;
    }

    public XlsCell(String value, boolean isWarning) {
        this.value = value;
        this.isWarning = isWarning;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isWarning() {
        return isWarning;
    }

    public void setWarning(boolean warning) {
        isWarning = warning;
    }
}
