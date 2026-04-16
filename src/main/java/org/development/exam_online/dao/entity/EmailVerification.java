package org.development.exam_online.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("email_verification")
@Data
public class EmailVerification {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("email")
    private String email;

    @TableField("code")
    private String code;

    @TableField("type")
    private String type;

    @TableField("used")
    private Integer used;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
