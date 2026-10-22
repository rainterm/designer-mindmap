package com.designer.admin.system.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.crypto.PasswordUtil;
import com.designer.admin.system.constant.Oauth2Consts;
import com.designer.admin.system.mapper.auth.SysOauthClientMapper;
import com.designer.admin.system.mapper.auth.SysOauthClientScopeMapper;
import com.designer.admin.system.mapper.auth.SysOauthScopeMapper;
import com.designer.admin.system.model.dto.auth.OauthClientQueryDTO;
import com.designer.admin.system.model.dto.auth.OauthClientSaveDTO;
import com.designer.admin.system.model.entity.auth.SysOauthClient;
import com.designer.admin.system.model.entity.auth.SysOauthClientScope;
import com.designer.admin.system.model.entity.auth.SysOauthScope;
import com.designer.admin.system.model.vo.auth.OauthClientCreatedVO;
import com.designer.admin.system.service.auth.SysOauthClientService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * OAuth2 客户端服务实现。
 *
 * @author 雨夜终端
 */
@Component
public class SysOauthClientServiceImpl implements SysOauthClientService {

    /** 密钥随机源 */
    private static final SecureRandom RANDOM = new SecureRandom();

    @Db
    private SysOauthClientMapper clientMapper;

    @Db
    private SysOauthClientScopeMapper clientScopeMapper;

    @Db
    private SysOauthScopeMapper scopeMapper;

    @Override
    public IPage<SysOauthClient> listPage(OauthClientQueryDTO query) {
        return clientMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysOauthClient>()
                .like(notBlank(query.getClientId()), SysOauthClient::getClientId, query.getClientId())
                .like(notBlank(query.getClientName()), SysOauthClient::getClientName, query.getClientName())
                .eq(query.getStatus() != null, SysOauthClient::getStatus, query.getStatus())
                .orderByDesc(SysOauthClient::getId));
    }

    @Override
    public SysOauthClient getById(Long id) {
        SysOauthClient client = clientMapper.selectById(id);
        if (client == null) {
            throw new BizException("OAuth2 客户端不存在");
        }
        fillScopes(client);
        return client;
    }

    @Override
    public SysOauthClient getEnabledByClientId(String clientId) {
        if (!notBlank(clientId)) {
            return null;
        }
        return clientMapper.selectOne(new LambdaQueryWrapper<SysOauthClient>()
                .eq(SysOauthClient::getClientId, clientId.trim())
                .eq(SysOauthClient::getStatus, SystemConsts.STATUS_ENABLE)
                .last("LIMIT 1"));
    }

    @Override
    public List<String> listAuthorizedScopeCodes(Long clientPk) {
        return clientScopeMapper.selectScopeCodesByClientPk(clientPk);
    }

    @Override
    @Transaction
    public OauthClientCreatedVO add(OauthClientSaveDTO dto) {
        String clientId = normalizeClientId(dto.getClientId());
        assertClientIdUnique(clientId, null);
        String plainSecret = generateSecret();
        SysOauthClient entity = toEntity(dto, clientId);
        entity.setClientSecret(PasswordUtil.encode(plainSecret));
        clientMapper.insert(entity);
        replaceScopes(entity.getId(), dto.getScopeIds());
        return toCreatedVo(entity, plainSecret);
    }

    @Override
    @Transaction
    public void update(OauthClientSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("客户端ID不能为空");
        }
        SysOauthClient existing = clientMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BizException("OAuth2 客户端不存在");
        }
        String clientId = normalizeClientId(dto.getClientId());
        assertClientIdUnique(clientId, dto.getId());
        SysOauthClient entity = toEntity(dto, clientId);
        entity.setClientSecret(existing.getClientSecret());
        clientMapper.updateById(entity);
        replaceScopes(entity.getId(), dto.getScopeIds());
    }

    @Override
    @Transaction
    public OauthClientCreatedVO resetSecret(Long id) {
        SysOauthClient existing = clientMapper.selectById(id);
        if (existing == null) {
            throw new BizException("OAuth2 客户端不存在");
        }
        String plainSecret = generateSecret();
        SysOauthClient patch = new SysOauthClient();
        patch.setId(id);
        patch.setClientSecret(PasswordUtil.encode(plainSecret));
        clientMapper.updateById(patch);
        existing.setClientSecret(null);
        return toCreatedVo(existing, plainSecret);
    }

    @Override
    @Transaction
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            clientScopeMapper.delete(new LambdaQueryWrapper<SysOauthClientScope>()
                    .eq(SysOauthClientScope::getClientId, id));
        }
        clientMapper.deleteByIds(ids);
    }

    private void fillScopes(SysOauthClient client) {
        List<Long> scopeIds = clientScopeMapper.selectScopeIdsByClientPk(client.getId());
        client.setScopeIds(scopeIds);
        client.setScopeCodes(clientScopeMapper.selectScopeCodesByClientPk(client.getId()));
    }

    private void replaceScopes(Long clientPk, List<Long> scopeIds) {
        clientScopeMapper.delete(new LambdaQueryWrapper<SysOauthClientScope>()
                .eq(SysOauthClientScope::getClientId, clientPk));
        if (scopeIds == null || scopeIds.isEmpty()) {
            return;
        }
        for (Long scopeId : scopeIds) {
            if (scopeId == null) {
                continue;
            }
            SysOauthScope scope = scopeMapper.selectById(scopeId);
            if (scope == null || scope.getStatus() == null || scope.getStatus() != SystemConsts.STATUS_ENABLE) {
                throw new BizException("无效或已停用的 Scope：" + scopeId);
            }
            SysOauthClientScope rel = new SysOauthClientScope();
            rel.setClientId(clientPk);
            rel.setScopeId(scopeId);
            clientScopeMapper.insert(rel);
        }
    }

    private SysOauthClient toEntity(OauthClientSaveDTO dto, String clientId) {
        SysOauthClient entity = new SysOauthClient();
        entity.setId(dto.getId());
        entity.setClientId(clientId);
        entity.setClientName(dto.getClientName() == null ? "" : dto.getClientName().trim());
        int ttl = dto.getAccessTokenTtl() == null ? Oauth2Consts.DEFAULT_ACCESS_TOKEN_TTL_SECONDS : dto.getAccessTokenTtl();
        if (ttl < Oauth2Consts.MIN_ACCESS_TOKEN_TTL_SECONDS) {
            throw new BizException("access_token 有效期不能小于 " + Oauth2Consts.MIN_ACCESS_TOKEN_TTL_SECONDS + " 秒");
        }
        entity.setAccessTokenTtl(ttl);
        entity.setStatus(dto.getStatus() == null ? SystemConsts.STATUS_ENABLE : dto.getStatus());
        entity.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        return entity;
    }

    private void assertClientIdUnique(String clientId, Long excludeId) {
        Long count = clientMapper.countByClientId(clientId, excludeId);
        if (count != null && count > 0) {
            throw new BizException("client_id 已存在：" + clientId);
        }
    }

    private static String normalizeClientId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException("client_id 不能为空");
        }
        String id = raw.trim().toLowerCase(Locale.ROOT);
        if (!id.matches(Oauth2Consts.CLIENT_ID_PATTERN)) {
            throw new BizException("client_id 需小写字母开头，3-64 位字母数字_-");
        }
        return id;
    }

    private static String generateSecret() {
        byte[] buf = new byte[Oauth2Consts.CLIENT_SECRET_BYTE_LENGTH];
        RANDOM.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    private static OauthClientCreatedVO toCreatedVo(SysOauthClient entity, String plainSecret) {
        OauthClientCreatedVO vo = new OauthClientCreatedVO();
        vo.setId(entity.getId());
        vo.setClientId(entity.getClientId());
        vo.setClientName(entity.getClientName());
        vo.setClientSecret(plainSecret);
        return vo;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
