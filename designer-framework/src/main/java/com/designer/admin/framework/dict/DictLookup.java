package com.designer.admin.framework.dict;

import java.util.Map;

/**
 * 数据字典读取 SPI：由 system 模块实现，业务模块按需注入。
 *
 * <p><b>为什么放在 framework 而不是直接依赖 system</b>：字典的读写实现、缓存、CRUD
 * 都在 system 模块；blog 等业务模块若直接依赖 system，就形成了「业务 → system」的反向耦合。
 * 这里只暴露一个读接口，实现由 system 提供，运行期由 Solon 按类型注入。</p>
 *
 * <p><b>只读</b>：字典的增删改一律走 system 的管理端接口，业务模块没有写入口，
 * 也就不会出现「某个业务悄悄改字典」这种找不到源头的数据问题。</p>
 *
 * @author 雨夜终端
 */
public interface DictLookup {

    /**
     * 按类型取「键值 → 标签」映射。
     *
     * <p>只包含启用项，按 {@code sort} 升序（{@link java.util.LinkedHashMap}，
     * 直接交给前端渲染下拉时顺序即为字典里配置的顺序）；键值重复时以第一条为准。</p>
     *
     * <p><b>调用方式</b>：实现类方法上带 {@code @Cache}，缓存拦截依赖 Bean 代理，
     * 所以请**从外部调用**取到映射后再在循环里查（一页数据只读一次缓存）；
     * 不要在实现类内部自调用，那样会绕过缓存直接打库。</p>
     *
     * @param dictType 字典类型标识，如 {@code blog_visit_device}
     * @return 键值 → 标签；该类型无数据时返回空 Map（不返回 {@code null}）
     */
    Map<String, String> labelMap(String dictType);
}
