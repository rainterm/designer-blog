package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.mapper.SysConfigMapper;
import com.designer.admin.system.model.dto.ConfigQueryDTO;
import com.designer.admin.system.model.dto.ConfigSaveDTO;
import com.designer.admin.system.model.entity.SysConfig;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.redisx.RedisClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import java.util.List;

/**
 * 参数配置：CRUD + Redis 缓存按键取值
 */
@Component
public class SysConfigService {
    public static final String KEY_CAPTCHA_ENABLED = "sys.account.captchaEnabled";
    /** 连续登录失败达到该次数后才要求验证码，默认 3 */
    public static final String KEY_CAPTCHA_FAIL_THRESHOLD = "sys.account.captchaFailThreshold";
    public static final String KEY_LOCK_ENABLED = "sys.account.lockEnabled";
    public static final String KEY_LOCK_MAX_RETRY = "sys.account.lockMaxRetry";
    public static final String KEY_LOCK_MINUTES = "sys.account.lockMinutes";
    /** Token 绝对有效期（秒），默认 86400=1 天；到期必须重新登录 */
    public static final String KEY_TOKEN_TIMEOUT = "sys.account.tokenTimeout";
    public static final int DEFAULT_TOKEN_TIMEOUT = 86400;
    /** Token 活跃续期（秒），默认 1800=30 分钟；有请求则续期，连续闲置超时则踢出；-1 关闭 */
    public static final String KEY_TOKEN_ACTIVE_TIMEOUT = "sys.account.tokenActiveTimeout";
    public static final int DEFAULT_TOKEN_ACTIVE_TIMEOUT = 1800;


    private static final String CACHE_PREFIX = "sys:config:";

    @Db
    private SysConfigMapper configMapper;
    @Inject
    private RedisClient redisClient;

    public IPage<SysConfig> page(ConfigQueryDTO query) {
        return configMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysConfig>()
                .like(notBlank(query.getConfigName()), SysConfig::getConfigName, query.getConfigName())
                .like(notBlank(query.getConfigKey()), SysConfig::getConfigKey, query.getConfigKey())
                .eq(notBlank(query.getConfigType()), SysConfig::getConfigType, query.getConfigType())
                .orderByAsc(SysConfig::getId));
    }

    public SysConfig getById(Long id) {
        SysConfig cfg = configMapper.selectById(id);
        if (cfg == null) {
            throw new BizException("参数不存在");
        }
        return cfg;
    }

    /** 按键取值（带 Redis 缓存） */
    public String getValue(String configKey) {
        if (!notBlank(configKey)) {
            return null;
        }
        String cacheKey = CACHE_PREFIX + configKey;
        String cached = redisClient.open1(s -> s.key(cacheKey).get());
        if (cached != null) {
            return cached;
        }
        SysConfig cfg = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey)
                .last("limit 1"));
        String value = cfg == null ? null : cfg.getConfigValue();
        if (value != null) {
            redisClient.open0(s -> s.key(cacheKey).expire(3600).set(value));
        }
        return value;
    }

    public boolean getBool(String configKey, boolean defaultValue) {
        String v = getValue(configKey);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim()) || "Y".equalsIgnoreCase(v.trim());
    }

    public int getInt(String configKey, int defaultValue) {
        String v = getValue(configKey);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Token 绝对有效期（秒），非法或过小则回退默认 */
    public long getTokenTimeoutSeconds() {
        int seconds = getInt(KEY_TOKEN_TIMEOUT, DEFAULT_TOKEN_TIMEOUT);
        if (seconds < 60) {
            return DEFAULT_TOKEN_TIMEOUT;
        }
        return seconds;
    }

    /**
     * Token 活跃续期窗口（秒）。
     * -1 = 关闭活跃续期；其它非法/过小回退默认 1800。
     */
    public long getTokenActiveTimeoutSeconds() {
        String v = getValue(KEY_TOKEN_ACTIVE_TIMEOUT);
        if (v == null || v.isBlank()) {
            return DEFAULT_TOKEN_ACTIVE_TIMEOUT;
        }
        try {
            int seconds = Integer.parseInt(v.trim());
            if (seconds == -1) {
                return -1;
            }
            if (seconds < 60) {
                return DEFAULT_TOKEN_ACTIVE_TIMEOUT;
            }
            return seconds;
        } catch (NumberFormatException e) {
            return DEFAULT_TOKEN_ACTIVE_TIMEOUT;
        }
    }

    public void save(ConfigSaveDTO dto) {
        assertKeyUnique(dto.getConfigKey(), null);
        SysConfig cfg = toEntity(dto);
        if (!notBlank(cfg.getConfigType())) {
            cfg.setConfigType("N");
        }
        configMapper.insert(cfg);
        clearCache(cfg.getConfigKey());
    }

    public void update(ConfigSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("参数ID不能为空");
        }
        SysConfig exist = getById(dto.getId());
        assertKeyUnique(dto.getConfigKey(), dto.getId());
        SysConfig cfg = toEntity(dto);
        // 内置标记不允许被改成可删
        if ("Y".equalsIgnoreCase(exist.getConfigType())) {
            cfg.setConfigType("Y");
        } else if (!notBlank(cfg.getConfigType())) {
            cfg.setConfigType("N");
        }
        configMapper.updateById(cfg);
        clearCache(exist.getConfigKey());
        if (!exist.getConfigKey().equals(cfg.getConfigKey())) {
            clearCache(cfg.getConfigKey());
        }
    }

    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<SysConfig> list = configMapper.selectList(new LambdaQueryWrapper<SysConfig>().in(SysConfig::getId, ids));
        for (SysConfig cfg : list) {
            if ("Y".equalsIgnoreCase(cfg.getConfigType())) {
                throw new BizException("内置参数「" + cfg.getConfigKey() + "」不允许删除");
            }
        }
        configMapper.deleteByIds(ids);
        for (SysConfig cfg : list) {
            clearCache(cfg.getConfigKey());
        }
    }

    public void clearCache(String configKey) {
        if (!notBlank(configKey)) {
            return;
        }
        redisClient.open0(s -> s.key(CACHE_PREFIX + configKey).delete());
    }

    public void refreshCache() {
        List<SysConfig> all = configMapper.selectList(null);
        for (SysConfig cfg : all) {
            clearCache(cfg.getConfigKey());
        }
    }

    private void assertKeyUnique(String key, Long excludeId) {
        Long cnt = configMapper.selectCount(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key)
                .ne(excludeId != null, SysConfig::getId, excludeId));
        if (cnt != null && cnt > 0) {
            throw new BizException("参数键名已存在");
        }
    }

    private SysConfig toEntity(ConfigSaveDTO dto) {
        SysConfig cfg = new SysConfig();
        cfg.setId(dto.getId());
        cfg.setConfigName(dto.getConfigName());
        cfg.setConfigKey(dto.getConfigKey());
        cfg.setConfigValue(dto.getConfigValue());
        cfg.setConfigType(dto.getConfigType());
        cfg.setRemark(dto.getRemark());
        return cfg;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
