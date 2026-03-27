package com.tqh.bus.ticket.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
        log.info("全局异常处理器已初始化，当前激活的 profiles: {}",
                Arrays.toString(environment.getActiveProfiles()));
    }

    private boolean isDevelopmentEnvironment() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("dev") || profile.equals("test"));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResultWrapper<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage(), e);
        return ResponseEntity.ok(ResultWrapper.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultWrapper<Void>> handleException(Exception e) {
        log.error("未预期异常: {}", e.getMessage(), e);

        String userMessage;
        if (isDevelopmentEnvironment()) {
            // 开发环境返回详细错误信息，便于调试
            userMessage = "服务器内部错误: " + e.getMessage();
        } else {
            // 生产环境返回通用错误信息，避免泄露敏感信息
            userMessage = "服务器内部错误，请稍后重试";
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultWrapper.fail(500, userMessage));
    }
}
