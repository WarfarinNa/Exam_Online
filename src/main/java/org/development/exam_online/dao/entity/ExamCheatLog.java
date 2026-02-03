package org.development.exam_online.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("exam_cheat_log")
@Data
public class ExamCheatLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("exam_id")
    private Long examId;

    @TableField("user_id")
    private Long userId;

    @TableField("cheat_type")
    private String cheatType;

    @TableField("count")
    private Integer count;

    @TableField("last_time")
    private LocalDateTime lastTime;

    @TableField("created_by")
    private Long createdBy;

    @TableField("deleted")
    private Integer deleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}


