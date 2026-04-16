package org.development.exam_online.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("exam_paper_question_snapshot")
@Data
public class ExamPaperQuestionSnapshot {

    @TableField("exam_id")
    private Long examId;

    @TableField("question_id")
    private Long questionId;

    @TableField("question_score")
    private BigDecimal questionScore;

    @TableField("question_order")
    private Integer questionOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
