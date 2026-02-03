package org.development.exam_online.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.development.exam_online.dao.entity.Question;
import org.development.exam_online.dao.entity.QuestionCategory;
import org.development.exam_online.dao.mapper.QuestionCategoryMapper;
import org.development.exam_online.dao.mapper.QuestionMapper;
import org.development.exam_online.service.QuestionCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QuestionCategoryServiceImpl implements QuestionCategoryService {

    private final QuestionCategoryMapper questionCategoryMapper;
    private final QuestionMapper questionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionCategory createCategory(QuestionCategory category) {
        if (category == null || !StringUtils.hasText(category.getName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类名称不能为空");
        }
        // 名称唯一性检查
        LambdaQueryWrapper<QuestionCategory> q = new LambdaQueryWrapper<>();
        q.eq(QuestionCategory::getDeleted, 0)
                .eq(QuestionCategory::getName, category.getName());
        Long count = questionCategoryMapper.selectCount(q);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "分类名称已存在");
        }

        category.setId(null);
        category.setDeleted(0);
        int inserted = questionCategoryMapper.insert(category);
        if (inserted <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "创建分类失败");
        }
        return questionCategoryMapper.selectById(category.getId());
    }

    @Override
    public List<QuestionCategory> getCategoryList() {
        LambdaQueryWrapper<QuestionCategory> q = new LambdaQueryWrapper<>();
        q.eq(QuestionCategory::getDeleted, 0)
                .orderByAsc(QuestionCategory::getId);
        return questionCategoryMapper.selectList(q);
    }

    @Override
    public QuestionCategory getCategoryById(Long categoryId) {
        QuestionCategory category = requireActiveCategory(categoryId);
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateCategory(Long categoryId, QuestionCategory category) {
        QuestionCategory existing = requireActiveCategory(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类信息不能为空");
        }
        if (StringUtils.hasText(category.getName()) && !category.getName().equals(existing.getName())) {
            // 名称唯一性检查
            LambdaQueryWrapper<QuestionCategory> q = new LambdaQueryWrapper<>();
            q.eq(QuestionCategory::getDeleted, 0)
                    .eq(QuestionCategory::getName, category.getName());
            Long count = questionCategoryMapper.selectCount(q);
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.CONFLICT.getCode(), "分类名称已存在");
            }
        }

        category.setId(categoryId);
        int updated = questionCategoryMapper.updateById(category);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "更新分类失败");
        }
        return "更新成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteCategory(Long categoryId) {
        QuestionCategory existing = requireActiveCategory(categoryId);

        Long count = getQuestionCountByCategory(existing.getId());
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.QUESTION_CATEGORY_HAS_QUESTIONS);
        }

        QuestionCategory update = new QuestionCategory();
        update.setId(existing.getId());
        update.setDeleted(1);
        int updated = questionCategoryMapper.updateById(update);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, "删除分类失败");
        }
        return "删除成功";
    }

    @Override
    public Long getQuestionCountByCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类ID不能为空");
        }
        LambdaQueryWrapper<Question> q = new LambdaQueryWrapper<>();
        q.eq(Question::getDeleted, 0)
                .eq(Question::getCategoryId, categoryId);
        return questionMapper.selectCount(q);
    }

    private QuestionCategory requireActiveCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类ID不能为空");
        }
        QuestionCategory category = questionCategoryMapper.selectById(categoryId);
        if (category == null || !Objects.equals(category.getDeleted(), 0)) {
            throw new BusinessException(ErrorCode.QUESTION_CATEGORY_NOT_FOUND);
        }
        return category;
    }
}

