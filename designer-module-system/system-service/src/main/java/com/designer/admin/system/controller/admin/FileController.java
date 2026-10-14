package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.core.R;
import com.designer.admin.framework.storage.FileStorageService;
import com.designer.admin.system.mapper.SysFileMapper;
import com.designer.admin.system.model.entity.SysFile;
import com.designer.admin.system.service.SysFileService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件上传 / 下载（上传会写入 sys_file 元数据；下载按文件记录的存储配置读取）
 */
@Controller
@Mapping("/admin-api/system/file")
public class FileController {

    @Inject
    private FileStorageService storageService;

    @Inject
    private SysFileService fileService;

    @Db
    private SysFileMapper fileMapper;

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
     * 下载 / 预览（公开，无需登录）。
     */
    @SaIgnore
    @Get
    @Mapping("/download/**")
    public void download(Context ctx) throws Throwable {
        String path = ctx.path();
        // 相对路径取 /download/ 之后，避免与配置前缀硬编码耦合
        int marker = path.indexOf("/download/");
        String relative = marker >= 0 ? path.substring(marker + "/download/".length()) : "";
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        if (relative.isBlank() || relative.contains("..")) {
            ctx.status(404);
            ctx.output("文件不存在");
            return;
        }

        SysFile meta = fileMapper.selectOne(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getFilePath, relative)
                .last("LIMIT 1"));
        Long configId = meta == null ? null : meta.getConfigId();
        String storageType = meta == null ? null : meta.getStorageType();

        try (InputStream in = storageService.open(relative, configId, storageType)) {
            String name = relative.contains("/") ? relative.substring(relative.lastIndexOf('/') + 1) : relative;
            ctx.contentType(meta != null && meta.getContentType() != null && !meta.getContentType().isBlank()
                    ? meta.getContentType()
                    : "application/octet-stream");
            ctx.headerSet("Content-Disposition", "inline; filename=\"" + name + "\"");
            in.transferTo(ctx.outputStream());
        } catch (Exception e) {
            ctx.status(404);
            ctx.output("文件不存在");
        }
    }
}
