package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.dao.entity.*;
import org.development.exam_online.dao.mapper.*;
import org.development.exam_online.service.GradingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradingServiceImpl implements GradingService {

    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ExamPaperQuestionMapper examPaperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final ExamAnswerMapper examAnswerMapper;

    @Override
    public PageResult<Map<String, Object>> getPendingGradingRecords(Long examId, Long page, Long size) {
        long p = (page == null || page < 1) ? 1 : page;
        long s = (size == null || size < 1) ? 10 : size;

        LambdaQueryWrapper<ExamRecord> q = new LambdaQueryWrapper<>();
        q.eq(ExamRecord::getDeleted, 0)
                .eq(ExamRecord::getStatus, 2); // 已提交，待评分
        if (examId != null) {
            q.eq(ExamRecord::getExamId, examId);
        }
        q.orderByDesc(ExamRecord::getSubmitTime);

        Page<ExamRecord> pageObj = new Page<>(p, s);
        Page<ExamRecord> recordsPage = examRecordMapper.selectPage(pageObj, q);
        if (recordsPage.getRecords().isEmpty()) {
            return PageResult.of(0, p, s, Collections.emptyList());
        }

        // 预取考试信息
        List<Long> examIds = recordsPage.getRecords().stream()
                .map(ExamRecord::getExamId)
                .distinct()
                .toList();
        Map<Long, Exam> examMap = examMapper.selectBatchIds(examIds).stream()
                .collect(Collectors.toMap(Exam::getId, e -> e));

        List<Map<String, Object>> views = new ArrayList<>();
        for (ExamRecord r : recordsPage.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            Exam exam = examMap.get(r.getExamId());
            m.put("recordId", r.getId());
            m.put("examId", r.getExamId());
            m.put("examName", exam != null ? exam.getName() : null);
            m.put("userId", r.getUserId());
            m.put("startTime", r.getStartTime());
            m.put("submitTime", r.getSubmitTime());
            m.put("status", r.getStatus());
            m.put("objectiveScore", r.getObjectiveScore());
            m.put("subjectiveScore", r.getSubjectiveScore());
            m.put("totalScore", r.getTotalScore());
            views.add(m);
        }
        return PageResult.of(recordsPage.getTotal(), p, s, views);
    }

    @Override
    public Map<String, Object> getExamRecordDetail(Long recordId) {
        ExamRecord record = requireRecord(recordId);
        Exam exam = requireExam(record.getExamId());
        ExamPaper paper = requirePaper(exam.getPaperId());

        List<ExamPaperQuestion> epqs = getPaperQuestions(paper.getId());
        Map<Long, ExamAnswer> answerMap = getAnswerMap(recordId);

        List<Long> qIds = epqs.stream().map(ExamPaperQuestion::getQuestionId).toList();
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<Map<String, Object>> questionViews = new ArrayList<>();
        for (ExamPaperQuestion epq : epqs) {
            Question q = questionMap.get(epq.getQuestionId());
            if (q == null) continue;
            ExamAnswer ans = answerMap.get(q.getId());

            String type = q.getType();
            boolean isObjective = QuestionType.SINGLE.getCode().equals(type)
                    || QuestionType.MULTIPLE.getCode().equals(type)
                    || QuestionType.JUDGE.getCode().equals(type)
                    || QuestionType.BLANK.getCode().equals(type);

            BigDecimal fullScore = epq.getQuestionScore() != null ? epq.getQuestionScore() : q.getScore();
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", q.getId());
            m.put("type", type);
            m.put("stem", q.getStem());
            m.put("optionsJson", q.getOptionsJson());
            m.put("answerJson", q.getAnswerJson());
            m.put("userAnswer", ans != null ? ans.getUserAnswer() : null);
            m.put("score", ans != null ? ans.getScore() : null);
            m.put("fullScore", fullScore);
            m.put("difficulty", q.getDifficulty());
            m.put("order", epq.getQuestionOrder());
            m.put("objective", isObjective);
            questionViews.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("exam", exam);
        result.put("record", record);
        result.put("paper", paper);
        result.put("questions", questionViews);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gradeQuestion(Long recordId, Long questionId, BigDecimal score) {
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.SUBJECTIVE_QUESTION_SCORE_INVALID);
        }
        ExamRecord record = requireRecord(recordId);
        Exam exam = requireExam(record.getExamId());
        ExamPaper paper = requirePaper(exam.getPaperId());

        // 获取试卷问题和题目
        List<ExamPaperQuestion> epqs = getPaperQuestions(paper.getId());
        Map<Long, ExamPaperQuestion> epqMap = epqs.stream()
                .collect(Collectors.toMap(ExamPaperQuestion::getQuestionId, e -> e));
        ExamPaperQuestion epq = epqMap.get(questionId);
        if (epq == null) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_QUESTION_NOT_FOUND);
        }

        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }
        String type = question.getType();
        boolean isObjective = QuestionType.SINGLE.getCode().equals(type)
                || QuestionType.MULTIPLE.getCode().equals(type)
                || QuestionType.JUDGE.getCode().equals(type)
                || QuestionType.BLANK.getCode().equals(type);
        if (isObjective) {
            throw new BusinessException(ErrorCode.SUBJECTIVE_QUESTION_SCORE_INVALID, "客观题无需人工评分");
        }

        BigDecimal fullScore = epq.getQuestionScore() != null ? epq.getQuestionScore() : question.getScore();
        if (fullScore == null) fullScore = BigDecimal.ZERO;
        if (score.compareTo(fullScore) > 0) {
            throw new BusinessException(ErrorCode.SUBJECTIVE_QUESTION_SCORE_EXCEED);
        }

        // 更新答案得分
        ExamAnswer answer = getOrCreateAnswer(recordId, questionId);
        answer.setScore(score);
        examAnswerMapper.updateById(answer);

        // 重新汇总主观题得分与总分
        recalcRecordScores(recordId, exam, paper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gradeQuestions(Long recordId, Map<Long, BigDecimal> scores) {
        if (scores == null || scores.isEmpty()) return;
        for (Map.Entry<Long, BigDecimal> e : scores.entrySet()) {
            gradeQuestion(recordId, e.getKey(), e.getValue());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoGradeRecord(Long recordId) {
        ExamRecord record = requireRecord(recordId);
        Exam exam = requireExam(record.getExamId());
        ExamPaper paper = requirePaper(exam.getPaperId());
        List<ExamPaperQuestion> epqs = getPaperQuestions(paper.getId());
        Map<Long, ExamAnswer> answerMap = getAnswerMap(recordId);

        List<Long> qIds = epqs.stream().map(ExamPaperQuestion::getQuestionId).toList();
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        BigDecimal objectiveScore = BigDecimal.ZERO;
        for (ExamPaperQuestion epq : epqs) {
            Question q = questionMap.get(epq.getQuestionId());
            if (q == null) continue;
            String type = q.getType();
            boolean isObjective = QuestionType.SINGLE.getCode().equals(type)
                    || QuestionType.MULTIPLE.getCode().equals(type)
                    || QuestionType.JUDGE.getCode().equals(type)
                    || QuestionType.BLANK.getCode().equals(type);
            if (!isObjective) continue;

            ExamAnswer ans = answerMap.get(q.getId());
            if (ans == null) continue;

            BigDecimal fullScore = epq.getQuestionScore() != null ? epq.getQuestionScore() : q.getScore();
            if (fullScore == null) fullScore = BigDecimal.ZERO;
            if (Objects.equals(q.getAnswerJson(), ans.getUserAnswer())) {
                ans.setScore(fullScore);
                objectiveScore = objectiveScore.add(fullScore);
            } else {
                ans.setScore(BigDecimal.ZERO);
            }
            examAnswerMapper.updateById(ans);
        }

        record.setObjectiveScore(objectiveScore);
        if (record.getSubjectiveScore() == null) {
            record.setSubjectiveScore(BigDecimal.ZERO);
        }
        record.setTotalScore(objectiveScore.add(record.getSubjectiveScore()));
        examRecordMapper.updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoGradeExam(Long examId) {
        requireExam(examId);
        LambdaQueryWrapper<ExamRecord> q = new LambdaQueryWrapper<>();
        q.eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getDeleted, 0);
        List<ExamRecord> records = examRecordMapper.selectList(q);
        int total = records.size();
        int graded = 0;
        for (ExamRecord r : records) {
            autoGradeRecord(r.getId());
            graded++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("totalRecords", total);
        result.put("gradedRecords", graded);
        return result;
    }

    @Override
    public Map<String, Object> getExamStatistics(Long examId) {
        Exam exam = requireExam(examId);
        LambdaQueryWrapper<ExamRecord> q = new LambdaQueryWrapper<>();
        q.eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getDeleted, 0);
        List<ExamRecord> records = examRecordMapper.selectList(q);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("examId", examId);
        statistics.put("examName", exam.getName());
        statistics.put("totalParticipants", records.size());

        if (records.isEmpty()) {
            statistics.put("completedCount", 0L);
            statistics.put("inProgressCount", 0L);
            statistics.put("notStartedCount", 0L);
            statistics.put("averageScore", 0);
            statistics.put("highestScore", 0);
            statistics.put("lowestScore", 0);
            statistics.put("passRate", 0);
            return statistics;
        }

        long completedCount = records.stream()
                .filter(r -> r.getTotalScore() != null)
                .count();
        long inProgressCount = records.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                .count();
        long notStartedCount = records.stream()
                .filter(r -> r.getStartTime() == null)
                .count();

        statistics.put("completedCount", completedCount);
        statistics.put("inProgressCount", inProgressCount);
        statistics.put("notStartedCount", notStartedCount);

        List<ExamRecord> submitted = records.stream()
                .filter(r -> r.getTotalScore() != null)
                .toList();
        if (submitted.isEmpty()) {
            statistics.put("averageScore", 0);
            statistics.put("highestScore", 0);
            statistics.put("lowestScore", 0);
            statistics.put("passRate", 0);
        } else {
            BigDecimal totalScore = submitted.stream()
                    .map(ExamRecord::getTotalScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = totalScore
                    .divide(BigDecimal.valueOf(submitted.size()), 2, RoundingMode.HALF_UP);
            BigDecimal highest = submitted.stream()
                    .map(ExamRecord::getTotalScore)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            BigDecimal lowest = submitted.stream()
                    .map(ExamRecord::getTotalScore)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            long passCount = submitted.stream()
                    .filter(r -> r.getTotalScore().compareTo(BigDecimal.valueOf(60)) >= 0)
                    .count();
            double passRate = submitted.isEmpty() ? 0.0 :
                    BigDecimal.valueOf(passCount * 100.0 / submitted.size())
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue();

            statistics.put("averageScore", average.doubleValue());
            statistics.put("highestScore", highest.doubleValue());
            statistics.put("lowestScore", lowest.doubleValue());
            statistics.put("passRate", passRate);
        }

        return statistics;
    }

    @Override
    public List<Map<String, Object>> getWrongQuestionAnalysis(Long examId) {
        Exam exam = requireExam(examId);
        ExamPaper paper = requirePaper(exam.getPaperId());
        List<ExamPaperQuestion> epqs = getPaperQuestions(paper.getId());

        if (epqs.isEmpty()) return Collections.emptyList();

        List<Long> qIds = epqs.stream().map(ExamPaperQuestion::getQuestionId).toList();
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 仅统计客观题
        List<Long> objectiveIds = questionMap.values().stream()
                .filter(q -> {
                    String type = q.getType();
                    return QuestionType.SINGLE.getCode().equals(type)
                            || QuestionType.MULTIPLE.getCode().equals(type)
                            || QuestionType.JUDGE.getCode().equals(type)
                            || QuestionType.BLANK.getCode().equals(type);
                })
                .map(Question::getId)
                .toList();
        if (objectiveIds.isEmpty()) return Collections.emptyList();

        LambdaQueryWrapper<ExamRecord> rcQ = new LambdaQueryWrapper<>();
        rcQ.eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getDeleted, 0);
        List<ExamRecord> records = examRecordMapper.selectList(rcQ);
        if (records.isEmpty()) return Collections.emptyList();
        List<Long> recordIds = records.stream().map(ExamRecord::getId).toList();

        LambdaQueryWrapper<ExamAnswer> ansQ = new LambdaQueryWrapper<>();
        ansQ.in(ExamAnswer::getRecordId, recordIds)
                .in(ExamAnswer::getQuestionId, objectiveIds)
                .eq(ExamAnswer::getDeleted, 0);
        List<ExamAnswer> answers = examAnswerMapper.selectList(ansQ);
        if (answers.isEmpty()) return Collections.emptyList();

        Map<Long, List<ExamAnswer>> byQuestion = answers.stream()
                .collect(Collectors.groupingBy(ExamAnswer::getQuestionId));

        List<Map<String, Object>> analysis = new ArrayList<>();
        for (Long qId : objectiveIds) {
            Question q = questionMap.get(qId);
            if (q == null) continue;
            List<ExamAnswer> qAnswers = byQuestion.getOrDefault(qId, Collections.emptyList());
            int attempts = qAnswers.size();
            if (attempts == 0) continue;

            String correct = q.getAnswerJson();
            int wrong = 0;
            for (ExamAnswer a : qAnswers) {
                if (!Objects.equals(correct, a.getUserAnswer())) {
                    wrong++;
                }
            }
            double wrongRate = attempts == 0 ? 0.0 :
                    BigDecimal.valueOf(wrong * 1.0 / attempts)
                            .setScale(3, RoundingMode.HALF_UP)
                            .doubleValue();

            Map<String, Object> m = new HashMap<>();
            m.put("questionId", qId);
            m.put("stem", q.getStem());
            m.put("type", q.getType());
            m.put("attempts", attempts);
            m.put("wrongCount", wrong);
            m.put("wrongRate", wrongRate);
            analysis.add(m);
        }

        analysis.sort((a, b) ->
                Double.compare((double) b.get("wrongRate"), (double) a.get("wrongRate")));
        return analysis;
    }

    @Override
    public PageResult<Map<String, Object>> getStudentRecords(Long userId, Long page, Long size) {
        long p = (page == null || page < 1) ? 1 : page;
        long s = (size == null || size < 1) ? 10 : size;

        LambdaQueryWrapper<ExamRecord> q = new LambdaQueryWrapper<>();
        q.eq(ExamRecord::getDeleted, 0)
                .eq(ExamRecord::getUserId, userId)
                .orderByDesc(ExamRecord::getStartTime);

        Page<ExamRecord> pageObj = new Page<>(p, s);
        Page<ExamRecord> recordsPage = examRecordMapper.selectPage(pageObj, q);
        if (recordsPage.getRecords().isEmpty()) {
            return PageResult.of(0, p, s, Collections.emptyList());
        }

        List<Long> examIds = recordsPage.getRecords().stream()
                .map(ExamRecord::getExamId)
                .distinct()
                .toList();
        Map<Long, Exam> examMap = examMapper.selectBatchIds(examIds).stream()
                .collect(Collectors.toMap(Exam::getId, e -> e));

        List<Map<String, Object>> views = new ArrayList<>();
        for (ExamRecord r : recordsPage.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            Exam exam = examMap.get(r.getExamId());
            m.put("recordId", r.getId());
            m.put("examId", r.getExamId());
            m.put("examName", exam != null ? exam.getName() : null);
            m.put("startTime", r.getStartTime());
            m.put("submitTime", r.getSubmitTime());
            m.put("status", r.getStatus());
            m.put("objectiveScore", r.getObjectiveScore());
            m.put("subjectiveScore", r.getSubjectiveScore());
            m.put("totalScore", r.getTotalScore());
            views.add(m);
        }
        return PageResult.of(recordsPage.getTotal(), p, s, views);
    }

    @Override
    public Map<String, Object> getStudentExamDetail(Long recordId, Long userId) {
        ExamRecord record = requireRecord(recordId);
        if (!Objects.equals(record.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Exam exam = requireExam(record.getExamId());
        ExamPaper paper = requirePaper(exam.getPaperId());

        List<ExamPaperQuestion> epqs = getPaperQuestions(paper.getId());
        Map<Long, ExamAnswer> answerMap = getAnswerMap(recordId);

        List<Long> qIds = epqs.stream().map(ExamPaperQuestion::getQuestionId).toList();
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<Map<String, Object>> questionViews = new ArrayList<>();
        for (ExamPaperQuestion epq : epqs) {
            Question q = questionMap.get(epq.getQuestionId());
            if (q == null) continue;
            ExamAnswer ans = answerMap.get(q.getId());

            BigDecimal fullScore = epq.getQuestionScore() != null ? epq.getQuestionScore() : q.getScore();
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", q.getId());
            m.put("type", q.getType());
            m.put("stem", q.getStem());
            m.put("optionsJson", q.getOptionsJson());
            m.put("answerJson", q.getAnswerJson());
            m.put("userAnswer", ans != null ? ans.getUserAnswer() : null);
            m.put("score", ans != null ? ans.getScore() : null);
            m.put("fullScore", fullScore);
            m.put("difficulty", q.getDifficulty());
            m.put("order", epq.getQuestionOrder());
            questionViews.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("exam", exam);
        result.put("record", record);
        result.put("paper", paper);
        result.put("questions", questionViews);
        return result;
    }

    private Exam requireExam(Long examId) {
        if (examId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "考试ID不能为空");
        }
        Exam exam = examMapper.selectById(examId);
        if (exam == null || Objects.equals(exam.getDeleted(), 1)) {
            throw new BusinessException(ErrorCode.EXAM_NOT_FOUND);
        }
        return exam;
    }

    private ExamPaper requirePaper(Long paperId) {
        if (paperId == null) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_NOT_FOUND);
        }
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null || Objects.equals(paper.getDeleted(), 1)) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_NOT_FOUND);
        }
        return paper;
    }

    private ExamRecord requireRecord(Long recordId) {
        if (recordId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "考试记录ID不能为空");
        }
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || Objects.equals(record.getDeleted(), 1)) {
            throw new BusinessException(ErrorCode.EXAM_RECORD_NOT_FOUND);
        }
        return record;
    }

    private List<ExamPaperQuestion> getPaperQuestions(Long paperId) {
        LambdaQueryWrapper<ExamPaperQuestion> q = new LambdaQueryWrapper<>();
        q.eq(ExamPaperQuestion::getPaperId, paperId)
                .orderByAsc(ExamPaperQuestion::getQuestionOrder);
        return examPaperQuestionMapper.selectList(q);
    }

    private Map<Long, ExamAnswer> getAnswerMap(Long recordId) {
        LambdaQueryWrapper<ExamAnswer> q = new LambdaQueryWrapper<>();
        q.eq(ExamAnswer::getRecordId, recordId)
                .eq(ExamAnswer::getDeleted, 0);
        List<ExamAnswer> list = examAnswerMapper.selectList(q);
        if (list == null || list.isEmpty()) return Collections.emptyMap();
        return list.stream().collect(Collectors.toMap(ExamAnswer::getQuestionId, a -> a));
    }

    private ExamAnswer getOrCreateAnswer(Long recordId, Long questionId) {
        LambdaQueryWrapper<ExamAnswer> q = new LambdaQueryWrapper<>();
        q.eq(ExamAnswer::getRecordId, recordId)
                .eq(ExamAnswer::getQuestionId, questionId)
                .eq(ExamAnswer::getDeleted, 0);
        ExamAnswer ans = examAnswerMapper.selectOne(q);
        if (ans == null) {
            ans = new ExamAnswer();
            ans.setRecordId(recordId);
            ans.setQuestionId(questionId);
            ans.setDeleted(0);
            examAnswerMapper.insert(ans);
        }
        return ans;
    }

    private void recalcRecordScores(Long recordId, Exam exam, ExamPaper paper) {
        ExamRecord record = requireRecord(recordId);
        List<ExamPaperQuestion> epqs = getPaperQuestions(paper.getId());
        List<Long> qIds = epqs.stream().map(ExamPaperQuestion::getQuestionId).toList();
        Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        Map<Long, ExamAnswer> ansMap = getAnswerMap(recordId);

        BigDecimal subjectiveScore = BigDecimal.ZERO;
        BigDecimal objectiveScore = BigDecimal.ZERO;

        for (ExamPaperQuestion epq : epqs) {
            Question q = qMap.get(epq.getQuestionId());
            if (q == null) continue;
            ExamAnswer ans = ansMap.get(q.getId());
            if (ans == null || ans.getScore() == null) continue;

            String type = q.getType();
            boolean isObjective = QuestionType.SINGLE.getCode().equals(type)
                    || QuestionType.MULTIPLE.getCode().equals(type)
                    || QuestionType.JUDGE.getCode().equals(type)
                    || QuestionType.BLANK.getCode().equals(type);
            if (isObjective) {
                objectiveScore = objectiveScore.add(ans.getScore());
            } else {
                subjectiveScore = subjectiveScore.add(ans.getScore());
            }
        }

        record.setObjectiveScore(objectiveScore);
        record.setSubjectiveScore(subjectiveScore);
        record.setTotalScore(objectiveScore.add(subjectiveScore));
        // 已经完成判卷
        record.setStatus(3);
        examRecordMapper.updateById(record);
    }
}

