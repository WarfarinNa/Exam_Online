package org.development.exam_online.dao.dto;

import lombok.Builder;
import lombok.Data;
import org.development.exam_online.dao.entity.User;

@Data
@Builder
public class LoginResponse {
    private String token;
    private User user;
}
