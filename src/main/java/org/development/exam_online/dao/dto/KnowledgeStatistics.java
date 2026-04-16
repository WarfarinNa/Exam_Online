package org.development.exam_online.dao.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class KnowledgeStatistics {
    
    private Long knowledgeId;
    
    private String knowledgeName;
    
    private Integer total;
    
    private Integer wrong;
    
    private BigDecimal accuracy;
}
