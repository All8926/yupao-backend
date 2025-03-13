package com.example.exception;

import com.example.common.BaseResponse;
import com.example.common.ErrorCode;
import com.example.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptonHandler {

    /**
     * 这里是捕获 BusinessException 类的异常
     * @param e
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse businessExceptionHandler(BusinessException e){
        log.error("businessException: " + e.getMessage(), e);
        return ResultUtils.error(e.getCode(),e.getMessage(),e.getDescription());
    }

    /**
     * 这里是捕获系统全局的的异常
     * @param e
     * @return
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse runtimeExceptionHandler(RuntimeException e){
        log.error("runtimeException: ", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,ErrorCode.SYSTEM_ERROR.getMessage(),"");
    }
}
