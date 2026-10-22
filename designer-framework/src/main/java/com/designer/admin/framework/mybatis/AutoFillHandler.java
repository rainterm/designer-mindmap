package com.designer.admin.framework.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.designer.admin.framework.satoken.OperatorHelper;
import org.apache.ibatis.reflection.MetaObject;
import org.noear.solon.annotation.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充：createBy / createTime / updateBy / updateTime
 *
 * <p>仅对实体中真实存在（且标注了 FieldFill）的字段生效。
 *
 * @author 雨夜终端
 */
@Component
public class AutoFillHandler implements MetaObjectHandler {

    private static final String CREATE_BY = "createBy";
    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_BY = "updateBy";
    private static final String UPDATE_TIME = "updateTime";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String operator = OperatorHelper.currentOperator();
        this.strictInsertFill(metaObject, CREATE_BY, String.class, operator);
        this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, now);
        this.strictInsertFill(metaObject, UPDATE_BY, String.class, operator);
        this.strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, UPDATE_BY, String.class, OperatorHelper.currentOperator());
        this.strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
    }
}
