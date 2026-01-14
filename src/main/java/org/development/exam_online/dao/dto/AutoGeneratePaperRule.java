package org.development.exam_online.dao.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 自动组卷规则DTO
 * 用于自动组卷时的规则传递
 */
@Data
public class AutoGeneratePaperRule {

    /**
     * 题型规则：题型代码 -> 题量
     * 例如：{"single": 10, "multiple": 5, "judge": 10}
     */
    private Map<String, Integer> typeRules;

    /**
     * 难度规则：难度等级 -> 比例（0-1之间的小数）
     * 例如：{"easy": 0.3, "medium": 0.5, "hard": 0.2}
     * 注意：如果Question实体中没有难度字段，这个可能暂时用不到
     */
    private Map<String, Double> difficultyRules;

    /**
     * 分类ID列表（从哪些分类中选择题目）
     */
    private List<Long> categoryIds;

    /**
     * 是否允许同一题目的多个变体
     */
    private Boolean allowDuplicate;

    /**
     * 随机种子（可选，用于保证结果可复现）
     */
    private Long randomSeed;
}

