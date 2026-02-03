package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.dao.entity.*;
import org.development.exam_online.dao.mapper.*;
import org.development.exam_online.service.ExamTakingService;
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
    private final QuestionMapper questionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final ExamCheatLogMapper examCheatLogMapper;

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
                .eq(ExamRecord::getDeleted, 0);
        ExamRecord existing = examRecordMapper.selectOne(q);
        if (existing != null) {
            if (existing.getStatus() != null && existing.getStatus() >= 2) {
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
        record.setStatus(1); // 进行中
        record.setDeleted(0);
        int inserted = examRecordMapper.insert(record);
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "创建考试记录失败");
        }

        long questionCount = getQuestionCountForPaper(paper.getId());
        int duration = resolveDuration(exam, paper);

        Map<String, Object> result = new HashMap<>();
        result.put("examId", examId);
        result.put("examName", exam.getName());
        result.put("paperId", paper.getId());
        result.put("paperName", paper.getName());
        result.put("recordId", record.getId());
        result.put("startTime", record.getStartTime());
        result.put("questionCount", questionCount);
        result.put("totalScore", paper.getTotalScore());
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
                .eq(ExamRecord::getDeleted, 0);
        ExamRecord record = examRecordMapper.selectOne(q);
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

        if (record.getStatus() != null && record.getStatus() == 1) {
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
        if (record.getStatus() != null && record.getStatus() >= 2) {
            throw new BusinessException(ErrorCode.EXAM_ALREADY_SUBMITTED);
        }

        List<ExamPaperQuestion> epqs = getPaperQuestions(paper.getId());
        Map<Long, ExamAnswer> answerMap = getAnswerMap(record.getId());
        Map<String, Object> result = buildQuestionView(exam, paper, record, epqs, answerMap, false);
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
                .eq(ExamCheatLog::getDeleted, 0);
        ExamCheatLog log = examCheatLogMapper.selectOne(q);
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
                .eq(ExamRecord::getDeleted, 0);
        ExamRecord record = examRecordMapper.selectOne(q);
        if (record == null || record.getStatus() == null || record.getStatus() != 1) {
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

        // 简单客观题自动判分：答案完全相等则得分，否则0（实际可交由专门判卷模块）
        List<ExamPaperQuestion> epqs = getPaperQuestions(paper.getId());
        Map<Long, ExamAnswer> answers = getAnswerMap(record.getId());
        List<Long> qIds = epqs.stream().map(ExamPaperQuestion::getQuestionId).toList();
        List<Question> questions = questionMapper.selectBatchIds(qIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        BigDecimal objectiveScore = BigDecimal.ZERO;
        for (ExamPaperQuestion epq : epqs) {
            Question q = questionMap.get(epq.getQuestionId());
            if (q == null) continue;
            ExamAnswer ans = answers.get(q.getId());
            if (ans == null) continue;

            String type = q.getType();
            boolean isObjective = QuestionType.SINGLE.getCode().equals(type)
                    || QuestionType.MULTIPLE.getCode().equals(type)
                    || QuestionType.JUDGE.getCode().equals(type)
                    || QuestionType.BLANK.getCode().equals(type);
            if (!isObjective) {
                continue;
            }
            BigDecimal fullScore = epq.getQuestionScore() != null ? epq.getQuestionScore() : q.getScore();
            if (fullScore == null) {
                fullScore = BigDecimal.ZERO;
            }
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
        record.setStatus(2); // 已提交
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

        List<ExamPaperQuestion> epqs = getPaperQuestions(paper.getId());
        Map<Long, ExamAnswer> answerMap = getAnswerMap(record.getId());
        Map<String, Object> view = buildQuestionView(exam, paper, record, epqs, answerMap, true);
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
                .eq(ExamRecord::getDeleted, 0);
        ExamRecord record = examRecordMapper.selectOne(q);
        if (record == null) {
            throw new BusinessException(ErrorCode.EXAM_NOT_STARTED_BY_USER);
        }
        return record;
    }

    private long getQuestionCountForPaper(Long paperId) {
        LambdaQueryWrapper<ExamPaperQuestion> q = new LambdaQueryWrapper<>();
        q.eq(ExamPaperQuestion::getPaperId, paperId);
        return examPaperQuestionMapper.selectCount(q);
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

    private void upsertAnswer(Long recordId, Long questionId, String answer) {
        if (recordId == null || questionId == null) return;
        LambdaQueryWrapper<ExamAnswer> q = new LambdaQueryWrapper<>();
        q.eq(ExamAnswer::getRecordId, recordId)
                .eq(ExamAnswer::getQuestionId, questionId)
                .eq(ExamAnswer::getDeleted, 0);
        ExamAnswer existing = examAnswerMapper.selectOne(q);
        if (existing == null) {
            existing = new ExamAnswer();
            existing.setRecordId(recordId);
            existing.setQuestionId(questionId);
            existing.setUserAnswer(answer);
            existing.setDeleted(0);
            examAnswerMapper.insert(existing);
        } else {
            existing.setUserAnswer(answer);
            examAnswerMapper.updateById(existing);
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
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BusinessException(ErrorCode.EXAM_TIME_EXPIRED);
        }
        if (record.getStatus() == null || record.getStatus() != 1) {
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

    private Map<String, Object> buildQuestionView(Exam exam,
                                                  ExamPaper paper,
                                                  ExamRecord record,
                                                  List<ExamPaperQuestion> epqs,
                                                  Map<Long, ExamAnswer> answers,
                                                  boolean includeAnswers) {
        List<Long> qIds = epqs.stream().map(ExamPaperQuestion::getQuestionId).toList();
        List<Question> questions = questionMapper.selectBatchIds(qIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<Map<String, Object>> questionViews = new ArrayList<>();
        for (ExamPaperQuestion epq : epqs) {
            Question q = questionMap.get(epq.getQuestionId());
            if (q == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", q.getId());
            m.put("type", q.getType());
            m.put("stem", q.getStem());
            m.put("optionsJson", q.getOptionsJson());
            if (includeAnswers) {
                ExamAnswer ans = answers.get(q.getId());
                m.put("userAnswer", ans != null ? ans.getUserAnswer() : null);
            }
            m.put("score", epq.getQuestionScore());
            m.put("difficulty", q.getDifficulty());
            m.put("order", epq.getQuestionOrder());
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

