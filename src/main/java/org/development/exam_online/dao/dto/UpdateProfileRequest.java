package org.development.exam_online.dao.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String realName;
    private String email;
    private String phone;
}


