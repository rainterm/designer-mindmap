package com.designer.admin.common.exception;

import com.designer.admin.common.core.ResultCode;
import lombok.Getter;

/**
 * 业务异常：由全局异常处理统一捕获并转换为 Result.fail
 *
 * @author 雨夜终端
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.ERROR;
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.ERROR;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
