package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.storage.FileStorageService;
import com.designer.admin.framework.storage.StoredFile;
import com.designer.admin.system.mapper.SysFileMapper;
import com.designer.admin.system.model.dto.FileQueryDTO;
import com.designer.admin.system.model.entity.SysFile;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.data.annotation.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 系统文件：上传落库、分页查询、删除（含物理文件）
 */
@Component
public class SysFileService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Db
    private SysFileMapper fileMapper;

    @Inject
    private FileStorageService storageService;

    public IPage<SysFile> page(FileQueryDTO query) {
        LambdaQueryWrapper<SysFile> w = new LambdaQueryWrapper<SysFile>()
                .like(notBlank(query.getOriginalName()), SysFile::getOriginalName, query.getOriginalName())
                .eq(notBlank(query.getBizDir()), SysFile::getBizDir, query.getBizDir())
                .eq(notBlank(query.getStorageType()), SysFile::getStorageType, query.getStorageType())
                .eq(notBlank(query.getExtension()), SysFile::getExtension, query.getExtension())
                .like(notBlank(query.getCreateBy()), SysFile::getCreateBy, query.getCreateBy())
                .orderByDesc(SysFile::getId);
        // 条件为 false 时 MP 仍会先求值第三个参数，时间解析必须包在 if 里
        if (notBlank(query.getBeginTime())) {
            w.ge(SysFile::getCreateTime, parseBegin(query.getBeginTime()));
        }
        if (notBlank(query.getEndTime())) {
            w.le(SysFile::getCreateTime, parseEnd(query.getEndTime()));
        }
        return fileMapper.selectPage(query.toPage(), w);
    }

    public SysFile getById(Long id) {
        SysFile file = fileMapper.selectById(id);
        if (file == null) {
            throw new BizException("文件不存在");
        }
        return file;
    }

    /** 上传并写入元数据 */
    public SysFile upload(UploadedFile file, String dir) {
        String bizDir = dir == null || dir.isBlank() ? "common" : dir.trim();
        StoredFile stored = storageService.store(file, bizDir);

        SysFile entity = new SysFile();
        entity.setOriginalName(stored.getOriginalName());
        entity.setFileName(stored.getFileName());
        entity.setFilePath(stored.getFilePath());
        entity.setUrl(stored.getUrl());
        entity.setFileSize(stored.getFileSize());
        entity.setContentType(stored.getContentType());
        entity.setExtension(stored.getExtension());
        entity.setBizDir(bizDir);
        entity.setStorageType(stored.getStorageType());
        entity.setConfigId(stored.getConfigId());
        fileMapper.insert(entity);
        return entity;
    }

    @Transaction
    public void removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<SysFile> files = fileMapper.selectByIds(ids);
        for (SysFile file : files) {
            storageService.delete(file.getFilePath(), file.getStorageType(), file.getConfigId());
            fileMapper.deleteById(file.getId());
        }
    }

    public void removeByIds(String idsCsv) {
        if (idsCsv == null || idsCsv.isBlank()) {
            throw new BizException("请选择要删除的文件");
        }
        List<Long> ids = Arrays.stream(idsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        removeByIds(ids);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static LocalDateTime parseBegin(String day) {
        return LocalDate.parse(day, DAY).atStartOfDay();
    }

    private static LocalDateTime parseEnd(String day) {
        return LocalDate.parse(day, DAY).atTime(LocalTime.MAX);
    }
}
