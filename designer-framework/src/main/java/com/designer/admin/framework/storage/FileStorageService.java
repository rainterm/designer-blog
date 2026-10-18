package com.designer.admin.framework.storage;

import com.designer.admin.common.exception.BizException;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.UploadedFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件存储门面：从 {@link StorageConfigSource} 读取配置，委托给对应 {@link FileStorageProvider}。
 *
 * @author 雨夜终端
 */
@Component
public class FileStorageService {

    /** 判断 MB 数值是否整数的容差 */
    private static final double INTEGRAL_TOLERANCE = 0.001;

    @Inject
    private FileStorageProviderRegistry providerRegistry;

    @Inject
    private StorageConfigSource storageConfigSource;

    /**
     * 使用默认存储配置上传
     */
    public StoredFile store(UploadedFile file, String subDir) {
        return store(file, subDir, storageConfigSource.requireDefaultRuntime());
    }

    public StoredFile store(UploadedFile file, String subDir, StorageRuntimeConfig config) {
        validate(file, config);
        String original = file.getName() == null ? "" : file.getName();
        String ext = extOf(original);
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String safeSub = sanitize(subDir);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relative = (safeSub.isEmpty() ? "" : safeSub + "/") + datePart + "/" + fileName;
        String contentType = file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();

        FileStorageProvider provider = providerRegistry.getRequired(config.getStorageType());
        provider.store(file, relative, contentType, config);

        return StoredFile.builder()
                .originalName(original)
                .fileName(fileName)
                .filePath(relative)
                .url(provider.buildAccessUrl(relative, config))
                .fileSize(file.getContentSize())
                .contentType(contentType)
                .extension(ext)
                .storageType(config.getStorageType())
                .configId(config.getConfigId())
                .build();
    }

    public void delete(String filePath, String storageType, Long configId) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            StorageRuntimeConfig config = resolveConfig(configId, storageType);
            providerRegistry.getRequired(config.getStorageType()).delete(filePath, config);
        } catch (Exception ignored) {
            // 物理删除失败不阻断业务
        }
    }

    public InputStream open(String filePath, Long configId, String storageType) throws Exception {
        StorageRuntimeConfig config = resolveConfig(configId, storageType);
        return providerRegistry.getRequired(config.getStorageType()).open(filePath, config);
    }

    /**
     * 文件所在存储配置是否允许公开访问。
     * 公开下载接口据此决定是否匿名放行。
     *
     * <p>没有 configId 的历史数据只能落到默认配置；此时若记录的存储类型与默认配置不一致，
     * 说明无法确认归属，按私有处理，避免借用默认配置的公开属性放行。</p>
     */
    public boolean isPublicAccess(Long configId, String storageType) {
        if (configId != null) {
            return storageConfigSource.requireRuntimeById(configId).isPublicAccess();
        }
        StorageRuntimeConfig def = storageConfigSource.requireDefaultRuntime();
        if (storageType != null && !storageType.isBlank()
                && !storageType.equalsIgnoreCase(def.getStorageType())) {
            return false;
        }
        return def.isPublicAccess();
    }

    private StorageRuntimeConfig resolveConfig(Long configId, String storageType) {
        if (configId != null) {
            return storageConfigSource.requireRuntimeById(configId);
        }
        if (storageType != null && !storageType.isBlank()) {
            StorageRuntimeConfig def = storageConfigSource.requireDefaultRuntime();
            if (storageType.equalsIgnoreCase(def.getStorageType())) {
                return def;
            }
        }
        return storageConfigSource.requireDefaultRuntime();
    }

    private void validate(UploadedFile file, StorageRuntimeConfig config) {
        if (file == null) {
            throw new BizException("上传文件不能为空");
        }
        String original = file.getName() == null ? "" : file.getName();
        String ext = extOf(original);
        if (ext.isEmpty()) {
            throw new BizException("文件缺少扩展名");
        }
        Set<String> allowed = Arrays.stream(config.getAllowedExtensions().split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT)).filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (!allowed.contains(ext)) {
            throw new BizException("不支持的文件类型：" + ext);
        }
        if (file.getContentSize() > config.getMaxSize()) {
            throw new BizException("单文件超过大小限制：" + formatMaxSizeMb(config.getMaxSize()));
        }
    }

    private static String formatMaxSizeMb(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        if (Math.abs(mb - Math.rint(mb)) < INTEGRAL_TOLERANCE) {
            return ((long) Math.rint(mb)) + " MB";
        }
        return String.format(Locale.ROOT, "%.2f MB", mb);
    }

    private String extOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitize(String subDir) {
        if (subDir == null || subDir.isBlank()) {
            return "";
        }
        return subDir.replaceAll("[^a-zA-Z0-9/_-]", "").replaceAll("^/+|/+$", "");
    }
}
