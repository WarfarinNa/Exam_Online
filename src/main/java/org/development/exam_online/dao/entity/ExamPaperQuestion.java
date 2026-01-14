package org.development.exam_online.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@TableName("exam_paper_question")
@Data
public class ExamPaperQuestion {

    @TableField("paper_id")
    private Long paperId;

    @TableField("question_id")
    private Long questionId;

    @TableField("score")
    private BigDecimal score;
}


