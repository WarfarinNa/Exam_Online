package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.dao.entity.Exam;
import org.development.exam_online.dao.entity.ExamPaper;
import org.development.exam_online.dao.entity.ExamRecord;
import org.development.exam_online.dao.mapper.ExamMapper;
import org.development.exam_online.dao.mapper.ExamPaperMapper;
import org.development.exam_online.dao.mapper.ExamRecordMapper;
import org.development.exam_online.security.AuthContext;
import org.development.exam_online.service.ExamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamMapper examMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Exam createExam(Exam exam) {
        if (exam == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "考试信息不能为空");
        }
        if (exam.getPaperId() == null) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_NOT_FOUND);
        }
        ExamPaper paper = examPaperMapper.selectById(exam.getPaperId());
        if (paper == null || Objects.equals(paper.getDeleted(), 1)) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_NOT_FOUND);
        }
        exam.setId(null);
        exam.setDeleted(0);
        if (exam.getCreatedBy() == null) {
            Long userId = AuthContext.getUserId();
            if (userId != null) {
                exam.setCreatedBy(userId);
            }
        }
        // 默认草稿状态 0
        if (exam.getStatus() == null) {
            exam.setStatus(0);
        }
        int inserted = examMapper.insert(exam);
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "创建考试失败");
        }
        return examMapper.selectById(exam.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Exam publishPaper(Long paperId, Exam exam) {
        if (paperId == null) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_NOT_FOUND);
        }
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null || Objects.equals(paper.getDeleted(), 1)) {
            throw new BusinessException(ErrorCode.EXAM_PAPER_NOT_FOUND);
        }
        if (exam == null) {
            exam = new Exam();
        }
        exam.setId(null);
        exam.setPaperId(paperId);
        if (!StringUtils.hasText(exam.getName())) {
            exam.setName(paper.getName());
        }
        if (exam.getCreatedBy() == null) {
            Long userId = AuthContext.getUserId();
            if (userId != null) {
                exam.setCreatedBy(userId);
            }
        }
        // 发布状态 1
        exam.setStatus(1);
        if (exam.getStartTime() == null || exam.getEndTime() == null) {
            // 若未设置时间，默认当前时间开始，按试卷时长推结束时间
            LocalDateTime now = LocalDateTime.now();
            exam.setStartTime(now);
            Integer duration = paper.getDuration();
            if (duration == null || duration <= 0) {
                duration = 60;
            }
            exam.setEndTime(now.plusMinutes(duration));
        }
        int inserted = examMapper.insert(exam);
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "发布考试失败");
        }
        return examMapper.selectById(exam.getId());
    }

    @Override
    public Exam getExamById(Long examId) {
        return requireActiveExam(examId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateExam(Long examId, Exam exam) {
        requireActiveExam(examId);
        if (exam == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "考试信息不能为空");
        }
        exam.setId(examId);
        int updated = examMapper.updateById(exam);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "更新考试失败");
        }
        return "更新成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String setExamTime(Long examId, LocalDateTime startTime, LocalDateTime endTime) {
        requireActiveExam(examId);
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.EXAM_DURATION_INVALID);
        }
        Exam update = new Exam();
        update.setId(examId);
        update.setStartTime(startTime);
        update.setEndTime(endTime);
        int updated = examMapper.updateById(update);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "设置考试时间失败");
        }
        return "考试时间设置成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String setExamPermissions(Long examId, List<String> allowRoles) {
        requireActiveExam(examId);
        Exam update = new Exam();
        update.setId(examId);
        try {
            String json = allowRoles == null ? "[]" : objectMapper.writeValueAsString(allowRoles);
            update.setAllowRoles(json);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "权限范围格式错误");
        }
        int updated = examMapper.updateById(update);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "设置考试权限失败");
        }
        return "考试权限设置成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteExam(Long examId) {
        requireActiveExam(examId);
        LambdaQueryWrapper<ExamRecord> q = new LambdaQueryWrapper<>();
        q.eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getDeleted, 0);
        Long count = examRecordMapper.selectCount(q);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.EXAM_HAS_RECORDS);
        }
        Exam update = new Exam();
        update.setId(examId);
        update.setDeleted(1);
        int updated = examMapper.updateById(update);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "删除考试失败");
        }
        return "删除成功";
    }

    @Override
    public PageResult<Exam> getExamList(Integer pageNum, Integer pageSize, String keyword, Long paperId, LocalDateTime startTime, LocalDateTime endTime) {
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<Exam> q = new LambdaQueryWrapper<>();
        q.eq(Exam::getDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            q.like(Exam::getName, keyword);
        }
        if (paperId != null) {
            q.eq(Exam::getPaperId, paperId);
        }
        if (startTime != null) {
            q.ge(Exam::getStartTime, startTime);
        }
        if (endTime != null) {
            q.le(Exam::getEndTime, endTime);
        }
        q.orderByDesc(Exam::getStartTime);

        Page<Exam> page = new Page<>(p, s);
        Page<Exam> result = examMapper.selectPage(page, q);
        return PageResult.of(result.getTotal(), p, s, result.getRecords());
    }

    @Override
    public PageResult<Exam> getPublishedExams(Integer pageNum, Integer pageSize) {
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Exam> q = new LambdaQueryWrapper<>();
        q.eq(Exam::getDeleted, 0)
                .eq(Exam::getStatus, 1)
                .le(Exam::getStartTime, now)
                .ge(Exam::getEndTime, now)
                .orderByAsc(Exam::getStartTime);

        Page<Exam> page = new Page<>(p, s);
        Page<Exam> result = examMapper.selectPage(page, q);
        return PageResult.of(result.getTotal(), p, s, result.getRecords());
    }

    @Override
    public PageResult<Exam> getMyExams(Long userId, Integer pageNum, Integer pageSize) {
        int p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<Exam> q = new LambdaQueryWrapper<>();
        q.eq(Exam::getDeleted, 0)
                .eq(Exam::getCreatedBy, userId)
                .orderByDesc(Exam::getStartTime);

        Page<Exam> page = new Page<>(p, s);
        Page<Exam> result = examMapper.selectPage(page, q);
        return PageResult.of(result.getTotal(), p, s, result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String unpublishExam(Long examId) {
        requireActiveExam(examId);
        // 取消发布：将结束时间设置为当前时间之前，或直接标记为结束状态2
        Exam update = new Exam();
        update.setId(examId);
        update.setStatus(2);
        update.setEndTime(LocalDateTime.now().minusMinutes(1));
        int updated = examMapper.updateById(update);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "取消发布失败");
        }
        return "考试已取消发布";
    }

    @Override
    public Map<String, Object> getExamStatistics(Long examId) {
        Exam exam = requireActiveExam(examId);
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
            return statistics;
        }

        long completedCount = records.stream()
                .filter(r -> (r.getStatus() != null && r.getStatus() >= 2) || r.getSubmitTime() != null)
                .count();
        long inProgressCount = records.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 1 && r.getSubmitTime() == null)
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
        } else {
            BigDecimal totalScore = submitted.stream()
                    .map(ExamRecord::getTotalScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = totalScore
                    .divide(BigDecimal.valueOf(submitted.size()), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal highest = submitted.stream()
                    .map(ExamRecord::getTotalScore)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            BigDecimal lowest = submitted.stream()
                    .map(ExamRecord::getTotalScore)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            statistics.put("averageScore", average.doubleValue());
            statistics.put("highestScore", highest.doubleValue());
            statistics.put("lowestScore", lowest.doubleValue());
        }

        return statistics;
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
}

