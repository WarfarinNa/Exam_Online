package org.development.exam_online.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.development.exam_online.dao.entity.Question;

import java.util.Collection;
import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    @Select("<script>" +
            "SELECT * FROM question WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Question> selectBatchIdsIgnoreDeleted(Collection<Long> ids);
}


