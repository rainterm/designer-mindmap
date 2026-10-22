package com.designer.admin.system.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.constant.HttpConsts;
import com.designer.admin.system.constant.SystemCacheConsts;
import com.designer.admin.system.mapper.auth.SysOauthClientScopeMapper;
import com.designer.admin.system.mapper.auth.SysOauthScopeMapper;
import com.designer.admin.system.model.dto.auth.OauthScopeQueryDTO;
import com.designer.admin.system.model.dto.auth.OauthScopeSaveDTO;
import com.designer.admin.system.model.entity.auth.SysOauthClientScope;
import com.designer.admin.system.model.entity.auth.SysOauthScope;
import com.designer.admin.system.service.auth.SysOauthScopeService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Cache;
import org.noear.solon.data.annotation.CacheRemove;
import org.noear.solon.data.annotation.Transaction;

import java.util.List;
import java.util.Locale;

/**
 * OAuth2 授权范围服务实现。
 *
 * @author 雨夜终端
 */
@Component
public class SysOauthScopeServiceImpl implements SysOauthScopeService {

    @Db
    private SysOauthScopeMapper scopeMapper;

    @Db
    private SysOauthClientScopeMapper clientScopeMapper;

    @Override
    public IPage<SysOauthScope> listPage(OauthScopeQueryDTO query) {
        return scopeMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysOauthScope>()
                .like(notBlank(query.getScopeCode()), SysOauthScope::getScopeCode, query.getScopeCode())
                .like(notBlank(query.getScopeName()), SysOauthScope::getScopeName, query.getScopeName())
                .eq(query.getStatus() != null, SysOauthScope::getStatus, query.getStatus())
                .orderByAsc(SysOauthScope::getId));
    }

    /**
     * 启用的授权范围列表。
     *
     * <p>两个读入口（管理端下拉 {@code /oauth-scope/options} 与开放接口鉴权
     * {@code OpenApiScopeGuard}）共用同一个缓存键：返回值完全一致，
     * 没必要各存一份、更没必要各失效一次。</p>
     */
    @Override
    @Cache(key = SystemCacheConsts.OAUTH_SCOPE_ENABLED,
            tags = SystemCacheConsts.OAUTH_SCOPE_TAG,
            seconds = SystemCacheConsts.CACHE_SECONDS)
    public List<SysOauthScope> listEnabled() {
        return scopeMapper.selectList(new LambdaQueryWrapper<SysOauthScope>()
                .eq(SysOauthScope::getStatus, SystemConsts.STATUS_ENABLE)
                .orderByAsc(SysOauthScope::getSort)
                .orderByAsc(SysOauthScope::getId));
    }

    /**
     * 开放接口鉴权用的 scope 列表。
     *
     * <p><b>注解必须同时标在这里</b>：{@code this.listEnabled()} 是自调用，不经过 Solon 的
     * Bean 代理，{@link Cache} 拦截不到 —— 只给 {@code listEnabled} 加注解的话，
     * 每个开放接口请求仍会打一次数据库。两处共用同一个 key，命中同一份数据。</p>
     */
    @Override
    @Cache(key = SystemCacheConsts.OAUTH_SCOPE_ENABLED,
            tags = SystemCacheConsts.OAUTH_SCOPE_TAG,
            seconds = SystemCacheConsts.CACHE_SECONDS)
    public List<SysOauthScope> listAllEnabledPatterns() {
        return listEnabled();
    }

    @Override
    public SysOauthScope getById(Long id) {
        SysOauthScope scope = scopeMapper.selectById(id);
        if (scope == null) {
            throw new BizException("Scope 不存在");
        }
        return scope;
    }

    @Override
    @Transaction
    @CacheRemove(tags = SystemCacheConsts.OAUTH_SCOPE_TAG)
    public SysOauthScope add(OauthScopeSaveDTO dto) {
        SysOauthScope entity = toEntity(dto);
        assertCodeUnique(entity.getScopeCode(), null);
        scopeMapper.insert(entity);
        return entity;
    }

    @Override
    @Transaction
    @CacheRemove(tags = SystemCacheConsts.OAUTH_SCOPE_TAG)
    public void update(OauthScopeSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("Scope ID不能为空");
        }
        if (scopeMapper.selectById(dto.getId()) == null) {
            throw new BizException("Scope 不存在");
        }
        SysOauthScope entity = toEntity(dto);
        assertCodeUnique(entity.getScopeCode(), dto.getId());
        scopeMapper.updateById(entity);
    }

    @Override
    @Transaction
    @CacheRemove(tags = SystemCacheConsts.OAUTH_SCOPE_TAG)
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            Long bound = clientScopeMapper.selectCount(new LambdaQueryWrapper<SysOauthClientScope>()
                    .eq(SysOauthClientScope::getScopeId, id));
            if (bound != null && bound > 0) {
                SysOauthScope scope = scopeMapper.selectById(id);
                String name = scope == null ? String.valueOf(id) : scope.getScopeCode();
                throw new BizException("Scope【" + name + "】已授权给客户端，无法删除");
            }
        }
        scopeMapper.deleteByIds(ids);
    }

    private SysOauthScope toEntity(OauthScopeSaveDTO dto) {
        SysOauthScope entity = new SysOauthScope();
        entity.setId(dto.getId());
        entity.setScopeCode(normalizeCode(dto.getScopeCode()));
        entity.setScopeName(dto.getScopeName() == null ? "" : dto.getScopeName().trim());
        entity.setPathPattern(normalizePath(dto.getPathPattern()));
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        entity.setStatus(dto.getStatus() == null ? SystemConsts.STATUS_ENABLE : dto.getStatus());
        entity.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        return entity;
    }

    private void assertCodeUnique(String code, Long excludeId) {
        Long count = scopeMapper.countByScopeCode(code, excludeId);
        if (count != null && count > 0) {
            throw new BizException("Scope 标识已存在：" + code);
        }
    }

    private static String normalizeCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException("Scope 标识不能为空");
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePath(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException("路径模式不能为空");
        }
        String p = raw.trim();
        if (!p.startsWith(HttpConsts.PATH_SEPARATOR)) {
            p = HttpConsts.PATH_SEPARATOR + p;
        }
        return p;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
