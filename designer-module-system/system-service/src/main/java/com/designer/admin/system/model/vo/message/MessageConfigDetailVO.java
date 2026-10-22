package com.designer.admin.system.model.vo.message;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息通道配置详情视图（编辑页回显）。
 *
 * <p>{@code config} 的键由通道类型（Provider）在运行时声明，前端据此渲染动态表单，
 * 因此保留为 Map；其余固定列改为显式属性，避免字符串键名写错后静默返回空字段。</p>
 *
 * @author 雨夜终端
 */
@Data
@Builder
public class MessageConfigDetailVO {

    /** 配置 ID */
    private Long id;

    /** 配置名称 */
    private String configName;

    /** 通道类型：inbox / email / ... */
    private String channelType;

    /** 是否默认：1是 0否 */
    private Integer isDefault;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 通道专属配置项（密钥字段已脱敏），键集合由通道类型决定 */
    private Map<String, Object> config;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
