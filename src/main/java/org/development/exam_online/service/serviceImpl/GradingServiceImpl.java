package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.constants.ExamRecordStatus;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.dao.entity.*;
import org.development.exam_online.dao.mapper.*;
import org.development.exam_online.service.AnalysisService;
import org.development.exam_online.service.GradingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradingServiceImpl implements GradingService {

    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ExamPaperQuestionSnapshotMapper snapshotMapper;
    private final QuestionMapper questionMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final UserMapper userMapper;
    private final AnalysisService analysisService;
    private final AiAnalysisReportMapper aiAnalysisReportMapper;
    private final ExamCheatLogMapper examCheatLogMapper;

    private static final ThreadPoolExecutor AI_REPORT_EXECUTOR = new ThreadPoolExecutor(
            2,
            10,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactory() {
                private final java.util.concurrent.atomic.AtomicInteger threadNumber = new java.util.concurrent.atomic.AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "ai-report-" + threadNumber.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Override
    public PageResult<Map<String, Object>> getPendingGradingRecords(Long examId, Long page, Long size) {
        long p = (page == null || page < 1) ? 1 : page;
        long s = (size == null || size < 1) ? 10 : size;

        LambdaQueryWrapper<ExamRecord> q = new LambdaQueryWrapper<>();
        q.eq(ExamRecord::getDeleted, 0)
                .in(ExamRecord::getStatus, 
                    ExamRecordStatus.SUBMITTED_UNGRADED, 
                    ExamRecordStatus.SUBMITTED_GRADING); // 待评分或评分中
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
            m.put("statusDesc", ExamRecordStatus.getDescription(r.getStatus()));
            m.put("studentView", ExamRecordStatus.getStudentView(r.getStatus()));
            m.put("teacherView", ExamRecordStatus.getTeacherView(r.getStatus()));
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

        List<ExamPaperQuestionSnapshot> snapshots = getSnapshotQuestions(exam.getId());
        Map<Long, ExamAnswer> answerMap = getAnswerMap(recordId);

        List<Long> qIds = snapshots.stream().map(ExamPaperQuestionSnapshot::getQuestionId).toList();
        Map<Long, Question> questionMap = questionMapper.selectBatchIdsIgnoreDeleted(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<Map<String, Object>> questionViews = new ArrayList<>();
        for (ExamPaperQuestionSnapshot snap : snapshots) {
            Question q = questionMap.get(snap.getQuestionId());
            if (q == null) continue;
            ExamAnswer ans = answerMap.get(q.getId());

            String type = q.getType();
            boolean isObjective = QuestionType.SINGLE.getCode().equals(type)
                    || QuestionType.MULTIPLE.getCode().equals(type)
                    || QuestionType.JUDGE.getCode().equals(type)
                    || QuestionType.BLANK.getCode().equals(type);

            BigDecimal fullScore = snap.getQuestionScore() != null ? snap.getQuestionScore() : q.getScore();
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", q.getId());
            m.put("type", type);
            m.put("stem", q.getStem());
            m.put("optionsJson", q.getOptionsJson());
            m.put("answerJson", q.getAnswerJson());
            m.put("userAnswer", ans != null ? ans.getUserAnswer() : null);
            m.put("score", ans != null ? ans.getScore() : null);
            m.put("isManualGraded", ans != null ? ans.getIsManualGraded() : null);
            m.put("fullScore", fullScore);
            m.put("difficulty", q.getDifficulty());
            m.put("order", snap.getQuestionOrder());
            m.put("objective", isObjective);
            questionViews.add(m);
        }

        // 查询切屏记录
        LambdaQueryWrapper<ExamCheatLog> cheatQuery = new LambdaQueryWrapper<>();
        cheatQuery.eq(ExamCheatLog::getExamId, exam.getId())
                .eq(ExamCheatLog::getUserId, record.getUserId())
                .eq(ExamCheatLog::getDeleted, 0)
                .orderByDesc(ExamCheatLog::getCount);
        List<ExamCheatLog> cheatLogs = examCheatLogMapper.selectList(cheatQuery);
        
        // 转换切屏记录为前端格式
        List<Map<String, Object>> cheatLogViews = new ArrayList<>();
        int totalCheatCount = 0;
        for (ExamCheatLog log : cheatLogs) {
            Map<String, Object> logView = new HashMap<>();
            logView.put("cheatType", log.getCheatType());
            logView.put("count", log.getCount());
            logView.put("lastTime", log.getLastTime());
            cheatLogViews.add(logView);
            totalCheatCount += log.getCount();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("exam", exam);
        result.put("record", record);
        result.put("paper", paper);
        result.put("questions", questionViews);
        result.put("cheatLogs", cheatLogViews);
        result.put("totalCheatCount", totalCheatCount);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gradeQuestion(Long recordId, Long questionId, BigDecimal score) {
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.SUBJECTIVE_QUESTION_SCORE_INVALID);
        }
        ExamRecord record = requireRecord(recordId);
        
        // 检查考试记录状态，状态5（已结束/已评分）不允许修改
        if (record.getStatus() != null && record.getStatus() == ExamRecordStatus.FINISHED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "成绩已确认，不允许修改评分");
        }
        
        Exam exam = requireExam(record.getExamId());
        ExamPaper paper = requirePaper(exam.getPaperId());

        // 从快照获取题目列表
        List<ExamPaperQuestionSnapshot> snapshots = getSnapshotQuestions(exam.getId());
        Map<Long, ExamPaperQuestionSnapshot> snapMap = snapshots.stream()
                .collect(Collectors.toMap(ExamPaperQuestionSnapshot::getQuestionId, e -> e));
        ExamPaperQuestionSnapshot snap = snapMap.get(questionId);
        if (snap == null) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_QUESTION_NOT_FOUND);
        }

        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }

        // 允许教师对任何题目（包括客观题）进行手动评分，以覆盖自动判分结果
        BigDecimal fullScore = snap.getQuestionScore() != null ? snap.getQuestionScore() : question.getScore();
        if (fullScore == null) fullScore = BigDecimal.ZERO;
        if (score.compareTo(fullScore) > 0) {
            throw new BusinessException(ErrorCode.SUBJECTIVE_QUESTION_SCORE_EXCEED);
        }

        // 更新答案得分，并标记为人工评分
        ExamAnswer answer = getOrCreateAnswer(recordId, questionId);
        answer.setScore(score);
        answer.setIsManualGraded(1); // 标记为人工评分
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
            autoGradeRecord(recordId, false);
        }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoGradeRecord(Long recordId, boolean forceOverride) {
            ExamRecord record = requireRecord(recordId);
            
            // 检查考试记录状态，状态5（已结束/已评分）不允许修改
            if (record.getStatus() != null && record.getStatus() == ExamRecordStatus.FINISHED) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "成绩已确认，不允许重新判分");
            }
            
            Exam exam = requireExam(record.getExamId());
            ExamPaper paper = requirePaper(exam.getPaperId());
            List<ExamPaperQuestionSnapshot> snapshots = getSnapshotQuestions(exam.getId());
            Map<Long, ExamAnswer> answerMap = getAnswerMap(recordId);

            List<Long> qIds = snapshots.stream().map(ExamPaperQuestionSnapshot::getQuestionId).toList();
            Map<Long, Question> questionMap = questionMapper.selectBatchIdsIgnoreDeleted(qIds).stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));

            BigDecimal objectiveScore = BigDecimal.ZERO;
            for (ExamPaperQuestionSnapshot snap : snapshots) {
                Question q = questionMap.get(snap.getQuestionId());
                if (q == null) continue;
                String type = q.getType();
                boolean isObjective = QuestionType.SINGLE.getCode().equals(type)
                        || QuestionType.MULTIPLE.getCode().equals(type)
                        || QuestionType.JUDGE.getCode().equals(type)
                        || QuestionType.BLANK.getCode().equals(type);
                if (!isObjective) continue;

                ExamAnswer ans = answerMap.get(q.getId());
                if (ans == null) continue;

                // 如果不强制覆盖，跳过已人工评分的题目
                if (!forceOverride && ans.getIsManualGraded() != null && ans.getIsManualGraded() == 1) {
                    // 已人工评分的题目，累加其分数但不重新判分
                    if (ans.getScore() != null) {
                        objectiveScore = objectiveScore.add(ans.getScore());
                    }
                    continue;
                }

                BigDecimal fullScore = snap.getQuestionScore() != null ? snap.getQuestionScore() : q.getScore();
                if (fullScore == null) fullScore = BigDecimal.ZERO;

                // 标准化答案：去除首尾空格后比较
                String correctAnswer = normalizeAnswer(q.getAnswerJson());
                String userAnswer = normalizeAnswer(ans.getUserAnswer());

                if (Objects.equals(correctAnswer, userAnswer)) {
                    ans.setScore(fullScore);
                    objectiveScore = objectiveScore.add(fullScore);
                } else {
                    ans.setScore(BigDecimal.ZERO);
                }
                ans.setIsManualGraded(0); // 标记为自动判分
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
                .filter(r -> ExamRecordStatus.isInProgress(r.getStatus()))
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
        List<ExamPaperQuestionSnapshot> snapshots = getSnapshotQuestions(examId);

        if (snapshots.isEmpty()) return Collections.emptyList();

        List<Long> qIds = snapshots.stream().map(ExamPaperQuestionSnapshot::getQuestionId).toList();
        Map<Long, Question> questionMap = questionMapper.selectBatchIdsIgnoreDeleted(qIds).stream()
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
            m.put("statusDesc", ExamRecordStatus.getDescription(r.getStatus()));
            m.put("studentView", ExamRecordStatus.getStudentView(r.getStatus()));
            m.put("teacherView", ExamRecordStatus.getTeacherView(r.getStatus()));
            
            // 只有状态为5（已结束/已评分）时才显示分数，否则隐藏
            if (r.getStatus() != null && r.getStatus() == ExamRecordStatus.FINISHED) {
                m.put("objectiveScore", r.getObjectiveScore());
                m.put("subjectiveScore", r.getSubjectiveScore());
                m.put("totalScore", r.getTotalScore());
            } else {
                m.put("objectiveScore", null);
                m.put("subjectiveScore", null);
                m.put("totalScore", null);
            }
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
        
        // 只有状态为5（已结束/已评分）时才能查看成绩和答案详情
        boolean canViewGrades = record.getStatus() != null && record.getStatus() == ExamRecordStatus.FINISHED;
        
        Exam exam = requireExam(record.getExamId());
        ExamPaper paper = requirePaper(exam.getPaperId());

        List<ExamPaperQuestionSnapshot> snapshots = getSnapshotQuestions(exam.getId());
        Map<Long, ExamAnswer> answerMap = getAnswerMap(recordId);

        List<Long> qIds = snapshots.stream().map(ExamPaperQuestionSnapshot::getQuestionId).toList();
        Map<Long, Question> questionMap = questionMapper.selectBatchIdsIgnoreDeleted(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<Map<String, Object>> questionViews = new ArrayList<>();
        for (ExamPaperQuestionSnapshot snap : snapshots) {
            Question q = questionMap.get(snap.getQuestionId());
            if (q == null) continue;
            ExamAnswer ans = answerMap.get(q.getId());

            BigDecimal fullScore = snap.getQuestionScore() != null ? snap.getQuestionScore() : q.getScore();
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", q.getId());
            m.put("type", q.getType());
            m.put("stem", q.getStem());
            m.put("optionsJson", q.getOptionsJson());
            
            // 只有成绩已发布时才显示正确答案、学生答案和得分
            if (canViewGrades) {
                m.put("answerJson", q.getAnswerJson());
                m.put("userAnswer", ans != null ? ans.getUserAnswer() : null);
                m.put("score", ans != null ? ans.getScore() : null);
            } else {
                m.put("answerJson", null);
                m.put("userAnswer", ans != null ? ans.getUserAnswer() : null);
                m.put("score", null);
            }
            
            m.put("fullScore", fullScore);
            m.put("difficulty", q.getDifficulty());
            m.put("order", snap.getQuestionOrder());
            questionViews.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("exam", exam);
        
        // 只有成绩已发布时才显示分数信息
        if (canViewGrades) {
            result.put("record", record);
        } else {
            // 隐藏分数字段
            ExamRecord recordCopy = new ExamRecord();
            recordCopy.setId(record.getId());
            recordCopy.setExamId(record.getExamId());
            recordCopy.setUserId(record.getUserId());
            recordCopy.setStartTime(record.getStartTime());
            recordCopy.setSubmitTime(record.getSubmitTime());
            recordCopy.setStatus(record.getStatus());
            recordCopy.setObjectiveScore(null);
            recordCopy.setSubjectiveScore(null);
            recordCopy.setTotalScore(null);
            result.put("record", recordCopy);
        }
        
        result.put("paper", paper);
        result.put("questions", questionViews);
        result.put("canViewGrades", canViewGrades);
        
        // 如果成绩已发布，添加AI分析报告
        if (canViewGrades) {
            Map<String, Object> aiReport = getExamRecordAiReport(recordId);
            result.put("aiReport", aiReport);
        } else {
            result.put("aiReport", null);
        }
        
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
        List<ExamAnswer> list = examAnswerMapper.selectList(q);
        if (list == null || list.isEmpty()) return Collections.emptyMap();
        return list.stream().collect(Collectors.toMap(ExamAnswer::getQuestionId, a -> a));
    }

    private ExamAnswer getOrCreateAnswer(Long recordId, Long questionId) {
        // 使用自定义方法查询，忽略逻辑删除状态
        ExamAnswer ans = examAnswerMapper.selectByRecordAndQuestionIgnoreDeleted(recordId, questionId);
        
        if (ans == null) {
            // 确实不存在，创建新记录
            try {
                ans = new ExamAnswer();
                ans.setRecordId(recordId);
                ans.setQuestionId(questionId);
                ans.setDeleted(0);
                ans.setIsManualGraded(0);
                examAnswerMapper.insert(ans);
            } catch (Exception e) {
                // 如果插入失败（并发导致的唯一索引冲突），再次查询
                if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                    ans = examAnswerMapper.selectByRecordAndQuestionIgnoreDeleted(recordId, questionId);
                    if (ans == null) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        
        // 如果记录是逻辑删除状态，恢复它
        if (ans != null && ans.getDeleted() != null && ans.getDeleted() == 1) {
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ExamAnswer> uw =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            uw.eq(ExamAnswer::getId, ans.getId())
              .set(ExamAnswer::getDeleted, 0)
              .set(ExamAnswer::getIsManualGraded, 0)
              .set(ExamAnswer::getUserAnswer, null)
              .set(ExamAnswer::getScore, null);
            examAnswerMapper.update(null, uw);
            ans.setDeleted(0);
            ans.setIsManualGraded(0);
            ans.setUserAnswer(null);
            ans.setScore(null);
        }
        return ans;
    }

    private void recalcRecordScores(Long recordId, Exam exam, ExamPaper paper) {
        ExamRecord record = requireRecord(recordId);
        List<ExamPaperQuestionSnapshot> snapshots = getSnapshotQuestions(exam.getId());
        List<Long> qIds = snapshots.stream().map(ExamPaperQuestionSnapshot::getQuestionId).toList();
        Map<Long, Question> qMap = questionMapper.selectBatchIdsIgnoreDeleted(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        Map<Long, ExamAnswer> ansMap = getAnswerMap(recordId);

        BigDecimal subjectiveScore = BigDecimal.ZERO;
        BigDecimal objectiveScore = BigDecimal.ZERO;

        for (ExamPaperQuestionSnapshot snap : snapshots) {
            Question q = qMap.get(snap.getQuestionId());
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
        // 教师完成全部评分
        record.setStatus(ExamRecordStatus.SUBMITTED_GRADED);
        examRecordMapper.updateById(record);
    }


    @Override
    public Map<String, Object> getRecordScoreInfo(Long recordId) {
        ExamRecord record = requireRecord(recordId);
        Map<String, Object> result = new HashMap<>();
        result.put("objectiveScore", record.getObjectiveScore());
        result.put("subjectiveScore", record.getSubjectiveScore());
        result.put("totalScore", record.getTotalScore());

        // 从快照计算试卷总分，避免试卷修改后影响已有记录
        Exam exam = requireExam(record.getExamId());
        List<ExamPaperQuestionSnapshot> snapshots = getSnapshotQuestions(exam.getId());
        BigDecimal snapshotTotalScore = snapshots.stream()
                .map(s -> s.getQuestionScore() != null ? s.getQuestionScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("fullScore", snapshotTotalScore);

        return result;
    }

    @Override
    public List<Map<String, Object>> getExamRecords(Long examId) {
        requireExam(examId);
        
        LambdaQueryWrapper<ExamRecord> q = new LambdaQueryWrapper<>();
        q.eq(ExamRecord::getExamId, examId)
         .orderByDesc(ExamRecord::getSubmitTime);
        List<ExamRecord> records = examRecordMapper.selectList(q);
        
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 获取学生信息
        List<Long> userIds = records.stream()
                .map(ExamRecord::getUserId)
                .distinct()
                .toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("recordId", record.getId());
            item.put("userId", record.getUserId());
            User user = userMap.get(record.getUserId());
            item.put("studentName", user != null ? user.getRealName() : "未知");
            item.put("status", record.getStatus());
            item.put("statusDesc", ExamRecordStatus.getDescription(record.getStatus()));
            item.put("studentView", ExamRecordStatus.getStudentView(record.getStatus()));
            item.put("teacherView", ExamRecordStatus.getTeacherView(record.getStatus()));
            item.put("submitTime", record.getSubmitTime());
            item.put("totalScore", record.getTotalScore());
            item.put("objectiveScore", record.getObjectiveScore());
            item.put("subjectiveScore", record.getSubjectiveScore());
            result.add(item);
        }
        
        return result;
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? null : answer.trim();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmGrading(Long recordId) {
        ExamRecord record = requireRecord(recordId);
        if (!ExamRecordStatus.isGraded(record.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, 
                "只有已评分的记录才能确认，当前状态：" + ExamRecordStatus.getDescription(record.getStatus()));
        }
        if (record.getStatus() == ExamRecordStatus.FINISHED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该记录已确认评分，无需重复操作");
        }
        record.setStatus(ExamRecordStatus.FINISHED);
        examRecordMapper.updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmExamGrading(Long examId) {
        requireExam(examId);
        LambdaQueryWrapper<ExamRecord> q = new LambdaQueryWrapper<>();
        q.eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStatus, ExamRecordStatus.SUBMITTED_GRADED)
                .eq(ExamRecord::getDeleted, 0);
        List<ExamRecord> records = examRecordMapper.selectList(q);

        List<Long> userIds = new ArrayList<>();

        for (ExamRecord record : records) {
            record.setStatus(ExamRecordStatus.FINISHED);
            examRecordMapper.updateById(record);
            userIds.add(record.getUserId());
        }

        // 异步生成AI分析报告
        if (!userIds.isEmpty()) {
            generateReportsAsync(examId, userIds);
        }

        return records.size();
    }

    private void generateReportsAsync(Long examId, List<Long> userIds) {
        for (Long userId : userIds) {
            AI_REPORT_EXECUTOR.execute(() -> {
                try {
                    analysisService.generateExamReport(userId, examId);
                } catch (Exception e) {
                    // 系统错误报告
                    System.err.println("为学生 " + userId + " 生成考试 " + examId + " 的AI报告失败: " + e.getMessage());
                }
            });
        }
    }

    @Override
    public Map<String, Object> getExamRecordAiReport(Long recordId) {
        ExamRecord record = requireRecord(recordId);
        
        // 查询该学生该考试的AI分析报告
        LambdaQueryWrapper<AiAnalysisReport> q = new LambdaQueryWrapper<>();
        q.eq(AiAnalysisReport::getUserId, record.getUserId())
         .eq(AiAnalysisReport::getExamIds, String.valueOf(record.getExamId()))
         .eq(AiAnalysisReport::getReportType, "single_exam")
         .eq(AiAnalysisReport::getDeleted, 0)
         .orderByDesc(AiAnalysisReport::getCreatedTime)
         .last("LIMIT 1");
        
        List<AiAnalysisReport> reports = aiAnalysisReportMapper.selectList(q);
        if (reports.isEmpty()) {
            return null;
        }
        
        AiAnalysisReport report = reports.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("reportId", report.getId());
        result.put("aiReport", report.getAiReport());
        result.put("modelName", report.getModelName());
        result.put("createdTime", report.getCreatedTime());
        result.put("generationTime", report.getGenerationTime());
        
        return result;
    }

}
