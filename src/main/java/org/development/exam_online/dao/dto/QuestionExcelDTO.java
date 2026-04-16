package org.development.exam_online.dao.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class QuestionExcelDTO {

    @ExcelProperty("题型")
    @ColumnWidth(12)
    private String type;

    @ExcelProperty("题干")
    @ColumnWidth(40)
    private String stem;

    @ExcelProperty("选项")
    @ColumnWidth(50)
    private String options;

    @ExcelProperty("答案")
    @ColumnWidth(30)
    private String answer;

    @ExcelProperty("解析")
    @ColumnWidth(40)
    private String analysis;

    @ExcelProperty("分值")
    @ColumnWidth(8)
    private Double score;

    @ExcelProperty("难度")
    @ColumnWidth(8)
    private String difficulty;

    @ExcelProperty("分类/知识点")
    @ColumnWidth(25)
    private String categoryKnowledge;
}
