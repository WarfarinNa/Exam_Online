package org.development.exam_online.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.development.exam_online.dao.entity.ExamAnswer;

@Mapper
public interface ExamAnswerMapper extends BaseMapper<ExamAnswer> {
    @Select("SELECT * FROM exam_answer WHERE record_id = #{recordId} AND question_id = #{questionId} LIMIT 1")
    ExamAnswer selectByRecordAndQuestionIgnoreDeleted(Long recordId, Long questionId);
}



