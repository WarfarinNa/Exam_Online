package org.development.exam_online.dao.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TemplateGenerateRequest {

    @NotBlank(message = "试卷名称不能为空")
    private String name;

    private String description;

    private Integer duration;

    @NotBlank(message = "模板代码不能为空")
    private String templateCode;

    private List<Long> categoryIds;

    private List<Long> knowledgeIds;

    private Map<String, TypeRule> typeRules;

    private Map<Integer, Double> difficultyRatio;

    @Data
    public static class TypeRule {
        private Integer count;

        private Double score;
    }
}
