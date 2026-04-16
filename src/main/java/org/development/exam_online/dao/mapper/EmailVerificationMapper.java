package org.development.exam_online.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.development.exam_online.dao.entity.EmailVerification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮箱验证码Mapper接口
 */
@Mapper
public interface EmailVerificationMapper extends BaseMapper<EmailVerification> {
}