package org.development.exam_online.dao.dto;

import lombok.Data;

@Data
public class WrongQuestionDetail {
    
    private Long questionId;
    
    private String stem;
    
    private String type;
    
    private String typeName;
    
    private String knowledge;
    
    private String userAnswer;
    
    private String correctAnswer;
    
    private String examName;
    
    private Long examId;
}
