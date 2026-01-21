package org.development.exam_online.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 * 用于封装业务逻辑错误，包含错误码和错误消息
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造函数（使用默认错误码400）
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    /**
     * 构造函数（使用ErrorCode枚举）
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 构造函数（使用ErrorCode枚举和自定义消息）
     * @param errorCode 错误码枚举
     * @param customMessage 自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
    }
}
