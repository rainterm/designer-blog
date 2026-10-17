package com.designer.admin.system.controller.admin;

import com.designer.admin.common.core.R;
import com.designer.admin.system.model.entity.SysFile;
import com.designer.admin.system.service.SysFileService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件上传 / 私有下载（上传会写入 sys_file 元数据；下载按文件记录的存储配置读取）
 */
@Controller
@Mapping("/admin-api/system/file")
public class FileController {

    @Inject
    private SysFileService fileService;

    /** 上传，返回可访问 URL 与文件记录 */
    @Post
    @Mapping("/upload")
    public R<Map<String, Object>> upload(UploadedFile file, String dir) {
        SysFile saved = fileService.upload(file, dir);
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
