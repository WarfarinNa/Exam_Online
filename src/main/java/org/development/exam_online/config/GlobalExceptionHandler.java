package org.development.exam_online.config;

import lombok.extern.slf4j.Slf4j;
import org.development.exam_online.common.Result;
import org.development.exam_online.common.exception.BusinessException;
import org.development.exam_online.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.failure(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", errorMessage);
        return Result.failure(ErrorCode.BAD_REQUEST.getCode(), "参数校验失败: " + errorMessage);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", errorMessage);
        return Result.failure(ErrorCode.BAD_REQUEST.getCode(), "参数绑定失败: " + errorMessage);
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        String message = e.getMessage();

        if (message != null && message.contains("用户名或密码错误")) {
            return Result.failure(ErrorCode.PASSWORD_ERROR.getCode(), ErrorCode.PASSWORD_ERROR.getMessage());
        }

        if (message != null && message.contains("不存在")) {
            return Result.failure(ErrorCode.NOT_FOUND.getCode(), message);
        }

        if (message != null && (message.contains("已存在") || message.contains("已被"))) {
            return Result.failure(ErrorCode.CONFLICT.getCode(), message);
        }

        return Result.failure(ErrorCode.BAD_REQUEST.getCode(), message != null ? message : "操作失败");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.failure(ErrorCode.INTERNAL_ERROR.getCode(), "系统内部错误，请稍后重试");
    }
}
