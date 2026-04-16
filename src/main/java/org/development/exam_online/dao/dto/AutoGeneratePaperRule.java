package org.development.exam_online.dao.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AutoGeneratePaperRule {

    private Map<String, TypeRule> typeRules;

    private DifficultyMode difficultyMode;

    private Map<Integer, Integer> difficultyQuota;

    private Map<Integer, Double> difficultyRatio;

    private List<Long> categoryIds;

    private List<Long> knowledgeIds;

    private Boolean allowDuplicate;

    private Long randomSeed;

    private FallbackMode fallbackMode;

    @Data
    public static class TypeRule {
        
        private Integer count;
        
        private Double score;
    }

    public enum DifficultyMode {
        QUOTA,
        
        RATIO
    }

    public enum FallbackMode {
        BALANCE,
        
        IGNORE,
        
        ERROR
    }
}