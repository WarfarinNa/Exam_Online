package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.dao.entity.Exam;
import org.development.exam_online.dao.entity.ExamPaper;
import org.development.exam_online.dao.entity.ExamRecord;
import org.development.exam_online.dao.mapper.ExamMapper;
import org.development.exam_online.dao.mapper.ExamPaperMapper;
import org.development.exam_online.dao.mapper.ExamRecordMapper;
import org.development.exam_online.service.ExamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 考试服务实现类
 */
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamMapper examMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建考试
     * @param exam 考试信息
     * @return 创建的考试
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Exam createExam(Exam exam) {
        // 验证试卷是否存在
        if (exam.getPaperId() == null) {
            throw new RuntimeException("试卷ID不能为空");
        }
        ExamPaper paper = examPaperMapper.selectById(exam.getPaperId());
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        // 验证考试名称
        if (!StringUtils.hasText(exam.getName())) {
            throw new RuntimeException("考试名称不能为空");
        }

        // 验证考试时间
        if (exam.getStartTime() == null || exam.getEndTime() == null) {
            throw new RuntimeException("考试开始时间和结束时间不能为空");
        }
        if (exam.getEndTime().isBefore(exam.getStartTime()) || exam.getEndTime().isEqual(exam.getStartTime())) {
            throw new RuntimeException("考试结束时间必须晚于开始时间");
        }

        // 设置创建时间
        exam.setCreatedAt(LocalDateTime.now());

        int result = examMapper.insert(exam);
        if (result > 0) {
            return examMapper.selectById(exam.getId());
        } else {
            throw new RuntimeException("创建考试失败");
        }
    }

    /**
     * 发布试卷为考试
     * @param paperId 试卷ID
     * @param exam 考试信息
     * @return 创建的考试
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Exam publishPaper(Long paperId, Exam exam) {
        // 验证试卷是否存在
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        // 设置试卷ID
        exam.setPaperId(paperId);

        // 如果考试名称为空，使用试卷名称
        if (!StringUtils.hasText(exam.getName())) {
            exam.setName(paper.getName());
        }

        return createExam(exam);
    }

    /**
     * 根据ID获取考试详情
     * @param examId 考试ID
     * @return 考试详情
     */
    @Override
    public Exam getExamById(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        return exam;
    }

    /**
     * 更新考试信息
     * @param examId 考试ID
     * @param exam 考试信息
     * @return 更新结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateExam(Long examId, Exam exam) {
        Exam existingExam = examMapper.selectById(examId);
        if (existingExam == null) {
            throw new RuntimeException("考试不存在");
        }

        // 如果更新了试卷ID，验证试卷是否存在
        if (exam.getPaperId() != null && !exam.getPaperId().equals(existingExam.getPaperId())) {
            ExamPaper paper = examPaperMapper.selectById(exam.getPaperId());
            if (paper == null) {
                throw new RuntimeException("试卷不存在");
            }
        }

        // 如果更新了时间，验证时间有效性
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            if (exam.getEndTime().isBefore(exam.getStartTime()) || exam.getEndTime().isEqual(exam.getStartTime())) {
                throw new RuntimeException("考试结束时间必须晚于开始时间");
            }
        }

        // 设置ID
        exam.setId(examId);

        int result = examMapper.updateById(exam);
        if (result > 0) {
            return "考试信息更新成功";
        } else {
            throw new RuntimeException("考试信息更新失败");
        }
    }

    /**
     * 设置考试时间
     * @param examId 考试ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 设置结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String setExamTime(Long examId, LocalDateTime startTime, LocalDateTime endTime) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }

        if (startTime == null || endTime == null) {
            throw new RuntimeException("考试开始时间和结束时间不能为空");
        }

        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new RuntimeException("考试结束时间必须晚于开始时间");
        }

        Exam updateExam = new Exam();
        updateExam.setId(examId);
        updateExam.setStartTime(startTime);
        updateExam.setEndTime(endTime);

        int result = examMapper.updateById(updateExam);
        if (result > 0) {
            return "考试时间设置成功";
        } else {
            throw new RuntimeException("考试时间设置失败");
        }
    }

    /**
     * 设置考试权限范围
     * @param examId 考试ID
     * @param allowRoles 允许的角色列表
     * @return 设置结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String setExamPermissions(Long examId, List<String> allowRoles) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }

        if (allowRoles == null || allowRoles.isEmpty()) {
            throw new RuntimeException("允许的角色列表不能为空");
        }

        // 将角色列表转换为JSON字符串存储
        String allowRolesJson;
        try {
            allowRolesJson = objectMapper.writeValueAsString(allowRoles);
        } catch (Exception e) {
            throw new RuntimeException("角色列表格式错误");
        }

        Exam updateExam = new Exam();
        updateExam.setId(examId);
        updateExam.setAllowRoles(allowRolesJson);

        int result = examMapper.updateById(updateExam);
        if (result > 0) {
            return "考试权限设置成功";
        } else {
            throw new RuntimeException("考试权限设置失败");
        }
    }

    /**
     * 删除考试
     * @param examId 考试ID
     * @return 删除结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }

        // 检查是否有考试记录
        LambdaQueryWrapper<ExamRecord> recordQuery = new LambdaQueryWrapper<>();
        recordQuery.eq(ExamRecord::getExamId, examId);
        Long recordCount = examRecordMapper.selectCount(recordQuery);

        if (recordCount > 0) {
            throw new RuntimeException("该考试已有 " + recordCount + " 条考试记录，无法删除");
        }

        int result = examMapper.deleteById(examId);
        if (result > 0) {
            return "考试删除成功";
        } else {
            throw new RuntimeException("考试删除失败");
        }
    }

    /**
     * 获取考试列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param keyword 搜索关键词
     * @param paperId 试卷ID筛选
     * @param startTime 开始时间筛选
     * @param endTime 结束时间筛选
     * @return 分页结果
     */
    @Override
    public PageResult<Exam> getExamList(Integer pageNum, Integer pageSize, String keyword, Long paperId, LocalDateTime startTime, LocalDateTime endTime) {
        Page<Exam> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Exam> query = new LambdaQueryWrapper<>();

        // 关键词搜索（考试名称）
        if (StringUtils.hasText(keyword)) {
            query.like(Exam::getName, keyword);
        }

        // 试卷ID筛选
        if (paperId != null) {
            query.eq(Exam::getPaperId, paperId);
        }

        // 开始时间筛选（查询此时间之后的考试）
        if (startTime != null) {
            query.ge(Exam::getStartTime, startTime);
        }

        // 结束时间筛选（查询此时间之前的考试）
        if (endTime != null) {
            query.le(Exam::getEndTime, endTime);
        }

        // 按创建时间倒序排列
        query.orderByDesc(Exam::getCreatedAt);

        IPage<Exam> pageResult = examMapper.selectPage(page, query);

        return PageResult.of(pageResult.getTotal(), pageNum, pageSize, pageResult.getRecords());
    }

    /**
     * 获取已发布的考试列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @Override
    public PageResult<Exam> getPublishedExams(Integer pageNum, Integer pageSize) {
        Page<Exam> page = new Page<>(pageNum, pageSize);

        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Exam> query = new LambdaQueryWrapper<>();
        // 查询当前时间在考试时间范围内的考试（已开始但未结束）
        query.le(Exam::getStartTime, now)
                .ge(Exam::getEndTime, now)
                .orderByDesc(Exam::getStartTime);

        IPage<Exam> pageResult = examMapper.selectPage(page, query);

        return PageResult.of(pageResult.getTotal(), pageNum, pageSize, pageResult.getRecords());
    }

    /**
     * 获取我创建的考试列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @Override
    public PageResult<Exam> getMyExams(Long userId, Integer pageNum, Integer pageSize) {
        Page<Exam> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Exam> query = new LambdaQueryWrapper<>();
        query.eq(Exam::getCreatedBy, userId)
                .orderByDesc(Exam::getCreatedAt);

        IPage<Exam> pageResult = examMapper.selectPage(page, query);

        return PageResult.of(pageResult.getTotal(), pageNum, pageSize, pageResult.getRecords());
    }

    /**
     * 取消发布考试
     * @param examId 考试ID
     * @return 取消结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String unpublishExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }

        // 将考试结束时间设置为当前时间之前，使其不再显示在已发布列表中
        Exam updateExam = new Exam();
        updateExam.setId(examId);
        updateExam.setEndTime(LocalDateTime.now().minusMinutes(1));

        int result = examMapper.updateById(updateExam);
        if (result > 0) {
            return "考试已取消发布";
        } else {
            throw new RuntimeException("取消发布失败");
        }
    }

    /**
     * 获取考试的统计信息
     * @param examId 考试ID
     * @return 统计信息
     */
    @Override
    public Map<String, Object> getExamStatistics(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }

        // 查询考试记录
        LambdaQueryWrapper<ExamRecord> recordQuery = new LambdaQueryWrapper<>();
        recordQuery.eq(ExamRecord::getExamId, examId);
        List<ExamRecord> records = examRecordMapper.selectList(recordQuery);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("examId", examId);
        statistics.put("examName", exam.getName());
        statistics.put("totalParticipants", records.size());

        if (records.isEmpty()) {
            statistics.put("completedCount", 0);
            statistics.put("inProgressCount", 0);
            statistics.put("notStartedCount", 0);
            statistics.put("averageScore", 0);
            statistics.put("highestScore", 0);
            statistics.put("lowestScore", 0);
        } else {
            // 统计已完成、进行中、未开始的考试
            long completedCount = records.stream()
                    .filter(r -> "completed".equals(r.getStatus()) || r.getSubmitTime() != null)
                    .count();
            long inProgressCount = records.stream()
                    .filter(r -> "in_progress".equals(r.getStatus()) && r.getSubmitTime() == null)
                    .count();
            long notStartedCount = records.stream()
                    .filter(r -> r.getStartTime() == null)
                    .count();

            statistics.put("completedCount", completedCount);
            statistics.put("inProgressCount", inProgressCount);
            statistics.put("notStartedCount", notStartedCount);

            // 计算平均分、最高分、最低分（只统计已提交的）
            List<ExamRecord> submittedRecords = records.stream()
                    .filter(r -> r.getTotalScore() != null)
                    .collect(Collectors.toList());

            if (!submittedRecords.isEmpty()) {
                BigDecimal totalScore = submittedRecords.stream()
                        .map(ExamRecord::getTotalScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal averageScore = totalScore.divide(
                        BigDecimal.valueOf(submittedRecords.size()),
                        2,
                        RoundingMode.HALF_UP
                );

                BigDecimal highestScore = submittedRecords.stream()
                        .map(ExamRecord::getTotalScore)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                BigDecimal lowestScore = submittedRecords.stream()
                        .map(ExamRecord::getTotalScore)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                statistics.put("averageScore", averageScore.doubleValue());
                statistics.put("highestScore", highestScore.doubleValue());
                statistics.put("lowestScore", lowestScore.doubleValue());
            } else {
                statistics.put("averageScore", 0);
                statistics.put("highestScore", 0);
                statistics.put("lowestScore", 0);
            }
        }

        return statistics;
    }
}
