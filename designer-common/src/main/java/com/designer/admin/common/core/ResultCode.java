package com.designer.admin.common.core;

/**
 * 统一业务状态码
 *
 * @author 雨夜终端
 */
public interface ResultCode {

    int SUCCESS = 200;
    int BAD_REQUEST = 400;
    int UNAUTHORIZED = 401;
    int FORBIDDEN = 403;
    int NOT_FOUND = 404;
    /** 请求过于频繁：与 nginx 层 limit_req 的 429 保持同一语义 */
    int TOO_MANY_REQUESTS = 429;
    int ERROR = 500;
}
