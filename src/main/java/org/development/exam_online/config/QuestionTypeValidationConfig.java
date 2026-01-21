package org.development.exam_online.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.development.exam_online.common.enums.QuestionType;
import org.springframework.stereotype.Component;

/**
 * 题型验证配置
 * 在应用启动时验证题型枚举的完整性
 */
@Slf4j
@Component
public class QuestionTypeValidationConfig {

    @PostConstruct
    public void validateQuestionTypes() {
        log.info("开始验证题型枚举配置...");

        QuestionType[] types = QuestionType.values();

        log.info("支持的题型数量: {}", types.length);

        for (QuestionType type : types) {
            log.info("题型: {} - {} ({})", type.name(), type.getLabel(), type.getCode());

            // 验证代码不为空
            if (type.getCode() == null || type.getCode().trim().isEmpty()) {
                throw new IllegalStateException("题型枚举 " + type.name() + " 的代码不能为空");
            }

            // 验证标签不为空
            if (type.getLabel() == null || type.getLabel().trim().isEmpty()) {
                throw new IllegalStateException("题型枚举 " + type.name() + " 的标签不能为空");
            }
        }

        // 验证代码的唯一性
        long uniqueCodes = java.util.Arrays.stream(types)
                .map(QuestionType::getCode)
                .distinct()
                .count();

        if (uniqueCodes != types.length) {
            throw new IllegalStateException("题型枚举中的代码必须唯一");
        }

        log.info("题型枚举配置验证通过 ✓");
    }
}