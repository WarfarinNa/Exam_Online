package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.PageResult;
import org.development.exam_online.common.enums.QuestionType;
import org.development.exam_online.dao.entity.Question;
import org.development.exam_online.dao.entity.QuestionCategory;
import org.development.exam_online.dao.mapper.QuestionCategoryMapper;
import org.development.exam_online.dao.mapper.QuestionMapper;
import org.development.exam_online.service.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目服务实现类
 */
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final QuestionCategoryMapper questionCategoryMapper;

    /**
     * 创建题目
     * @param question 题目信息
     * @return 创建的题目
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Question createQuestion(Question question) {
        // 验证分类是否存在
        if (question.getCategoryId() != null) {
            QuestionCategory category = questionCategoryMapper.selectById(question.getCategoryId());
            if (category == null) {
                throw new RuntimeException("题目分类不存在");
            }
        }

        // 验证题型
        if (!StringUtils.hasText(question.getType())) {
            throw new RuntimeException("题型不能为空");
        }
        
        // 验证题型是否有效
        try {
            QuestionType.of(question.getType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("题型格式错误：" + e.getMessage());
        }

        // 验证题目内容
        if (!StringUtils.hasText(question.getContent())) {
            throw new RuntimeException("题目内容不能为空");
        }

        // 验证答案
        if (!StringUtils.hasText(question.getAnswer())) {
            throw new RuntimeException("题目答案不能为空");
        }

        // 设置创建时间
        question.setCreatedAt(LocalDateTime.now());

        int result = questionMapper.insert(question);
        if (result > 0) {
            return questionMapper.selectById(question.getId());
        } else {
            throw new RuntimeException("创建题目失败");
        }
    }

    /**
     * 根据ID获取题目详情
     * @param questionId 题目ID
     * @return 题目详情
     */
    @Override
    public Question getQuestionById(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }
        return question;
    }

    /**
     * 更新题目
     * @param questionId 题目ID
     * @param question 题目信息
     * @return 更新结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateQuestion(Long questionId, Question question) {
        Question existingQuestion = questionMapper.selectById(questionId);
        if (existingQuestion == null) {
            throw new RuntimeException("题目不存在");
        }

        // 验证分类是否存在（如果提供了分类ID）
        if (question.getCategoryId() != null) {
            QuestionCategory category = questionCategoryMapper.selectById(question.getCategoryId());
            if (category == null) {
                throw new RuntimeException("题目分类不存在");
            }
        }

        // 验证题型是否有效（如果提供了题型）
        if (StringUtils.hasText(question.getType())) {
            try {
                QuestionType.of(question.getType());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("题型格式错误：" + e.getMessage());
            }
        }

        // 设置ID
        question.setId(questionId);

        int result = questionMapper.updateById(question);
        if (result > 0) {
            return "题目更新成功";
        } else {
            throw new RuntimeException("题目更新失败");
        }
    }

    /**
     * 删除题目
     * @param questionId 题目ID
     * @return 删除结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteQuestion(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }

        int result = questionMapper.deleteById(questionId);
        if (result > 0) {
            return "题目删除成功";
        } else {
            throw new RuntimeException("题目删除失败");
        }
    }

    /**
     * 批量删除题目
     * @param questionIds 题目ID列表
     * @return 删除结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteQuestions(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new RuntimeException("题目ID列表不能为空");
        }

        int deletedCount = 0;
        for (Long questionId : questionIds) {
            int result = questionMapper.deleteById(questionId);
            if (result > 0) {
                deletedCount++;
            }
        }

        return "成功删除 " + deletedCount + " 个题目";
    }

    /**
     * 获取题目列表（支持分页和搜索）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param type 题型筛选
     * @param categoryId 分类ID筛选
     * @param keyword 搜索关键词
     * @param createdBy 创建者ID筛选
     * @return 分页结果
     */
    @Override
    public PageResult<Question> getQuestionList(Integer pageNum, Integer pageSize, String type, Long categoryId, String keyword, Long createdBy) {
        Page<Question> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<>();

        // 题型筛选
        if (StringUtils.hasText(type)) {
            // 验证题型是否有效
            if (!QuestionType.isValid(type)) {
                throw new RuntimeException("题型格式错误：" + type);
            }
            query.eq(Question::getType, type);
        }

        // 分类筛选
        if (categoryId != null) {
            query.eq(Question::getCategoryId, categoryId);
        }

        // 关键词搜索（题目内容）
        if (StringUtils.hasText(keyword)) {
            query.like(Question::getContent, keyword);
        }

        // 创建者筛选
        if (createdBy != null) {
            query.eq(Question::getCreatedBy, createdBy);
        }

        // 按创建时间倒序排列
        query.orderByDesc(Question::getCreatedAt);

        IPage<Question> pageResult = questionMapper.selectPage(page, query);

        return PageResult.of(pageResult.getTotal(), pageNum, pageSize, pageResult.getRecords());
    }

    /**
     * 搜索题目
     * @param keyword 搜索关键词
     * @param type 题型筛选
     * @param categoryId 分类ID筛选
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @Override
    public PageResult<Question> searchQuestions(String keyword, String type, Long categoryId, Integer pageNum, Integer pageSize) {
        // 搜索功能实际上和getQuestionList类似，可以复用逻辑
        return getQuestionList(pageNum, pageSize, type, categoryId, keyword, null);
    }

    /**
     * 批量导入题目
     * @param file Excel文件
     * @return 导入结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importQuestions(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            throw new RuntimeException("文件格式错误，请上传Excel文件（.xlsx或.xls）");
        }

        // TODO: 实现Excel解析和批量导入逻辑
        // 这里可以集成Apache POI或EasyExcel来处理Excel文件
        // 暂时返回提示信息
        throw new RuntimeException("批量导入功能暂未实现，请使用创建题目接口逐个添加");
    }

    /**
     * 导出题目模板
     * @return 导出结果消息
     */
    @Override
    public String exportTemplate() {
        // TODO: 实现Excel模板导出逻辑
        // 这里可以生成一个包含题目字段的Excel模板文件
        // 暂时返回提示信息
        throw new RuntimeException("模板导出功能暂未实现");
    }

    /**
     * 根据分类获取题目列表
     * @param categoryId 分类ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @Override
    public PageResult<Question> getQuestionsByCategory(Long categoryId, Integer pageNum, Integer pageSize) {
        // 验证分类是否存在
        QuestionCategory category = questionCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new RuntimeException("题目分类不存在");
        }

        return getQuestionList(pageNum, pageSize, null, categoryId, null, null);
    }

    /**
     * 根据题型获取题目列表
     * @param type 题型
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @Override
    public PageResult<Question> getQuestionsByType(String type, Integer pageNum, Integer pageSize) {
        if (!StringUtils.hasText(type)) {
            throw new RuntimeException("题型不能为空");
        }

        // 验证题型是否有效
        try {
            QuestionType.of(type);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("题型格式错误：" + e.getMessage());
        }

        return getQuestionList(pageNum, pageSize, type, null, null, null);
    }
}

