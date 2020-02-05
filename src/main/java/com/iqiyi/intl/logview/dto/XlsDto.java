package com.iqiyi.intl.logview.dto;

import java.util.List;

/**
 * Created by zhuxh on 16/10/28.
 */
public class XlsDto {

    private List<String> head;

    private List<List<XlsCell>> data;

    private String fileName;

    public XlsDto(String fileName, List<String> head, List<List<XlsCell>> data) {
        this.head = head;
        this.data = data;
        this.fileName = fileName;
    }

    public List<String> getHead() {
        return head;
    }

    public void setHead(List<String> head) {
        this.head = head;
    }

    public List<List<XlsCell>> getData() {
        return data;
    }

    public void setData(List<List<XlsCell>> data) {
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
