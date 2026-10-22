package com.designer.admin.framework.storage.provider;

import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.spi.ConfigFieldMeta;
import com.designer.admin.framework.storage.FileStorageProvider;
import com.designer.admin.framework.storage.StorageCachePolicy;
import com.designer.admin.framework.storage.StorageProperties;
import com.designer.admin.framework.storage.StorageRuntimeConfig;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.annotation.Inject;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * S3 / MinIO 兼容对象存储
 *
 * @author 雨夜终端
 */
@Component
@Slf4j
public class S3FileStorageProvider implements FileStorageProvider {

    public static final String TYPE = "s3";

    /** 访问密钥 */
    private static final String KEY_ACCESS_KEY = "accessKey";

    /** 密钥口令 */
    private static final String KEY_SECRET_KEY = "secretKey";

    /** 存储桶 */
    private static final String KEY_BUCKET = "bucket";

    /** URL 路径分隔符 */
    private static final String URL_SEPARATOR = "/";

    /** configId -> client，配置变更后由 closeClient 清理 */
    private final ConcurrentHashMap<Long, S3Client> clients = new ConcurrentHashMap<>();

    @Inject
    private StorageProperties storageProperties;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String label() {
        return "S3 / MinIO";
    }

    @Override
    public List<ConfigFieldMeta> configFields() {
        return List.of(
                ConfigFieldMeta.builder().name("endpoint").label("Endpoint").inputType("text").required(false)
                        .placeholder("http://127.0.0.1:9000").help("MinIO/自建网关填写；AWS 官方可留空").build(),
                ConfigFieldMeta.builder().name("region").label("Region").inputType("text").required(false)
                        .placeholder("cn-hangzhou")
                        .help("MinIO 可留空；阿里云/腾讯云/华为云/AWS 填写桶所在地域").build(),
                ConfigFieldMeta.builder().name("accessKey").label("Access Key").inputType("text").required(true)
                        .placeholder("Access Key").build(),
                ConfigFieldMeta.builder().name("secretKey").label("Secret Key").inputType("password").required(true)
                        .placeholder("Secret Key").build(),
                ConfigFieldMeta.builder().name("bucket").label("Bucket").inputType("text").required(true)
                        .placeholder("bucket-name").build(),
                ConfigFieldMeta.builder().name("pathStyleAccess").label("Path-Style").inputType("switch").required(false)
                        .defaultValue("true").help("MinIO 建议开启").build(),
                ConfigFieldMeta.builder().name("publicUrl").label("公网/CDN 前缀").inputType("text").required(false)
                        .placeholder("https://cdn.example.com/bucket")
                        .help("配置后直接返回外链；留空则走代理下载前缀").build()
        );
    }

    @Override
    public void validateConfig(Map<String, Object> props) {
        if (blank(props, KEY_ACCESS_KEY) || blank(props, KEY_SECRET_KEY) || blank(props, KEY_BUCKET)) {
            throw new BizException("S3 需配置 accessKey / secretKey / bucket");
        }
    }

    @Override
    public void store(InputStream in, long size, String filePath, String contentType, StorageRuntimeConfig config) {
        S3Client client = clientOf(config);
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(config.prop("bucket"))
                    .key(filePath)
                    .contentType(contentType)
                    .contentLength(size)
                    // 公开配置可能返回 CDN / 公网直链，浏览器与边缘节点根本不经过后端的下载接口，
                    // 因此缓存策略必须在**上传时**写进对象元数据，由对象存储当响应头返回。
                    // 私有对象的直链不会下发（buildAccessUrl 只在公开配置上用 publicUrl），
                    // 但同样写上 no-store：万一以后有人把私有桶接到 CDN 上，也不至于被缓存。
                    .cacheControl(StorageCachePolicy.of(config.isPublicAccess()))
                    .build();
            // 流由调用方关闭；body 的长度必须与压缩后的实际字节数一致，SDK 才能定长签名
            client.putObject(req, RequestBody.fromInputStream(in, size));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("S3 上传失败：" + e.getMessage());
        }
    }

    @Override
    public void delete(String filePath, StorageRuntimeConfig config) {
        try {
            clientOf(config).deleteObject(DeleteObjectRequest.builder()
                    .bucket(config.prop("bucket"))
                    .key(filePath)
                    .build());
        } catch (Exception e) {
            log.warn("S3 删除失败 path={}：{}", filePath, e.getMessage());
        }
    }

    @Override
    public InputStream open(String filePath, StorageRuntimeConfig config) {
        return clientOf(config).getObject(GetObjectRequest.builder()
                .bucket(config.prop("bucket"))
                .key(filePath)
                .build());
    }

    @Override
    public String buildAccessUrl(String filePath, StorageRuntimeConfig config) {
        // 私有配置必须走代理下载（鉴权在 /admin-api），不能返回公网/CDN 直链
        String publicUrl = config.prop("publicUrl");
        if (config.isPublicAccess() && !publicUrl.isBlank()) {
            return publicUrl.replaceAll("/+$", "") + URL_SEPARATOR + filePath;
        }
        String prefix = config.getUrlPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = storageProperties.urlPrefix(config.isPublicAccess());
        }
        if (!prefix.endsWith(URL_SEPARATOR)) {
            prefix = prefix + URL_SEPARATOR;
        }
        return prefix + filePath;
    }

    /** 配置更新后失效对应客户端 */
    public void evictClient(Long configId) {
        if (configId == null) {
            return;
        }
        S3Client old = clients.remove(configId);
        if (old != null) {
            old.close();
        }
    }

    @Destroy
    public void destroy() {
        clients.forEach((id, c) -> {
            try {
                c.close();
            } catch (Exception ignored) {
                // ignore
            }
        });
        clients.clear();
    }

    private S3Client clientOf(StorageRuntimeConfig config) {
        Long id = config.getConfigId() == null ? 0L : config.getConfigId();
        return clients.computeIfAbsent(id, k -> buildClient(config));
    }

    private S3Client buildClient(StorageRuntimeConfig config) {
        String accessKey = config.prop("accessKey");
        String secretKey = config.prop("secretKey");
        if (accessKey.isBlank() || secretKey.isBlank()) {
            throw new BizException("S3 accessKey / secretKey 未配置");
        }
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        var s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(config.propBool("pathStyleAccess", true))
                .build();
        var builder = S3Client.builder()
                .credentialsProvider(credentials)
                // SDK 要求 region；MinIO 无关，统一默认 us-east-1
                .region(Region.of(config.prop("region", "us-east-1")))
                .serviceConfiguration(s3Config);
        String endpoint = config.prop("endpoint");
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    private static boolean blank(Map<String, Object> props, String key) {
        if (props == null || props.get(key) == null) {
            return true;
        }
        return String.valueOf(props.get(key)).isBlank();
    }
}
