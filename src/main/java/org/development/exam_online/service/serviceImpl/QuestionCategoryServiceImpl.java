package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.dao.entity.Question;
import org.development.exam_online.dao.entity.QuestionCategory;
import org.development.exam_online.dao.mapper.QuestionCategoryMapper;
import org.development.exam_online.dao.mapper.QuestionMapper;
import org.development.exam_online.service.QuestionCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 题目分类服务实现类
 */
@Service
@RequiredArgsConstructor
public class QuestionCategoryServiceImpl implements QuestionCategoryService {

    private final QuestionCategoryMapper questionCategoryMapper;
    private final QuestionMapper questionMapper;

    /**
     * 创建题目分类
     * @param category 分类信息
     * @return 创建的分类
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionCategory createCategory(QuestionCategory category) {
        // 验证分类名称不能为空
        if (!StringUtils.hasText(category.getName())) {
            throw new RuntimeException("分类名称不能为空");
        }

        // 检查分类名称是否已存在
        LambdaQueryWrapper<QuestionCategory> query = new LambdaQueryWrapper<>();
        query.eq(QuestionCategory::getName, category.getName());
        QuestionCategory existingCategory = questionCategoryMapper.selectOne(query);
        if (existingCategory != null) {
            throw new RuntimeException("分类名称已存在");
        }

        int result = questionCategoryMapper.insert(category);
        if (result > 0) {
            return questionCategoryMapper.selectById(category.getId());
        } else {
            throw new RuntimeException("创建分类失败");
        }
    }

    /**
     * 获取所有题目分类列表
     * @return 分类列表
     */
    @Override
    public List<QuestionCategory> getCategoryList() {
        return questionCategoryMapper.selectList(null);
    }

    /**
     * 根据ID获取分类详情
     * @param categoryId 分类ID
     * @return 分类详情
     */
    @Override
    public QuestionCategory getCategoryById(Long categoryId) {
        QuestionCategory category = questionCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        return category;
    }

    /**
     * 更新题目分类
     * @param categoryId 分类ID
     * @param category 分类信息
     * @return 更新结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateCategory(Long categoryId, QuestionCategory category) {
        QuestionCategory existingCategory = questionCategoryMapper.selectById(categoryId);
        if (existingCategory == null) {
            throw new RuntimeException("分类不存在");
        }

        // 如果修改了分类名称，检查新名称是否已存在
        if (StringUtils.hasText(category.getName()) && !category.getName().equals(existingCategory.getName())) {
            LambdaQueryWrapper<QuestionCategory> query = new LambdaQueryWrapper<>();
            query.eq(QuestionCategory::getName, category.getName())
                    .ne(QuestionCategory::getId, categoryId);
            QuestionCategory duplicateCategory = questionCategoryMapper.selectOne(query);
            if (duplicateCategory != null) {
                throw new RuntimeException("分类名称已存在");
            }
        }

        // 设置ID
        category.setId(categoryId);

        int result = questionCategoryMapper.updateById(category);
        if (result > 0) {
            return "分类更新成功";
        } else {
            throw new RuntimeException("分类更新失败");
        }
    }

    /**
     * 删除题目分类
     * @param categoryId 分类ID
     * @return 删除结果消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteCategory(Long categoryId) {
        QuestionCategory category = questionCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }

        // 检查该分类下是否有题目
        LambdaQueryWrapper<Question> questionQuery = new LambdaQueryWrapper<>();
        questionQuery.eq(Question::getCategoryId, categoryId);
        Long questionCount = questionMapper.selectCount(questionQuery);
        
        if (questionCount > 0) {
            throw new RuntimeException("该分类下存在 " + questionCount + " 道题目，无法删除。请先删除或移动这些题目");
        }

        int result = questionCategoryMapper.deleteById(categoryId);
        if (result > 0) {
            return "分类删除成功";
        } else {
            throw new RuntimeException("分类删除失败");
        }
    }

    /**
     * 获取分类下的题目数量
     * @param categoryId 分类ID
     * @return 题目数量
     */
    @Override
    public Long getQuestionCountByCategory(Long categoryId) {
        // 验证分类是否存在
        QuestionCategory category = questionCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }

        // 统计该分类下的题目数量
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<>();
        query.eq(Question::getCategoryId, categoryId);
        return questionMapper.selectCount(query);
    }
}

