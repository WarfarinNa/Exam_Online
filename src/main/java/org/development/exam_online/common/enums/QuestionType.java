package org.development.exam_online.common.enums;

import lombok.Getter;

/**
 * 题目类型枚举
 */
@Getter
public enum QuestionType {

    SINGLE("single", "单选题"),
    MULTIPLE("multiple", "多选题"),
    JUDGE("judge", "判断题"),
    BLANK("blank", "填空题"),
    SHORT("short", "简答题");

    private final String code;
    private final String label;

    QuestionType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 根据代码获取枚举
     * @param code 题型代码
     * @return 题目类型枚举
     */
    public static QuestionType of(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (QuestionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("非法题型：" + code);
    }

    /**
     * 验证题型代码是否有效
     * @param code 题型代码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        for (QuestionType type : values()) {
            if (type.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}

