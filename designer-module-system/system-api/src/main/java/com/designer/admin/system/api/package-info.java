/**
 * 按需对外暴露的 API 契约包。
 * <p>
 * 约定：仅放置其他模块需要调用的 {@code *Api} 接口；不要把内部 {@code *Service} 或业务 {@code model}
 * （entity/dto/vo）放进本模块。{@code *Api} 入参/出参如需类型，在本模块自行定义（勿复用 service 侧 model）。
 * 实现类在对应 {@code *-service} 子模块的同名 {@code api} 包下（如 {@code XxxApiImpl}）。
 */
package com.designer.admin.system.api;
