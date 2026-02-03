package org.development.exam_online.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("question")
@Data
public class Question {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("type")
    private String type;

    @TableField("stem")
    private String stem;

    @TableField("options_json")
    private String optionsJson;

    @TableField("answer_json")
    private String answerJson;

    @TableField("analysis")
    private String analysis;

    @TableField("score")
    private BigDecimal score;

    @TableField("difficulty")
    private Integer difficulty;

    @TableField("category_id")
    private Long categoryId;

    @TableField("knowledge_id")
    private Long knowledgeId;

    @TableField("created_by")
    private Long createdBy;

    @TableField("deleted")
    private Integer deleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

}


