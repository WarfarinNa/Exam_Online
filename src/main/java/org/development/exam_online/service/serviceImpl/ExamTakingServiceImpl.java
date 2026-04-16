package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.constants.ExamRecordStatus;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.dao.entity.*;
import org.development.exam_online.dao.mapper.*;
import org.development.exam_online.service.ExamTakingService;
import org.development.exam_online.service.grading.GradingHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamTakingServiceImpl implements ExamTakingService {

    private final ExamMapper examMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ExamPaperQuestionMapper examPaperQuestionMapper;
    private final ExamPaperQuestionSnapshotMapper snapshotMapper;
    private final QuestionMapper questionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final ExamCheatLogMapper examCheatLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startExam(Long examId, Long userId) {
        Exam exam = requireActiveExam(examId);
        ExamPaper paper = requireActivePaper(exam.getPaperId());
        validateExamPermissionAndTime(exam, userId);

        // 查询是否已有记录
        LambdaQueryWrapper<ExamRecord> q = new LambdaQueryWrapper<>();
        q.eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getDeleted, 0)
                .last("LIMIT 1");
        List<ExamRecord> existingList = examRecordMapper.selectList(q);
        ExamRecord existing = existingList.isEmpty() ? null : existingList.get(0);
        if (existing != null) {
            if (ExamRecordStatus.isSubmitted(existing.getStatus())) {
                throw new BusinessException(ErrorCode.EXAM_ALREADY_SUBMITTED);
            }
            throw new BusinessException(ErrorCode.EXAM_ALREADY_STARTED);
        }

        LocalDateTime now = LocalDateTime.now();
        ExamRecord record = new ExamRecord();
        record.setExamId(examId);
        record.setUserId(userId);
        record.setStartTime(now);
        record.setObjectiveScore(BigDecimal.ZERO);
        record.setSubjectiveScore(BigDecimal.ZERO);
        record.setTotalScore(BigDecimal.ZERO);
        record.setStatus(ExamRecordStatus.IN_PROGRESS); // 进行中
        record.setDeleted(0);
        int inserted = examRecordMapper.insert(record);
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "创建考试记录失败");
        }

        long questionCount = getSnapshotQuestionCount(examId);
        int duration = resolveDuration(exam, paper);

        // 从快照计算试卷总分
        List<ExamPaperQuestionSnapshot> snapshots = getSnapshotQuestions(examId);
        BigDecimal snapshotTotalScore = snapshots.stream()
                .map(s -> s.getQuestionScore() != null ? s.getQuestionScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("examName", exam.getName());
        result.put("paperId", paper.getId());
        result.put("paperName", paper.getName());
        result.put("recordId", record.getId());
        result.put("startTime", record.getStartTime());
        result.put("questionCount", questionCount);
        result.put("totalScore", snapshotTotalScore);
        result.put("duration", duration);
        result.put("endTime", exam.getEndTime());
        return result;
    }

    @Override
    public Map<String, Object> getExamStatus(Long examId, Long userId) {
        Exam exam = requireActiveExam(examId);
        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("examName", exam.getName());

        LambdaQueryWrapper<ExamRecord> q = new LambdaQueryWrapper<>();
        q.eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getDeleted, 0)
                .last("LIMIT 1");
        List<ExamRecord> records = examRecordMapper.selectList(q);
        ExamRecord record = records.isEmpty() ? null : records.get(0);
        if (record == null) {
            result.put("status", "NOT_STARTED");
            result.put("started", false);
            return result;
        }

        result.put("recordId", record.getId());
        result.put("startTime", record.getStartTime());
        result.put("submitTime", record.getSubmitTime());
        result.put("status", record.getStatus());
        result.put("started", true);

        if (record.getStatus() != null && record.getStatus() == ExamRecordStatus.IN_PROGRESS) {
            long remainingSeconds = computeRemainingSeconds(exam, record);
            result.put("remainingTime", remainingSeconds);
        } else {
            result.put("remainingTime", 0L);
        }
        return result;
    }

    @Override
    public Map<String, Object> getExamQuestions(Long examId, Long userId) {
        Exam exam = requireActiveExam(examId);
        ExamPaper paper = requireActivePaper(exam.getPaperId());

        ExamRecord record = requireExamRecord(examId, userId);
        if (record.getStatus() != null && record.getStatus() >= ExamRecordStatus.SUBMITTED_UNGRADED) {
            throw new BusinessException(ErrorCode.EXAM_ALREADY_SUBMITTED);
        }

        List<ExamPaperQuestionSnapshot> snapshots = getSnapshotQuestions(examId);
        Map<Long, ExamAnswer> answerMap = getAnswerMap(record.getId());
        Map<String, Object> result = buildQuestionViewFromSnapshot(exam, paper, record, snapshots, answerMap, false);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnswer(Long examId, Long userId, Long questionId, String answer) {
        Exam exam = requireActiveExam(examId);
        ExamRecord record = requireExamRecord(examId, userId);
        validateExamInProgress(exam, record);
        upsertAnswer(record.getId(), questionId, answer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnswers(Long examId, Long userId, Map<Long, String> answers) {
        Exam exam = requireActiveExam(examId);
        ExamRecord record = requireExamRecord(examId, userId);
        validateExamInProgress(exam, record);
        if (answers == null || answers.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, String> e : answers.entrySet()) {
            upsertAnswer(record.getId(), e.getKey(), e.getValue());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logCheat(Long examId, Long userId, String cheatType) {
        requireExamRecord(examId, userId);

        LambdaQueryWrapper<ExamCheatLog> q = new LambdaQueryWrapper<>();
        q.eq(ExamCheatLog::getExamId, examId)
                .eq(ExamCheatLog::getUserId, userId)
                .eq(ExamCheatLog::getCheatType, cheatType)
                .eq(ExamCheatLog::getDeleted, 0)
                .last("LIMIT 1");  
        
        List<ExamCheatLog> logs = examCheatLogMapper.selectList(q);
        ExamCheatLog log = logs.isEmpty() ? null : logs.get(0);
        
        LocalDateTime now = LocalDateTime.now();
        if (log == null) {
            log = new ExamCheatLog();
            log.setExamId(examId);
            log.setUserId(userId);
            log.setCheatType(cheatType);
            log.setCount(1);
            log.setLastTime(now);
            log.setDeleted(0);
            examCheatLogMapper.insert(log);
        } else {
            log.setCount(log.getCount() + 1);
            log.setLastTime(now);
            examCheatLogMapper.updateById(log);
        }
    }

    @Override
    public Long getRemainingTime(Long examId, Long userId) {
        Exam exam = requireActiveExam(examId);
        LambdaQueryWrapper<ExamRecord> q = new LambdaQueryWrapper<>();
        q.eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getDeleted, 0)
                .last("LIMIT 1");
        List<ExamRecord> records = examRecordMapper.selectList(q);
        ExamRecord record = records.isEmpty() ? null : records.get(0);
        if (record == null || record.getStatus() == null || record.getStatus() != ExamRecordStatus.IN_PROGRESS) {
            return 0L;
        }
        return computeRemainingSeconds(exam, record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitExam(Long examId, Long userId) {
        Exam exam = requireActiveExam(examId);
        ExamPaper paper = requireActivePaper(exam.getPaperId());
        ExamRecord record = requireExamRecord(examId, userId);
        validateExamInProgress(exam, record);

        // 客观题自动判分（使用快照中的试卷分值）
        List<ExamPaperQuestionSnapshot> snapshots = getSnapshotQuestions(examId);
        Map<Long, ExamAnswer> answers = getAnswerMap(record.getId());
        List<Long> qIds = snapshots.stream().map(ExamPaperQuestionSnapshot::getQuestionId).toList();
        List<Question> questions = questionMapper.selectBatchIdsIgnoreDeleted(qIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        BigDecimal objectiveScore = BigDecimal.ZERO;
        for (ExamPaperQuestionSnapshot snap : snapshots) {
            Question q = questionMap.get(snap.getQuestionId());
            if (q == null) continue;
            
            // 只对客观题进行自动判分（包括填空题）
            if (!GradingHelper.isObjectiveQuestion(q.getType())) {
                continue;
            }
            
            ExamAnswer ans = answers.get(q.getId());
            if (ans == null) {
                // 创建0分记录
                ans = new ExamAnswer();
                ans.setRecordId(record.getId());
                ans.setQuestionId(q.getId());
                ans.setScore(BigDecimal.ZERO);
                ans.setDeleted(0);
                ans.setIsManualGraded(0); // 标记为自动判分
                examAnswerMapper.insert(ans);
                continue;
            }

            // 使用快照中的分值（优先）或题目默认分值
            BigDecimal fullScore = snap.getQuestionScore() != null ? snap.getQuestionScore() : q.getScore();
            if (fullScore == null) fullScore = BigDecimal.ZERO;
            
            // 使用GradingHelper计算得分
            BigDecimal score = GradingHelper.calculateObjectiveScore(
                q.getAnswerJson(), 
                ans.getUserAnswer(), 
                fullScore
            );
            
            ans.setScore(score);
            ans.setIsManualGraded(0); // 标记为自动判分
            objectiveScore = objectiveScore.add(score);
            examAnswerMapper.updateById(ans);
        }

        record.setObjectiveScore(objectiveScore);
        if (record.getSubjectiveScore() == null) {
            record.setSubjectiveScore(BigDecimal.ZERO);
        }
        record.setTotalScore(objectiveScore.add(record.getSubjectiveScore()));
        
        // 判断是否有主观题，决定进入状态3（评分中）还是状态4（已评分）
        boolean hasSubjectiveQuestion = questionMap.values().stream()
                .anyMatch(q -> !GradingHelper.isObjectiveQuestion(q.getType()));
        if (hasSubjectiveQuestion) {
            record.setStatus(ExamRecordStatus.SUBMITTED_GRADING); // 客观题已判分，主观题待教师评分
        } else {
            record.setStatus(ExamRecordStatus.SUBMITTED_GRADED); // 全部客观题，评分完成
        }
        record.setSubmitTime(LocalDateTime.now());
        examRecordMapper.updateById(record);

        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("recordId", record.getId());
        result.put("objectiveScore", record.getObjectiveScore());
        result.put("subjectiveScore", record.getSubjectiveScore());
        result.put("totalScore", record.getTotalScore());
        return result;
    }

    @Override
    public Map<String, Object> continueExam(Long examId, Long userId) {
        Exam exam = requireActiveExam(examId);
        ExamPaper paper = requireActivePaper(exam.getPaperId());
        ExamRecord record = requireExamRecord(examId, userId);

        List<ExamPaperQuestionSnapshot> snapshots = getSnapshotQuestions(examId);
        Map<Long, ExamAnswer> answerMap = getAnswerMap(record.getId());
        Map<String, Object> view = buildQuestionViewFromSnapshot(exam, paper, record, snapshots, answerMap, true);
        long remainingSeconds = computeRemainingSeconds(exam, record);
        view.put("remainingTime", remainingSeconds);
        return view;
    }

    private Exam requireActiveExam(Long examId) {
        if (examId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "考试ID不能为空");
        }
        Exam exam = examMapper.selectById(examId);
        if (exam == null || Objects.equals(exam.getDeleted(), 1)) {
            throw new BusinessException(ErrorCode.EXAM_NOT_FOUND);
        }
        return exam;
    }

    private ExamPaper requireActivePaper(Long paperId) {
        if (paperId == null) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_NOT_FOUND);
        }
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null || Objects.equals(paper.getDeleted(), 1)) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_NOT_FOUND);
        }
        return paper;
    }

    private ExamRecord requireExamRecord(Long examId, Long userId) {
        LambdaQueryWrapper<ExamRecord> q = new LambdaQueryWrapper<>();
        q.eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getDeleted, 0)
                .last("LIMIT 1");  // 防止多条记录导致异常
        
        List<ExamRecord> records = examRecordMapper.selectList(q);
        if (records.isEmpty()) {
            throw new BusinessException(ErrorCode.EXAM_NOT_STARTED_BY_USER);
        }
        return records.get(0);
    }

    private long getSnapshotQuestionCount(Long examId) {
        LambdaQueryWrapper<ExamPaperQuestionSnapshot> q = new LambdaQueryWrapper<>();
        q.eq(ExamPaperQuestionSnapshot::getExamId, examId);
        return snapshotMapper.selectCount(q);
    }

    private int resolveDuration(Exam exam, ExamPaper paper) {
        if (paper.getDuration() != null && paper.getDuration() > 0) {
            return paper.getDuration();
        }
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            long minutes = Duration.between(exam.getStartTime(), exam.getEndTime()).toMinutes();
            return (int) Math.max(minutes, 0);
        }
        return 60;
    }

    private List<ExamPaperQuestion> getPaperQuestions(Long paperId) {
        LambdaQueryWrapper<ExamPaperQuestion> q = new LambdaQueryWrapper<>();
        q.eq(ExamPaperQuestion::getPaperId, paperId)
                .orderByAsc(ExamPaperQuestion::getQuestionOrder);
        return examPaperQuestionMapper.selectList(q);
    }

    /**
     * 从快照表获取考试的题目列表（考试发布时冻结的版本）
     */
    private List<ExamPaperQuestionSnapshot> getSnapshotQuestions(Long examId) {
        LambdaQueryWrapper<ExamPaperQuestionSnapshot> q = new LambdaQueryWrapper<>();
        q.eq(ExamPaperQuestionSnapshot::getExamId, examId)
                .orderByAsc(ExamPaperQuestionSnapshot::getQuestionOrder);
        return snapshotMapper.selectList(q);
    }

    private Map<Long, ExamAnswer> getAnswerMap(Long recordId) {
        LambdaQueryWrapper<ExamAnswer> q = new LambdaQueryWrapper<>();
        q.eq(ExamAnswer::getRecordId, recordId)
                .eq(ExamAnswer::getDeleted, 0);
        List<ExamAnswer> answers = examAnswerMapper.selectList(q);
        if (answers == null || answers.isEmpty()) {
            return Collections.emptyMap();
        }
        return answers.stream().collect(Collectors.toMap(ExamAnswer::getQuestionId, a -> a));
    }

    private void upsertAnswer(Long recordId, Long questionId, String answerJson) {
        if (recordId == null || questionId == null) return;
        // 强制要求传入的是合法 JSON 文本
        if (answerJson == null || answerJson.isBlank()) {
            answerJson = "null";
        }
        try {
            objectMapper.readTree(answerJson);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "答案必须是合法的JSON格式");
        }

        // 使用自定义方法查询，忽略逻辑删除状态
        ExamAnswer existing = examAnswerMapper.selectByRecordAndQuestionIgnoreDeleted(recordId, questionId);
        
        if (existing == null) {
            // 不存在，创建新记录
            try {
                existing = new ExamAnswer();
                existing.setRecordId(recordId);
                existing.setQuestionId(questionId);
                existing.setUserAnswer(answerJson);
                existing.setDeleted(0);
                existing.setIsManualGraded(0);
                examAnswerMapper.insert(existing);
            } catch (Exception e) {
                // 如果插入失败（并发导致的唯一索引冲突），重新查询并更新
                if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                    existing = examAnswerMapper.selectByRecordAndQuestionIgnoreDeleted(recordId, questionId);
                    if (existing == null) {
                        throw e;
                    }
                    // 继续执行下面的更新逻辑
                } else {
                    throw e;
                }
            }
        }
        
        // 如果记录存在，更新它
        if (existing != null && existing.getId() != null) {
            if (existing.getDeleted() != null && existing.getDeleted() == 1) {
                // 恢复已逻辑删除的记录
                com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ExamAnswer> uw =
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
                uw.eq(ExamAnswer::getId, existing.getId())
                  .set(ExamAnswer::getDeleted, 0)
                  .set(ExamAnswer::getUserAnswer, answerJson)
                  .set(ExamAnswer::getIsManualGraded, 0)
                  .set(ExamAnswer::getScore, null);
                examAnswerMapper.update(null, uw);
            } else {
                existing.setUserAnswer(answerJson);
                // 学生修改答案时，如果之前是人工评分，重置为自动判分（因为答案变了）
                if (existing.getIsManualGraded() != null && existing.getIsManualGraded() == 1) {
                    existing.setIsManualGraded(0);
                    existing.setScore(null);
                }
                examAnswerMapper.updateById(existing);
            }
        }
    }

    private void validateExamPermissionAndTime(Exam exam, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BusinessException(ErrorCode.EXAM_NOT_STARTED);
        }
        if (exam.getEndTime() != null && (now.isAfter(exam.getEndTime()) || now.isEqual(exam.getEndTime()))) {
            throw new BusinessException(ErrorCode.EXAM_ENDED);
        }
        if (exam.getStatus() == null || exam.getStatus() != 1) {
            throw new BusinessException(ErrorCode.EXAM_PERMISSION_DENIED, "考试未发布");
        }
        // allowRoles JSON 校验可后续补充；此处默认所有学生可参加
    }

    private void validateExamInProgress(Exam exam, ExamRecord record) {
        LocalDateTime now = LocalDateTime.now();
        
        // 检查考试硬性结束时间
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BusinessException(ErrorCode.EXAM_TIME_EXPIRED);
        }
        
        // 检查基于试卷时长的结束时间（重要：防止倒计时结束后仍可提交）
        if (record.getStartTime() != null) {
            ExamPaper paper = requireActivePaper(exam.getPaperId());
            int duration = resolveDuration(exam, paper);
            LocalDateTime endByDuration = record.getStartTime().plusMinutes(duration);
            
            // 取两个结束时间中较早的一个作为实际结束时间
            LocalDateTime hardEnd = exam.getEndTime() != null ? exam.getEndTime() : endByDuration;
            LocalDateTime effectiveEnd = endByDuration.isBefore(hardEnd) ? endByDuration : hardEnd;
            
            if (now.isAfter(effectiveEnd)) {
                throw new BusinessException(ErrorCode.EXAM_TIME_EXPIRED, "考试时间已到，无法继续作答或提交");
            }
        }
        
        if (record.getStatus() == null || record.getStatus() != ExamRecordStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.EXAM_RECORD_NOT_SUBMITTED, "考试不在进行中，无法保存或提交");
        }
    }

    private long computeRemainingSeconds(Exam exam, ExamRecord record) {
        int duration = resolveDuration(exam, requireActivePaper(exam.getPaperId()));
        LocalDateTime endByDuration = record.getStartTime().plusMinutes(duration);
        LocalDateTime hardEnd = exam.getEndTime() != null ? exam.getEndTime() : endByDuration;
        LocalDateTime effectiveEnd = endByDuration.isBefore(hardEnd) ? endByDuration : hardEnd;
        long seconds = Duration.between(LocalDateTime.now(), effectiveEnd).getSeconds();
        return Math.max(seconds, 0L);
    }

    private Map<String, Object> buildQuestionViewFromSnapshot(Exam exam,
                                                  ExamPaper paper,
                                                  ExamRecord record,
                                                  List<ExamPaperQuestionSnapshot> snapshots,
                                                  Map<Long, ExamAnswer> answers,
                                                  boolean includeAnswers) {
        List<Long> qIds = snapshots.stream().map(ExamPaperQuestionSnapshot::getQuestionId).toList();
        List<Question> questions = questionMapper.selectBatchIdsIgnoreDeleted(qIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<Map<String, Object>> questionViews = new ArrayList<>();
        for (ExamPaperQuestionSnapshot snap : snapshots) {
            Question q = questionMap.get(snap.getQuestionId());
            if (q == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", q.getId());
            m.put("type", q.getType());
            m.put("stem", q.getStem());
            m.put("optionsJson", q.getOptionsJson());
            if (includeAnswers) {
                ExamAnswer ans = answers.get(q.getId());
                if (ans != null && ans.getUserAnswer() != null) {
                    try {
                        m.put("userAnswer", objectMapper.readTree(ans.getUserAnswer()));
                    } catch (Exception e) {
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "存储的答案JSON无效");
                    }
                } else {
                    m.put("userAnswer", null);
                }
            }
            m.put("score", snap.getQuestionScore());
            m.put("difficulty", q.getDifficulty());
            m.put("order", snap.getQuestionOrder());
            questionViews.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("examId", exam.getId());
        result.put("examName", exam.getName());
        result.put("paperId", paper.getId());
        result.put("paperName", paper.getName());
        result.put("recordId", record.getId());
        result.put("questions", questionViews);
        return result;
    }
}

