package com.designer.admin.framework.crypto;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.vault.VaultUtils;

import java.util.Collection;
import java.util.Map;

/**
 * 配置类密钥（对象存储 AK/SK、SMTP 密码等）的落库加解密。
 *
 * <p><b>为什么需要</b>：存储配置、消息通道配置都以 JSON 存在业务表里，
 * 密码字段此前只在前端回显时打码，数据库里仍是明文 —— 一次库表导出 / 备份泄露，
 * 就等于把对象存储与邮箱的控制权一起交出去。</p>
 *
 * <p><b>用的是 Solon Vault</b>：{@link VaultUtils#encrypt} 产出 {@code ENC(...)} 形态的密文，
 * 主密钥来自 {@code solon.vault.password}。未配置主密钥时整段逻辑不做任何改写，
 * 历史明文数据照常读取（升级过程不需要停机刷库）；配置之后新写入的密钥才变成密文。</p>
 *
 * <p><b>解密失败只记日志、不抛异常</b>：密文与主密钥不匹配（换了密钥 / 从别处导入的库）
 * 时，抛异常会让存储与消息功能整体不可用。这里保留原值并打 error 日志，
 * 让运维从「上传失败」的日志里看到真正原因，而不是一个启动期的 500。</p>
 *
 * @author 雨夜终端
 */
@Slf4j
public final class ConfigSecretUtil {

    /** Vault 主密钥配置项 */
    public static final String VAULT_PASSWORD_KEY = "solon.vault.password";

    private ConfigSecretUtil() {
    }

    /** 是否配置了主密钥；未配置则加解密均为空操作 */
    public static boolean enabled() {
        return Utils.isNotEmpty(Solon.cfg().get(VAULT_PASSWORD_KEY));
    }

    /**
     * 加密指定字段（已是密文的跳过，避免重复加密）。
     *
     * @param props     配置键值
     * @param secretKeys 需要加密的字段名
     */
    public static void encrypt(Map<String, Object> props, Collection<String> secretKeys) {
        if (!enabled() || props == null || secretKeys == null || secretKeys.isEmpty()) {
            return;
        }
        for (String key : secretKeys) {
            Object value = props.get(key);
            if (value == null) {
                continue;
            }
            String raw = String.valueOf(value);
            if (raw.isBlank() || VaultUtils.isEncrypted(raw)) {
                continue;
            }
            props.put(key, VaultUtils.encrypt(raw));
        }
    }

    /**
     * 解密指定字段（非 {@code ENC(...)} 形态的原样保留，兼容历史明文数据）。
     *
     * @param props     配置键值
     * @param secretKeys 需要解密的字段名
     */
    public static void decrypt(Map<String, Object> props, Collection<String> secretKeys) {
        if (props == null || secretKeys == null || secretKeys.isEmpty()) {
            return;
        }
        for (String key : secretKeys) {
            Object value = props.get(key);
            if (!(value instanceof String raw) || !VaultUtils.isEncrypted(raw)) {
                continue;
            }
            try {
                props.put(key, VaultUtils.decrypt(raw));
            } catch (RuntimeException e) {
                log.error("配置项 {} 解密失败：请确认 solon.vault.password 与加密时一致", key, e);
            }
        }
    }
}
