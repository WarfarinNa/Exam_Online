package org.development.exam_online.service.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.Getter;
import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.dao.dto.QuestionExcelDTO;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class QuestionImportListener extends AnalysisEventListener<QuestionExcelDTO> {


    @Getter
    private final List<QuestionExcelDTO> validRows = new ArrayList<>();

    @Getter
    private final List<String> errors = new ArrayList<>();

    private int rowIndex = 1;

    @Override
    public void invoke(QuestionExcelDTO dto, AnalysisContext context) {
        rowIndex++;
        String prefix = "第" + rowIndex + "行: ";

        // 题型校验
        String typeCode = convertType(dto.getType());
        if (typeCode == null) {
            errors.add(prefix + "题型无效，支持：单选题/多选题/判断题/填空题/简答题");
            return;
        }
        dto.setType(typeCode);

        // 题干
        if (!StringUtils.hasText(dto.getStem())) {
            errors.add(prefix + "题干不能为空");
            return;
        }

        // 选择题必须有选项
        if (("single".equals(typeCode) || "multiple".equals(typeCode))
                && !StringUtils.hasText(dto.getOptions())) {
            errors.add(prefix + "选择题必须提供选项");
            return;
        }

        if (!StringUtils.hasText(dto.getAnswer())) {
            errors.add(prefix + "答案不能为空");
            return;
        }

        if (dto.getScore() == null || dto.getScore() <= 0) {
            errors.add(prefix + "分值必须大于0");
            return;
        }

        Integer diff = convertDifficulty(dto.getDifficulty());
        if (diff == null) {
            errors.add(prefix + "难度无效，支持：1/2/3 或 简单/普通/困难");
            return;
        }
        dto.setDifficulty(String.valueOf(diff));

        validRows.add(dto);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
    }

    private String convertType(String type) {
        if (!StringUtils.hasText(type)) return null;
        String t = type.trim();
        return switch (t) {
            case "单选题", "单选" -> "single";
            case "多选题", "多选" -> "multiple";
            case "判断题", "判断" -> "judge";
            case "填空题", "填空" -> "blank";
            case "简答题", "简答" -> "short";
            default -> QuestionType.isValid(t) ? t : null;
        };
    }

    private Integer convertDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) return null;
        String d = difficulty.trim();
        return switch (d) {
            case "简单", "1" -> 1;
            case "普通", "中等", "2" -> 2;
            case "困难", "难", "3" -> 3;
            default -> null;
        };
    }
}
