package com.designer.admin.system.controller.admin;

import com.designer.admin.common.core.R;
import com.designer.admin.system.model.entity.SysFile;
import com.designer.admin.system.model.vo.StorageOptionVO;
import com.designer.admin.system.service.SysFileService;
import com.designer.admin.system.service.SysStorageConfigService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传 / 私有下载（上传会写入 sys_file 元数据；下载按文件记录的存储配置读取）
 */
@Controller
@Mapping("/admin-api/system/file")
public class FileController {

    @Inject
    private SysFileService fileService;

    @Inject
    private SysStorageConfigService storageConfigService;

    /**
     * 上传，返回可访问 URL 与文件记录。
     *
     * @param configId   指定存储配置，留空用默认配置
     * @param publicOnly true 时要求所选配置为公开（登录页 Logo 等匿名可访问的资源），
     *                   落到私有配置会让匿名访客拿到 401
     */
    @Post
    @Mapping("/upload")
    public R<Map<String, Object>> upload(UploadedFile file, String dir, Long configId, Boolean publicOnly) {
        SysFile saved = fileService.upload(file, dir, configId, Boolean.TRUE.equals(publicOnly));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", saved.getId());
        data.put("url", saved.getUrl());
        data.put("name", saved.getOriginalName());
        data.put("fileName", saved.getFileName());
        data.put("fileSize", saved.getFileSize());
        data.put("storageType", saved.getStorageType());
        data.put("configId", saved.getConfigId());
        return R.ok(data);
    }

    /**
     * 上传时可选的公开存储配置。
     *
     * <p>只返回启用的公开项，且不含密钥类字段；供系统设置 / 博客设置这类
     * 「图片要匿名可访问」的上传入口使用，因此不额外要求存储配置的查看权限。</p>
     */
    @Get
    @Mapping("/storage-options")
    public R<List<StorageOptionVO>> storageOptions() {
        return R.ok(storageConfigService.listPublicOptions());
    }

    /**
     * 私有下载 / 预览：需要登录后台。
     *
     * <p>{@code /admin-api/**} 由 Sa-Token 统一要求登录，这里刻意不加 {@code @SaIgnore}。
     * 公开文件请走 {@code /app-api/system/file/download/**}（见 AppFileController）。</p>
     */
    @Get
    @Mapping("/download/**")
    public void download(Context ctx) throws Exception {
        fileService.download(ctx, false);
    }
}
