package org.development.exam_online.dao.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;
    private String phone;
}


