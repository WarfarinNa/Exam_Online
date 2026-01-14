package org.development.exam_online.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.development.exam_online.dao.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}


