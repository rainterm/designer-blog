package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.core.PageQuery;
import com.designer.admin.common.util.AsyncRunner;
import com.designer.admin.system.model.entity.SysLoginLog;
import com.designer.admin.system.mapper.SysLoginLogMapper;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;

import java.time.LocalDateTime;

/**
 * 登录日志服务
 */
@Component
public class SysLoginLogService {

    @Db
    private SysLoginLogMapper loginLogMapper;

    public void recordAsync(SysLoginLog log) {
        AsyncRunner.run(() -> {
            try {
                loginLogMapper.insert(log);
            } catch (Exception ignored) {
            }
        });
    }

    public IPage<SysLoginLog> page(PageQuery query, String username, Integer status) {
        LambdaQueryWrapper<SysLoginLog> w = new LambdaQueryWrapper<SysLoginLog>()
                .like(username != null && !username.isEmpty(), SysLoginLog::getUsername, username)
                .eq(status != null, SysLoginLog::getStatus, status)
                .orderByDesc(SysLoginLog::getId);
        return loginLogMapper.selectPage(query.toPage(), w);
    }

    /** 删除指定天数之前的登录日志，返回删除条数（供定时任务调用） */
    public int cleanOlderThan(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(Math.max(days, 1));
        return loginLogMapper.delete(new LambdaQueryWrapper<SysLoginLog>()
                .lt(SysLoginLog::getLoginTime, before));
    }
}
