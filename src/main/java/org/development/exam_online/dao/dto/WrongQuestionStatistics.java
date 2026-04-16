package org.development.exam_online.dao.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class WrongQuestionStatistics {
    
    private Long userId;
    
    private String userName;
    
    private Integer examCount;
    
    private Integer totalQuestions;
    
    private Integer wrongQuestions;
    
    private BigDecimal accuracy;
    
    private Map<String, TypeStatistics> byQuestionType;
    
    private List<KnowledgeStatistics> byKnowledge;
    
    private List<WrongQuestionDetail> wrongQuestionDetails;
}
