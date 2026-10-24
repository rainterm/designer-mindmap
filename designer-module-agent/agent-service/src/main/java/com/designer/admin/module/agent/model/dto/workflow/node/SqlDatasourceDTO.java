package com.designer.admin.module.agent.model.dto.workflow.node;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * SQL 节点内联数据源连接信息。
 *
 * @author 雨夜终端
 */
@Data
public class SqlDatasourceDTO {

    @ApiModelProperty("数据源 ID（与 datasourceId 匹配）")
    private String id;

    @ApiModelProperty("类型：mysql / postgresql")
    private String type;

    @ApiModelProperty("完整 JDBC URL；为空时按 host/port/database 拼装")
    private String jdbcUrl;

    @ApiModelProperty("主机")
    private String host;

    @ApiModelProperty("端口")
    private Integer port;

    @ApiModelProperty("库名")
    private String database;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("密码")
    private String password;
}
