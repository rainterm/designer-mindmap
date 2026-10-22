package com.designer.admin.system.model.vo.auth;

import com.designer.admin.system.constant.Oauth2Consts;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.noear.snack4.annotation.ONodeAttr;

/**
 * OAuth2 错误响应（RFC 6749 §5.2）。
 *
 * @author 雨夜终端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Oauth2ErrorVO {

    /** 错误码：invalid_request / invalid_client / unsupported_grant_type / invalid_scope */
    @ONodeAttr(name = Oauth2Consts.FIELD_ERROR)
    private String error;

    /** 错误描述 */
    @ONodeAttr(name = Oauth2Consts.FIELD_ERROR_DESCRIPTION)
    private String errorDescription;
}
