package com.designer.admin.system.constant;

/**
 * 声明式缓存（Solon {@code @Cache}）的键、tag 与兜底存活时间。
 *
 * <p><b>什么样的接口才放进来</b>：读多写少、返回值与调用者无关（公共字典 / 下拉 / 白名单），
 * 且写入口能枚举完整 —— 漏掉一个写入口就是一份脏数据，直到 TTL 到期。
 * 因此这里刻意不收录「带分页 / 关键字」的查询：它们的键组合无穷，缓存命中率低、失效粒度还粗。</p>
 *
 * <p><b>键名是契约</b>：上线的键不要改名，旧键不会被读取，只能等 TTL 自然消失。
 * 键前缀统一 {@code system:}；实际 Redis 键还会再套一层框架的 {@code cache:} 前缀
 * （见 framework 的 {@code RedisKeyConsts.CACHE_PREFIX}），此处不要重复写。</p>
 *
 * <p><b>tag 与 key 的关系</b>：tag 是「一组键的索引」，用于按业务维度整体失效；
 * key 是单条缓存。二者都必须在这里定义，避免 key 字面量散落在 Controller / Service 里对不上。</p>
 *
 * @author 雨夜终端
 */
public final class SystemCacheConsts {

    /** 公共只读接口的兜底存活时间（秒）：1 小时。写侧会主动失效，TTL 只兜底 */
    public static final int CACHE_SECONDS = 3600;

    // ------------------------------ 系统设置 ------------------------------

    /** 公开系统设置：{@code GET /admin-api/system/setting/public} */
    public static final String SETTING_PUBLIC = "system:public:setting";

    // ------------------------------ 数据字典 ------------------------------

    /**
     * 字典类型下拉：{@code GET /admin-api/system/dict/type/options}。
     *
     * <p>无参数，一份缓存全体共用。</p>
     */
    public static final String DICT_TYPE_OPTIONS = "system:dict:type:options";

    /**
     * 按类型查询字典数据：{@code GET /admin-api/system/dict/data/{dictType}}。
     *
     * <p>{@code #{dictType}} 由 Solon 从方法参数求值（项目已开 {@code -parameters}），
     * 所以方法参数名必须一直叫 {@code dictType}，改名会让键求值直接抛异常。</p>
     */
    public static final String DICT_DATA_KEY_TMPL = "system:dict:data:#{dictType}";

    /**
     * 字典「键值 → 标签」映射：{@code DictLookup#labelMap}。
     *
     * <p>与 {@link #DICT_DATA_KEY_TMPL} 缓存的是同一批行的不同形态（一个是原始行，
     * 一个是渲染直接可用的映射），故共用 {@link #DICT_DATA_TAG} 失效 —— 字典数据一改，
     * 管理端下拉与后端文案同时生效。</p>
     */
    public static final String DICT_LABEL_MAP_KEY_TMPL = "system:dict:label:#{dictType}";

    /** 字典类型缓存 tag：类型增删改时整体失效 */
    public static final String DICT_TYPE_TAG = "system:dict:type";

    /**
     * 字典数据缓存 tag：数据增删改时整体失效。
     *
     * <p><b>为什么整体失效而不是按 dictType 精准删</b>：缓存键的粒度是「每个字典类型一个」，
     * 键总数 = 字典类型数（通常几十个），全清的成本远低于「精准删但要处理改名、批量删」的复杂度。
     * 而且 {@code removeData(List<Long> ids)} 只有主键、拿不到 dictType，精准删根本做不到。</p>
     */
    public static final String DICT_DATA_TAG = "system:dict:data";

    /** 字典全部缓存 tag（逗号分隔，符合 {@code @CacheRemove#tags} 的格式）：类型标识改动会连带影响数据归属 */
    public static final String DICT_ALL_TAGS = DICT_TYPE_TAG + "," + DICT_DATA_TAG;

    // ------------------------------ OAuth2 授权范围 ------------------------------

    /**
     * 启用的授权范围列表：{@code GET /admin-api/system/oauth-scope/options}。
     *
     * <p>也是开放接口的鉴权依据 —— {@code OpenApiScopeGuard} 每个请求都要用它做路径匹配，
     * 是本项目里读频率最高的配置类查询之一。</p>
     */
    public static final String OAUTH_SCOPE_ENABLED = "system:oauth:scope:enabled";

    /** 授权范围缓存 tag */
    public static final String OAUTH_SCOPE_TAG = "system:oauth:scope";

    private SystemCacheConsts() {
    }
}
