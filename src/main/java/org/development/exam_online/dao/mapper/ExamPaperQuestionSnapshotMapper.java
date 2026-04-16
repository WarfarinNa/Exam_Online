package org.development.exam_online.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.development.exam_online.dao.entity.ExamPaperQuestionSnapshot;

@Mapper
public interface ExamPaperQuestionSnapshotMapper extends BaseMapper<ExamPaperQuestionSnapshot> {


    @Insert("INSERT INTO exam_paper_question_snapshot (exam_id, question_id, question_score, question_order) " +
            "SELECT #{examId}, epq.question_id, epq.question_score, epq.question_order " +
            "FROM exam_paper_question epq WHERE epq.paper_id = #{paperId}")
    int createSnapshotFromPaper(@Param("examId") Long examId, @Param("paperId") Long paperId);
}
