package org.development.exam_online.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("exam_paper")
@Data
public class ExamPaper {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("build_type")
    private Integer buildType;

    @TableField("rule_json")
    private String ruleJson;

    @TableField("total_score")
    private BigDecimal totalScore;

    @TableField("duration")
    private Integer duration;

    @TableField("created_by")
    private Long createdBy;

    @TableField("deleted")
    private Integer deleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}


