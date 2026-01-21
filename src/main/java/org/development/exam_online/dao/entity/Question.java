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

    @TableField("content")
    private String content;

    // JSON 字段，按需在全局或字段级别配置类型处理器
    @TableField("options")
    private String options;

    @TableField("answer")
    private String answer;

    @TableField("score")
    private BigDecimal score;

    @TableField("category_id")
    private Long categoryId;

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;
    //1.简单 2.普通 3.困难
    @TableField("difficulty")
    private Integer difficulty;

}


