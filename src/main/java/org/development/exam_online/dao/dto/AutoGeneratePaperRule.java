package org.development.exam_online.dao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 自动组卷规则DTO
 * 用于自动组卷时的规则传递
 */
@Schema(description = "自动组卷规则", example = "{\n" +
        "  \"typeRules\": {\n" +
        "    \"single\": {\"count\": 20, \"score\": 2.0},\n" +
        "    \"multiple\": {\"count\": 10, \"score\": 3.0},\n" +
        "    \"judge\": {\"count\": 10, \"score\": 1.0},\n" +
        "    \"short\": {\"count\": 5, \"score\": 10.0}\n" +
        "  },\n" +
        "  \"difficultyMode\": \"RATIO\",\n" +
        "  \"difficultyRatio\": {\"1\": 0.3, \"2\": 0.5, \"3\": 0.2},\n" +
        "  \"categoryIds\": [1, 2, 3],\n" +
        "  \"allowDuplicate\": false,\n" +
        "  \"fallbackMode\": \"BALANCE\"\n" +
        "}")
@Data
public class AutoGeneratePaperRule {

    /**
     * 题型规则：题型代码 -> 题型规则详情
     * 题型代码参考QuestionType枚举：single(单选题)、multiple(多选题)、judge(判断题)、blank(填空题)、short(简答题)
     */
    @Schema(description = "题型规则（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "{\n" +
            "    \"single\": {\"count\": 20, \"score\": 2.0},\n" +
            "    \"multiple\": {\"count\": 10, \"score\": 3.0},\n" +
            "    \"judge\": {\"count\": 10, \"score\": 1.0},\n" +
            "    \"blank\": {\"count\": 5, \"score\": 2.0},\n" +
            "    \"short\": {\"count\": 5, \"score\": 10.0}\n" +
            "  }")
    private Map<String, TypeRule> typeRules;

    /**
     * 难度规则模式
     * QUOTA: 配额模式，直接指定每个难度的题量
     * RATIO: 比例模式，按比例计算每个难度的题量
     */
    @Schema(description = "难度规则模式（可选）", example = "RATIO", 
            allowableValues = {"QUOTA", "RATIO"},
            defaultValue = "QUOTA")
    private DifficultyMode difficultyMode;

    /**
     * 难度配额（当difficultyMode=QUOTA时使用）
     */
    @Schema(description = "难度配额（当difficultyMode=QUOTA时必填）。key: 难度等级(1-简单, 2-普通, 3-困难), value: 该难度的题目数量", 
            example = "{\"1\": 6, \"2\": 10, \"3\": 4}")
    private Map<Integer, Integer> difficultyQuota;

    /**
     * 难度比例（当difficultyMode=RATIO时使用）
     */
    @Schema(description = "难度比例（当difficultyMode=RATIO时必填）。key: 难度等级(1-简单, 2-普通, 3-困难), value: 该难度的比例(0.0-1.0)，总和必须接近1.0", 
            example = "{\"1\": 0.3, \"2\": 0.5, \"3\": 0.2}")
    private Map<Integer, Double> difficultyRatio;

    /**
     * 分类ID列表（从哪些分类中选择题目）
     */
    @Schema(description = "分类ID列表（可选）。如果不提供，则从所有分类中选择题目", example = "[1, 2, 3]")
    private List<Long> categoryIds;

    /**
     * 是否允许重复题目
     */
    @Schema(description = "是否允许重复题目（可选）。true: 允许同一题目被多次选择；false: 确保每道题只选择一次（默认）", 
            example = "false", defaultValue = "false")
    private Boolean allowDuplicate;

    /**
     * 随机种子（可选，用于保证结果可复现）
     */
    @Schema(description = "随机种子（可选）。如果提供，相同种子会产生相同的结果；不提供则每次结果都不同", example = "12345")
    private Long randomSeed;

    /**
     * 兜底策略（当某个难度的题目数量不足时）
     */
    @Schema(description = "兜底策略（可选，默认ERROR）。BALANCE: 自动平衡，将不足的配额分配给其他难度；IGNORE: 忽略难度限制，从其他难度中补足；ERROR: 报错，不允许组卷", 
            example = "BALANCE", 
            allowableValues = {"BALANCE", "IGNORE", "ERROR"},
            defaultValue = "ERROR")
    private FallbackMode fallbackMode;

    /**
     * 题型规则详情
     */
    @Schema(description = "题型规则详情")
    @Data
    public static class TypeRule {
        
        @Schema(description = "题目数量（必填）。该题型的题目数量，必须大于0", example = "20", 
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        private Integer count;
        
        @Schema(description = "每道题的分值（可选）。该题型每道题的默认分值，如果不提供则使用题目的默认分值", example = "2.0")
        private Double score;
    }

    /**
     * 难度规则模式枚举
     */
    @Schema(description = "难度规则模式")
    public enum DifficultyMode {
        @Schema(description = "配额模式：直接指定每个难度的题量，例如：简单6道，普通10道，困难4道")
        QUOTA,
        
        @Schema(description = "比例模式：按比例计算每个难度的题量，例如：简单30%，普通50%，困难20%")
        RATIO
    }

    /**
     * 兜底策略枚举
     */
    @Schema(description = "兜底策略")
    public enum FallbackMode {
        @Schema(description = "自动平衡：将不足的配额分配给其他难度")
        BALANCE,
        
        @Schema(description = "忽略难度限制：从其他难度中补足")
        IGNORE,
        
        @Schema(description = "报错：不允许组卷，直接抛出异常")
        ERROR
    }
}