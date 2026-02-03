package org.development.exam_online.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("exam_answer")
@Data
public class ExamAnswer {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("record_id")
    private Long recordId;

    @TableField("question_id")
    private Long questionId;

    @TableField("user_answer")
    private String userAnswer;

    @TableField("score")
    private BigDecimal score;

    @TableField("created_by")
    private Long createdBy;

    @TableField("deleted")
    private Integer deleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}


