package com.designer.admin.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.AsyncRunner;
import com.designer.admin.framework.message.MessagePayload;
import com.designer.admin.framework.message.MessageRecordWriter;
import com.designer.admin.framework.message.MessageRuntimeConfig;
import com.designer.admin.framework.satoken.LoginHelper;
import com.designer.admin.system.mapper.SysMessageRecordMapper;
import com.designer.admin.system.model.dto.MessageRecordQueryDTO;
import com.designer.admin.system.model.entity.SysMessageRecord;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息发送记录；实现 framework {@link MessageRecordWriter}。
 */
@Component
public class SysMessageRecordService implements MessageRecordWriter {

    @Db
    private SysMessageRecordMapper recordMapper;

    public IPage<SysMessageRecord> page(MessageRecordQueryDTO query) {
        return recordMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysMessageRecord>()
                .eq(notBlank(query.getChannelType()), SysMessageRecord::getChannelType, query.getChannelType())
                .like(notBlank(query.getTitle()), SysMessageRecord::getTitle, query.getTitle())
                .like(notBlank(query.getReceivers()), SysMessageRecord::getReceivers, query.getReceivers())
                .like(notBlank(query.getSendBy()), SysMessageRecord::getSendBy, query.getSendBy())
                .eq(query.getStatus() != null, SysMessageRecord::getStatus, query.getStatus())
                .orderByDesc(SysMessageRecord::getId));
    }

    public void removeByIds(String idsCsv) {
        List<Long> ids = parseIds(idsCsv);
        if (ids.isEmpty()) {
            throw new BizException("请选择要删除的记录");
        }
        recordMapper.deleteByIds(ids);
    }

    public void clear() {
        recordMapper.delete(new LambdaQueryWrapper<SysMessageRecord>().ge(SysMessageRecord::getId, 0));
    }

    /** 删除指定天数之前的消息记录，返回删除条数 */
    public int cleanOlderThan(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(Math.max(days, 1));
        return recordMapper.delete(new LambdaQueryWrapper<SysMessageRecord>()
                .lt(SysMessageRecord::getSendTime, before));
    }

    /** 异步写入发送记录，不阻断主流程 */
    public void recordAsync(MessageRuntimeConfig config, MessagePayload payload,
                            boolean success, String errorMsg, long costTime) {
        SysMessageRecord record = buildRecord(config, payload, success, errorMsg, costTime);
        AsyncRunner.run(() -> {
            try {
                recordMapper.insert(record);
            } catch (Exception ignored) {
                // ignore
            }
        });
    }

    private SysMessageRecord buildRecord(MessageRuntimeConfig config, MessagePayload payload,
                                         boolean success, String errorMsg, long costTime) {
        SysMessageRecord record = new SysMessageRecord();
        record.setConfigId(config == null ? null : config.getConfigId());
        record.setConfigName(config == null ? "" : nullToEmpty(config.getConfigName()));
        record.setChannelType(config == null ? "" : nullToEmpty(config.getChannelType()));
        record.setReceivers(join(payload == null ? null : payload.getTo()));
        record.setTitle(payload == null ? "" : nullToEmpty(payload.getTitle()));
        String content = payload == null ? "" : nullToEmpty(payload.getContent());
        if (content.length() > 2000) {
            content = content.substring(0, 2000) + "...";
        }
        record.setContent(content);
        record.setHtml(payload != null && payload.isHtml() ? 1 : 0);
        record.setStatus(success ? 1 : 0);
        record.setErrorMsg(errorMsg == null ? "" : (errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg));
        record.setCostTime(costTime);
        record.setSendBy(currentUsername());
        record.setSendTime(LocalDateTime.now());
        return record;
    }

    private static String join(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).collect(Collectors.joining(","));
    }

    private static String currentUsername() {
        try {
            if (!StpUtil.isLogin()) {
                return "";
            }
            return nullToEmpty(LoginHelper.getUsername());
        } catch (Exception e) {
            return "";
        }
    }

    private static List<Long> parseIds(String idsCsv) {
        if (idsCsv == null || idsCsv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(idsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
