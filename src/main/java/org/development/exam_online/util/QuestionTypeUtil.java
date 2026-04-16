package org.development.exam_online.util;

import org.development.exam_online.common.enums.QuestionType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public final class QuestionTypeUtil {

    private QuestionTypeUtil() {}

    public static List<Map<String, String>> getTypeList() {
        return Arrays.stream(QuestionType.values())
                .map(type -> Map.of(
                        "code", type.getCode(),
                        "label", type.getLabel()
                ))
                .collect(Collectors.toList());
    }

}
