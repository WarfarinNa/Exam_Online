package org.development.exam_online.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_analysis_report")
public class AiAnalysisReport {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String reportType;
    
    private String examIds;
    
    private String analysisData;
    
    private String aiReport;
    
    private String modelName;
    
    private Integer tokenUsage;
    
    private Integer generationTime;
    
    private LocalDateTime createdTime;
    
    private LocalDateTime updatedTime;
    
    private Integer deleted;
}
