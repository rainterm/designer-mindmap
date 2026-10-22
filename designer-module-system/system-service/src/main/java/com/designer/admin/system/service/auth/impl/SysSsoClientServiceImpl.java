package com.designer.admin.system.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.constant.HttpConsts;
import com.designer.admin.system.mapper.auth.SysSsoClientMapper;
import com.designer.admin.system.model.dto.auth.SsoClientQueryDTO;
import com.designer.admin.system.model.dto.auth.SsoClientSaveDTO;
import com.designer.admin.system.model.entity.auth.SysSsoClient;
import com.designer.admin.system.service.auth.SysSsoClientService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * SSO 客户端：白名单维护与回跳校验
 *
 * @author 雨夜终端
 */
@Component
public class SysSsoClientServiceImpl implements SysSsoClientService {

    @Db
    private SysSsoClientMapper ssoClientMapper;

    @Override
    public IPage<SysSsoClient> listPage(SsoClientQueryDTO query) {
        return ssoClientMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysSsoClient>()
                .like(notBlank(query.getClientName()), SysSsoClient::getClientName, query.getClientName())
                .like(notBlank(query.getClientKey()), SysSsoClient::getClientKey, query.getClientKey())
                .eq(query.getStatus() != null, SysSsoClient::getStatus, query.getStatus())
                .orderByAsc(SysSsoClient::getSort)
                .orderByAsc(SysSsoClient::getId));
    }

    @Override
    public List<SysSsoClient> listEnabled() {
        return ssoClientMapper.selectList(new LambdaQueryWrapper<SysSsoClient>()
                .eq(SysSsoClient::getStatus, 1)
                .orderByAsc(SysSsoClient::getSort)
                .orderByAsc(SysSsoClient::getId));
    }

    @Override
    public SysSsoClient getById(Long id) {
        return ssoClientMapper.selectById(id);
    }

    @Override
    public SysSsoClient getEnabledByKey(String clientKey) {
        if (!notBlank(clientKey)) {
            throw new BizException("客户端标识不能为空");
        }
        SysSsoClient client = ssoClientMapper.selectOne(new LambdaQueryWrapper<SysSsoClient>()
                .eq(SysSsoClient::getClientKey, clientKey.trim())
                .eq(SysSsoClient::getStatus, 1)
                .last("LIMIT 1"));
        if (client == null) {
            throw new BizException("SSO 客户端不存在或已停用：" + clientKey);
        }
        return client;
    }

    @Override
    public void assertAllowedReturnUrl(String redirect) {
        String origin = extractOrigin(redirect);
        if (origin == null) {
            throw new BizException("非法的 SSO 回跳地址");
        }
        Long count = ssoClientMapper.selectCount(new LambdaQueryWrapper<SysSsoClient>()
                .eq(SysSsoClient::getOrigin, origin)
                .eq(SysSsoClient::getStatus, 1));
        if (count == null || count == 0) {
            throw new BizException("回跳地址未在 SSO 客户端白名单中：" + origin);
        }
    }

    @Override
    @Transaction
    public void add(SsoClientSaveDTO dto) {
        SysSsoClient entity = toEntity(dto);
        assertClientKeyUnique(entity.getClientKey(), null);
        assertOriginUnique(entity.getOrigin(), null);
        ssoClientMapper.insert(entity);
    }

    @Override
    @Transaction
    public void update(SsoClientSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("客户端ID不能为空");
        }
        SysSsoClient existing = ssoClientMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BizException("SSO 客户端不存在");
        }
        SysSsoClient entity = toEntity(dto);
        assertClientKeyUnique(entity.getClientKey(), dto.getId());
        assertOriginUnique(entity.getOrigin(), dto.getId());
        ssoClientMapper.updateById(entity);
    }

    @Override
    @Transaction
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ssoClientMapper.deleteByIds(ids);
    }

    private SysSsoClient toEntity(SsoClientSaveDTO dto) {
        String origin = normalizeOrigin(dto.getOrigin());
        String openUrl = normalizeOpenUrl(dto.getOpenUrl(), origin);
        String entryPath = normalizeEntryPath(dto.getEntryPath());
        String clientKey = dto.getClientKey() == null ? "" : dto.getClientKey().trim().toLowerCase(Locale.ROOT);

        SysSsoClient entity = new SysSsoClient();
        entity.setId(dto.getId());
        entity.setClientName(dto.getClientName() == null ? "" : dto.getClientName().trim());
        entity.setClientKey(clientKey);
        entity.setOrigin(origin);
        entity.setOpenUrl(openUrl);
        entity.setEntryPath(entryPath);
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        return entity;
    }

    private void assertClientKeyUnique(String clientKey, Long excludeId) {
        if (!notBlank(clientKey)) {
            throw new BizException("客户端标识不能为空");
        }
        Long count = ssoClientMapper.countByClientKey(clientKey, excludeId);
        if (count != null && count > 0) {
            throw new BizException("客户端标识已存在：" + clientKey);
        }
    }

    private void assertOriginUnique(String origin, Long excludeId) {
        Long count = ssoClientMapper.countByOrigin(origin, excludeId);
        if (count != null && count > 0) {
            throw new BizException("Origin 已存在：" + origin);
        }
    }

    static String extractOrigin(String redirect) {
        if (!notBlank(redirect)) {
            return null;
        }
        try {
            URI uri = URI.create(redirect.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            if (!HttpConsts.SCHEME_HTTP.equals(scheme) && !HttpConsts.SCHEME_HTTPS.equals(scheme)) {
                return null;
            }
            int port = uri.getPort();
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (port > 0 && port != defaultPort(scheme)) {
                return scheme + "://" + host + ":" + port;
            }
            return scheme + "://" + host;
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeOrigin(String raw) {
        String origin = extractOrigin(raw);
        if (origin == null) {
            // 允许只填 origin 文本
            try {
                URI uri = URI.create(raw == null ? "" : raw.trim());
                origin = extractOrigin(uri.toString());
            } catch (Exception ignored) {
                // fall through
            }
        }
        if (origin == null) {
            throw new BizException("Origin 格式不正确，示例：http://localhost:5176");
        }
        return origin;
    }

    private static String normalizeOpenUrl(String raw, String origin) {
        if (!notBlank(raw)) {
            return origin;
        }
        String trimmed = raw.trim().replaceAll("/+$", "");
        try {
            URI uri = URI.create(trimmed);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new BizException("打开地址格式不正确");
            }
            return trimmed;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("打开地址格式不正确");
        }
    }

    private static String normalizeEntryPath(String raw) {
        if (!notBlank(raw)) {
            return "/sso";
        }
        String p = raw.trim();
        if (!p.startsWith(HttpConsts.PATH_SEPARATOR)) {
            p = HttpConsts.PATH_SEPARATOR + p;
        }
        if (p.length() > 1 && p.endsWith(HttpConsts.PATH_SEPARATOR)) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private static int defaultPort(String scheme) {
        return "https".equals(scheme) ? 443 : 80;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
