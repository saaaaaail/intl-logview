package com.iqiyi.intl.logview.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iqiyi.intl.logview.dto.XlsCell;
import com.iqiyi.intl.logview.dto.XlsDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelReportUtils<T> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String EXCEL_SUFFIX = ".xlsx";
    private final String CONFIG_SUFFIX = ".json";
    private final String BASE_DIR = "excel";

    int EXCEL_BUFFER_SIZE = 100;

    public Map<String, JSONObject> configMap = new HashMap<>();

    private static ExcelReportUtils INSTANCE;

    private ExcelReportUtils() {
    }

    public static ExcelReportUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ExcelReportUtils();
        }
        return INSTANCE;
    }

    private JSONObject loadJsonConfig(String excel) {
        JSONObject jsonObject = configMap.get(excel);
        if (jsonObject == null) {
            URL fileURL = this.getClass().getResource("/" + BASE_DIR + "/" + excel + CONFIG_SUFFIX);
            if (fileURL == null)
                return null;
            jsonObject = JSONUtil.readToJSON(fileURL);
            configMap.put(excel, jsonObject);
        }
        return jsonObject;
    }


    /**
     * 下载
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void writeExcel(HttpServletRequest request, HttpServletResponse response, String excelType, List<T> list) {
        response.reset();

        JSONObject jsonConfig = loadJsonConfig(excelType);

        if (jsonConfig == null)
            throw new RuntimeException("没有找到类型为\"" + excelType + "\"的相关配置，请检查配置文件。");

        String fileName = jsonConfig.getString("fileName");

        try {
            response.setHeader("Content-Disposition", "attachment;filename=" + getFileDisplay(request, fileName) + EXCEL_SUFFIX);
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("UTF-8");
            reportExcel(response.getOutputStream(), jsonConfig, list);
            response.flushBuffer();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeXlsDto(HttpServletRequest request, HttpServletResponse response, String fileName, XlsDto xlsDto) {
        response.reset();
        try {
            response.setHeader("Content-Disposition", "attachment;filename=" + getFileDisplay(request, fileName) + EXCEL_SUFFIX);
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("UTF-8");
            reportExcel(response.getOutputStream(), xlsDto);
            response.flushBuffer();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 写入excel
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void reportExcel(OutputStream output, JSONObject reportConfig, List<T> list) {
        List<String> fields = (ArrayList) reportConfig.get("fieldName");
        JSONArray heads = reportConfig.getJSONArray("head");
        JSONArray mergeCell = reportConfig.getJSONArray("mergeCell");

        SXSSFWorkbook wb = new SXSSFWorkbook(EXCEL_BUFFER_SIZE);
        Sheet sheet = wb.createSheet("第1页");
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setAlignment(CellStyle.ALIGN_CENTER);
        Font font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerStyle.setFont(font);

        createHeaderRow(sheet, headerStyle, heads);
        createCell(sheet, fields, list);
        addMergedRegion(sheet, mergeCell);
        close(output, wb);

    }


    public String createExcel(String fileName, XlsDto xlsDto) throws IOException {
        String src = "/xls/" + fileName + EXCEL_SUFFIX;
        File file = new File(src);
       /* if (file.exists()) {
            return src;
        }*/
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
        try (OutputStream outputStream = new FileOutputStream(file)) {
            reportExcel(outputStream, xlsDto);
        }
        return src;
    }


    /**
     * 下载
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public String createExcel(String excelType, List<T> list) throws IOException {
        JSONObject jsonConfig = loadJsonConfig(excelType);
        if (jsonConfig == null)
            throw new RuntimeException("没有找到类型为\"" + excelType + "\"的相关配置，请检查配置文件。");
        String src = "/xls/" + excelType + EXCEL_SUFFIX;
        File file = new File(src);
       /* if (file.exists()) {
            return src;
        }*/
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
        try (OutputStream outputStream = new FileOutputStream(file)) {
            reportExcel(outputStream, jsonConfig, list);
        }
        return src;
    }


    /**
     * 下载
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public String createExcel(String excelType, String fileName, List<T> list) throws IOException {
        JSONObject jsonConfig = loadJsonConfig(excelType);
        if (jsonConfig == null)
            throw new RuntimeException("没有找到类型为\"" + excelType + "\"的相关配置，请检查配置文件。");
        String src = "/xls/" + fileName + EXCEL_SUFFIX;
        File file = new File(src);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
        try (OutputStream outputStream = new FileOutputStream(file)) {
            reportExcel(outputStream, jsonConfig, list);
        }
        return src;
    }


    private void addMergedRegion(Sheet sheet, JSONArray mergeCells) {
        if (mergeCells == null || mergeCells.isEmpty()) {
            return;
        }

        mergeCells.forEach(o -> {
            JSONObject cellRangeAddress = (JSONObject) o;
            CellRangeAddress var1 = new CellRangeAddress(cellRangeAddress.getInteger("firstRow"), cellRangeAddress.getInteger("lastRow"), cellRangeAddress.getInteger("firstCol"), cellRangeAddress.getInteger("lastCol"));
            sheet.addMergedRegion(var1);
        });

    }

    private void reportExcel(OutputStream output, XlsDto xlsDto) {
        SXSSFWorkbook wb = new SXSSFWorkbook(EXCEL_BUFFER_SIZE);
        Sheet sheet = wb.createSheet("第1页");
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setAlignment(CellStyle.ALIGN_CENTER);
        Font font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerStyle.setFont(font);

        CellStyle warningCellStyle = wb.createCellStyle();
        warningCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        warningCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        warningCellStyle.setAlignment(CellStyle.ALIGN_CENTER);
        warningCellStyle.setFont(font);


        Row headerRow = sheet.createRow(0);
        createHeaderRow(headerRow, headerStyle, xlsDto);
        createCell(sheet, xlsDto, warningCellStyle);
        close(output, wb);
    }

    private void createHeaderRow(Row row, CellStyle headerStyle, XlsDto xlsDto) {
        int index = 0;
        List<String> headList = xlsDto.getHead();
        for (String head : headList) {
            row.createCell(index).setCellValue(head);
            row.getCell(index).setCellStyle(headerStyle);
            index++;
        }
    }

    private void createCell(Sheet sheet, XlsDto xlsDto, CellStyle warningCellStyle) {
        int rowIndex = 1;
        int cellIndex = 0;
        List<List<XlsCell>> data = xlsDto.getData();
        for (List<XlsCell> rowData : data) {
            Row row = sheet.createRow(rowIndex);
            cellIndex = 0;
            for (XlsCell xlsCell : rowData) {
                Cell cell = row.createCell(cellIndex);
                cell.setCellValue(xlsCell.getValue());
                if (xlsCell.isWarning()) {
                    cell.setCellStyle(warningCellStyle);

                }
                cellIndex++;
            }
            rowIndex++;
        }
    }

    /**
     * 创建头部标题
     */
    @SuppressWarnings("unchecked")
    private void createHeaderRow(Sheet sheet, CellStyle headerStyle, JSONArray heads) {
        if (heads != null) {
            for (int rowIndex = 0; rowIndex < heads.size(); rowIndex++) {
                JSONArray head = heads.getJSONArray(rowIndex);
                Row row = sheet.createRow(rowIndex);
                for (int cellIndex = 0; cellIndex < head.size(); cellIndex++) {
                    String cellValue = head.getString(cellIndex);
                    row.createCell(cellIndex).setCellValue(cellValue);
                    row.getCell(cellIndex).setCellStyle(headerStyle);
                }
            }
        }
    }

    /**
     * 创建单元格
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void createCell(Sheet sheet, List<String> fields, List<T> list) {
        int rowIndex = sheet.getPhysicalNumberOfRows();

        for (T t : list) {
            Row row = sheet.createRow(rowIndex);
            int cellIndex = 0;
            for (String key : fields) {
                String value = "";
                if (t instanceof Map) {
                    value = StringUtils.defaultEmptyStr(((Map) t).get(key));
                } else {
                    String getMethodName = "get" + key.substring(0, 1).toUpperCase() + key.substring(1);
                    try {
                        Method getMethod = t.getClass().getMethod(getMethodName, new Class[]{});
                        Object val = getMethod.invoke(t, new Object[]{});
                        if (val != null)
                            value = val.toString();
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("找不到字段:" + key);
                    }

                }
                Cell cell = row.createCell(cellIndex);
                cell.setCellValue(value);
                cellIndex++;
            }
            rowIndex++;
        }
    }

    /**
     * 关闭IO
     */
    private void close(OutputStream output, SXSSFWorkbook wb) {
        try {
            output.flush();
            wb.write(output);
            output.close();
            wb.dispose();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取显示的文件名
     * 兼容不同的浏览器
     */
    private String getFileDisplay(HttpServletRequest request, String fileName) {
        try {
            if (request.getHeader("USER-AGENT").toLowerCase().indexOf("firefox") != -1)
                return new String(fileName.getBytes("UTF-8"), "ISO8859-1");

            return URLEncoder.encode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "TemplateExcel";
    }

}

