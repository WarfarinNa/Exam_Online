package org.development.exam_online.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("exam_record")
@Data
public class ExamRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("exam_id")
    private Long examId;

    @TableField("user_id")
    private Long userId;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("submit_time")
    private LocalDateTime submitTime;

    @TableField("total_score")
    private BigDecimal totalScore;

    @TableField("objective_score")
    private BigDecimal objectiveScore;

    @TableField("subjective_score")
    private BigDecimal subjectiveScore;

    @TableField("status")
    private String status;
}


