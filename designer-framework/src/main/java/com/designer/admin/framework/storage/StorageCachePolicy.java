package com.designer.admin.framework.storage;

/**
 * 上传文件的缓存策略常量。
 *
 * <p><b>为什么必须集中一处</b>：同一个公开文件可能经由两条完全不同的链路到达浏览器 ——
 * 一是后端的代理下载接口，二是存储配置里配了公网 / CDN 前缀时返回的直链。
 * 前者由我们写响应头，后者由对象存储把**上传时写入的元数据**当响应头返回。
 * 两边的值一旦分叉，就会出现「直连 CDN 能缓存一年、走代理每次都回源」这类症状，
 * 排查时又极难想到是两处字面量不同。</p>
 *
 * <p>取值依据：存储文件名是 UUID、写入后不再原地覆盖（见 {@code FileStorageService} 的
 * 路径生成），因此「URL 不变 ⇒ 内容不变」成立，公开文件可以给最强缓存。</p>
 *
 * @author 雨夜终端
 */
public final class StorageCachePolicy {

    /**
     * 公开文件：强缓存一年且免协商。
     *
     * <p>{@code immutable} 让浏览器在用户触发刷新时也跳过协商请求；
     * CDN 同样可按该 TTL 长期缓存，回源压力接近零。</p>
     */
    public static final String PUBLIC = "public, max-age=31536000, immutable";

    /**
     * 私有文件：禁止落盘。
     *
     * <p>私有下载需要登录态，若允许浏览器缓存，同一台共用电脑上换个账号仍能从缓存里
     * 读到上一个账号的图片。这里宁可每次重新下载。</p>
     */
    public static final String PRIVATE = "private, no-store";

    private StorageCachePolicy() {
    }

    /**
     * 按访问类型取缓存策略。
     *
     * @param publicAccess 是否公开访问
     * @return 可直接写入 {@code Cache-Control} 的值
     */
    public static String of(boolean publicAccess) {
        return publicAccess ? PUBLIC : PRIVATE;
    }
}
