package org.development.exam_online.dao.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TypeStatistics {
    
    private String typeName;
    
    private Integer total;
    
    private Integer wrong;
    
    private BigDecimal accuracy;
}
