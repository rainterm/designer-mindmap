package com.designer.admin.common.core;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果
 *
 * @author 雨夜终端
 */
@Data
public class Result<T> implements Serializable {

    /** 业务状态码 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    public Result() {
    }

    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.SUCCESS, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS, "操作成功", data);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}
