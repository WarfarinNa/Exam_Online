package org.development.exam_online.util;

import org.development.exam_online.common.enums.QuestionType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题型工具类
 * 提供题型相关的工具方法，确保题型代码的使用一致性
 */
public final class QuestionTypeUtil {

    private QuestionTypeUtil() {}

    /**
     * 获取所有题型的Map（code -> label）
     * @return 题型代码到标签的映射
     */
    public static Map<String, String> getCodeToLabelMap() {
        return Arrays.stream(QuestionType.values())
                .collect(Collectors.toMap(QuestionType::getCode, QuestionType::getLabel));
    }

    /**
     * 获取所有题型的Map（code -> QuestionType）
     * @return 题型代码到枚举的映射
     */
    public static Map<String, QuestionType> getCodeToTypeMap() {
        return Arrays.stream(QuestionType.values())
                .collect(Collectors.toMap(QuestionType::getCode, type -> type));
    }

    /**
     * 获取所有题型的List<Map>，包含code和label
     * @return 题型列表，每个元素包含code和label
     */
    public static List<Map<String, String>> getTypeList() {
        return Arrays.stream(QuestionType.values())
                .map(type -> Map.of(
                        "code", type.getCode(),
                        "label", type.getLabel()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 验证题型代码是否存在
     * @param code 题型代码
     * @return 是否存在
     */
    public static boolean isValidTypeCode(String code) {
        return QuestionType.isValid(code);
    }

    /**
     * 根据代码获取题型标签
     * @param code 题型代码
     * @return 题型标签，如果不存在返回null
     */
    public static String getLabelByCode(String code) {
        try {
            QuestionType type = QuestionType.of(code);
            return type != null ? type.getLabel() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 获取支持的题型代码列表（用于API文档）
     * @return 题型代码数组
     */
    public static String[] getSupportedTypeCodes() {
        return QuestionType.getAllCodes();
    }

    /**
     * 获取支持的题型标签列表（用于API文档）
     * @return 题型标签数组
     */
    public static String[] getSupportedTypeLabels() {
        return QuestionType.getAllLabels();
    }
}
