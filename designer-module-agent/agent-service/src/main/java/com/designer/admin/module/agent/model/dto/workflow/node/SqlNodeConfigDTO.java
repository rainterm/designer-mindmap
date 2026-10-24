package com.designer.admin.module.agent.model.dto.workflow.node;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * SQL 节点配置。
 *
 * @author 雨夜终端
 */
@Data
public class SqlNodeConfigDTO {

    @ApiModelProperty("SQL 模板（可含变量占位）")
    private String sql;

    @ApiModelProperty("选中的数据源 ID；空则用主库")
    private String datasourceId;

    @ApiModelProperty("内联数据源列表")
    private List<SqlDatasourceDTO> datasources;
}
