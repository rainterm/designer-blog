package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.framework.storage.FileStorageProvider;
import com.designer.admin.framework.storage.FileStorageProviderRegistry;
import com.designer.admin.framework.storage.StorageConfigSource;
import com.designer.admin.framework.storage.StorageFieldMeta;
import com.designer.admin.framework.storage.StorageProperties;
import com.designer.admin.framework.storage.StorageRuntimeConfig;
import com.designer.admin.framework.storage.provider.S3FileStorageProvider;
import com.designer.admin.system.mapper.SysStorageConfigMapper;
import com.designer.admin.system.model.dto.StorageConfigQueryDTO;
import com.designer.admin.system.model.dto.StorageConfigSaveDTO;
import com.designer.admin.system.model.entity.SysStorageConfig;
import com.designer.admin.system.model.vo.ConfigFieldMeta;
import com.designer.admin.system.model.vo.StorageTypeVO;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件存储配置：CRUD + 默认配置缓存 + 运行时解析；实现 framework {@link StorageConfigSource}。
 */
@Component
public class SysStorageConfigService implements StorageConfigSource {

    private static final String SECRET_MASK = "******";

    @Db
    private SysStorageConfigMapper configMapper;

    @Inject
    private FileStorageProviderRegistry providerRegistry;

    @Inject
    private S3FileStorageProvider s3Provider;

    @Inject
    private StorageProperties storageProperties;

    /** 默认配置缓存 */
    private volatile StorageRuntimeConfig cachedDefault;

    public IPage<SysStorageConfig> page(StorageConfigQueryDTO query) {
        IPage<SysStorageConfig> page = configMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysStorageConfig>()
                .like(notBlank(query.getConfigName()), SysStorageConfig::getConfigName, query.getConfigName())
                .eq(notBlank(query.getStorageType()), SysStorageConfig::getStorageType, query.getStorageType())
                .eq(query.getStatus() != null, SysStorageConfig::getStatus, query.getStatus())
                .orderByDesc(SysStorageConfig::getIsDefault)
                .orderByDesc(SysStorageConfig::getId));
        page.getRecords().forEach(this::maskSecrets);
        return page;
    }

    public SysStorageConfig getById(Long id) {
        SysStorageConfig cfg = configMapper.selectById(id);
        if (cfg == null) {
            throw new BizException("存储配置不存在");
        }
        return maskSecrets(cfg);
    }

    /** 详情（含脱敏后的 config Map，便于前端回填） */
    public Map<String, Object> detailView(Long id) {
        SysStorageConfig cfg = getById(id);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", cfg.getId());
        view.put("configName", cfg.getConfigName());
        view.put("storageType", cfg.getStorageType());
        view.put("isDefault", cfg.getIsDefault());
        view.put("status", cfg.getStatus());
        view.put("urlPrefix", cfg.getUrlPrefix());
        view.put("allowedExtensions", cfg.getAllowedExtensions());
        view.put("maxSize", cfg.getMaxSize());
        view.put("remark", cfg.getRemark());
        view.put("config", JsonUtil.toMap(cfg.getConfigJson()));
        view.put("createTime", cfg.getCreateTime());
        view.put("updateTime", cfg.getUpdateTime());
        return view;
    }

    public List<StorageTypeVO> listTypes() {
        String defaultUrlPrefix = storageProperties.normalizedUrlPrefix();
        return providerRegistry.list().stream().map(p -> {
            StorageTypeVO vo = new StorageTypeVO();
            vo.setType(p.type());
            vo.setLabel(p.label());
            vo.setFields(p.configFields().stream().map(this::toConfigField).toList());
            vo.setDefaultUrlPrefix(defaultUrlPrefix);
            return vo;
        }).toList();
    }

    @Transaction
    public void save(StorageConfigSaveDTO dto) {
        validateSave(dto, true);
        FileStorageProvider provider = providerRegistry.getRequired(dto.getStorageType());
        Map<String, Object> props = dto.getConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getConfig());
        validateRequiredFields(provider, props);
        provider.validateConfig(props);

        SysStorageConfig entity = new SysStorageConfig();
        fillEntity(entity, dto, props);
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            clearDefaultFlag();
        } else if (countEnabled() == 0) {
            entity.setIsDefault(1);
        }
        configMapper.insert(entity);
        clearCache();
    }

    @Transaction
    public void update(StorageConfigSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("配置 ID 不能为空");
        }
        SysStorageConfig existing = configMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BizException("存储配置不存在");
        }
        validateSave(dto, false);
        FileStorageProvider provider = providerRegistry.getRequired(dto.getStorageType());

        Map<String, Object> props = mergeConfig(existing, dto);
        validateRequiredFields(provider, props);
        provider.validateConfig(props);

        SysStorageConfig entity = new SysStorageConfig();
        entity.setId(dto.getId());
        fillEntity(entity, dto, props);
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            clearDefaultFlag();
        }
        configMapper.updateById(entity);
        evictProviderClient(dto.getId());
        clearCache();
    }

    @Transaction
    public void removeByIds(String idsCsv) {
        List<Long> ids = parseIds(idsCsv);
        if (ids.isEmpty()) {
            throw new BizException("请选择要删除的配置");
        }
        Long defaultId = findDefaultId();
        for (Long id : ids) {
            if (defaultId != null && defaultId.equals(id)) {
                throw new BizException("不能删除默认存储配置，请先切换默认项");
            }
            configMapper.deleteById(id);
            evictProviderClient(id);
        }
        clearCache();
    }

    @Transaction
    public void setDefault(Long id) {
        SysStorageConfig cfg = configMapper.selectById(id);
        if (cfg == null) {
            throw new BizException("存储配置不存在");
        }
        if (cfg.getStatus() == null || cfg.getStatus() != 1) {
            throw new BizException("停用的配置不能设为默认");
        }
        clearDefaultFlag();
        SysStorageConfig upd = new SysStorageConfig();
        upd.setId(id);
        upd.setIsDefault(1);
        configMapper.updateById(upd);
        clearCache();
    }

    @Transaction
    public void changeStatus(Long id, Integer status) {
        SysStorageConfig cfg = configMapper.selectById(id);
        if (cfg == null) {
            throw new BizException("存储配置不存在");
        }
        if (status != null && status == 0 && cfg.getIsDefault() != null && cfg.getIsDefault() == 1) {
            throw new BizException("默认配置不能停用，请先切换默认项");
        }
        SysStorageConfig upd = new SysStorageConfig();
        upd.setId(id);
        upd.setStatus(status);
        configMapper.updateById(upd);
        clearCache();
    }

    /** 获取当前默认启用配置（带缓存） */
    public StorageRuntimeConfig requireDefaultRuntime() {
        StorageRuntimeConfig cached = cachedDefault;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cachedDefault != null) {
                return cachedDefault;
            }
            SysStorageConfig cfg = configMapper.selectOne(new LambdaQueryWrapper<SysStorageConfig>()
                    .eq(SysStorageConfig::getIsDefault, 1)
                    .eq(SysStorageConfig::getStatus, 1)
                    .last("LIMIT 1"));
            if (cfg == null) {
                cfg = configMapper.selectOne(new LambdaQueryWrapper<SysStorageConfig>()
                        .eq(SysStorageConfig::getStatus, 1)
                        .orderByAsc(SysStorageConfig::getId)
                        .last("LIMIT 1"));
            }
            if (cfg == null) {
                throw new BizException("未配置可用的文件存储，请先在「存储配置」中添加并启用");
            }
            cachedDefault = toRuntime(cfg);
            return cachedDefault;
        }
    }

    public StorageRuntimeConfig requireRuntimeById(Long configId) {
        if (configId == null) {
            return requireDefaultRuntime();
        }
        SysStorageConfig cfg = configMapper.selectById(configId);
        if (cfg == null) {
            throw new BizException("文件对应的存储配置不存在");
        }
        return toRuntime(cfg);
    }

    public StorageRuntimeConfig toRuntime(SysStorageConfig cfg) {
        return StorageRuntimeConfig.builder()
                .configId(cfg.getId())
                .configName(cfg.getConfigName())
                .storageType(cfg.getStorageType())
                .urlPrefix(cfg.getUrlPrefix())
                .allowedExtensions(cfg.getAllowedExtensions())
                .maxSize(cfg.getMaxSize() == null ? 5242880L : cfg.getMaxSize())
                .props(JsonUtil.toMap(cfg.getConfigJson()))
                .build();
    }

    public void clearCache() {
        cachedDefault = null;
    }

    private void fillEntity(SysStorageConfig entity, StorageConfigSaveDTO dto, Map<String, Object> props) {
        entity.setConfigName(dto.getConfigName().trim());
        entity.setStorageType(dto.getStorageType().trim());
        entity.setIsDefault(dto.getIsDefault() == null ? 0 : dto.getIsDefault());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setUrlPrefix(blankToDefault(dto.getUrlPrefix(), storageProperties.normalizedUrlPrefix()));
        entity.setAllowedExtensions(blankToDefault(dto.getAllowedExtensions(),
                "jpg,jpeg,png,gif,webp,bmp,pdf,doc,docx,xls,xlsx,ppt,pptx,zip,txt"));
        entity.setMaxSize(dto.getMaxSize() == null || dto.getMaxSize() <= 0 ? 5242880L : dto.getMaxSize());
        entity.setConfigJson(JsonUtil.toJson(props));
        entity.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
    }

    private Map<String, Object> mergeConfig(SysStorageConfig existing, StorageConfigSaveDTO dto) {
        Map<String, Object> oldProps = JsonUtil.toMap(existing.getConfigJson());
        Map<String, Object> incoming = dto.getConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getConfig());
        Set<String> secretFields = secretFieldNames(dto.getStorageType());
        for (String key : secretFields) {
            Object v = incoming.get(key);
            if (v == null || String.valueOf(v).isBlank() || SECRET_MASK.equals(String.valueOf(v))) {
                if (oldProps.containsKey(key)) {
                    incoming.put(key, oldProps.get(key));
                }
            }
        }
        return incoming;
    }

    private SysStorageConfig maskSecrets(SysStorageConfig cfg) {
        Map<String, Object> props = JsonUtil.toMap(cfg.getConfigJson());
        Set<String> secrets = secretFieldNames(cfg.getStorageType());
        boolean changed = false;
        for (String key : secrets) {
            Object v = props.get(key);
            if (v != null && !String.valueOf(v).isBlank()) {
                props.put(key, SECRET_MASK);
                changed = true;
            }
        }
        if (changed) {
            cfg.setConfigJson(JsonUtil.toJson(props));
        }
        return cfg;
    }

    private Set<String> secretFieldNames(String storageType) {
        if (!providerRegistry.supports(storageType)) {
            return Set.of();
        }
        return providerRegistry.getRequired(storageType).configFields().stream()
                .filter(f -> "password".equalsIgnoreCase(f.getInputType()))
                .map(StorageFieldMeta::getName)
                .collect(Collectors.toSet());
    }

    private void validateSave(StorageConfigSaveDTO dto, boolean creating) {
        if (!notBlank(dto.getConfigName())) {
            throw new BizException("配置名称不能为空");
        }
        if (!notBlank(dto.getStorageType())) {
            throw new BizException("存储类型不能为空");
        }
        if (!providerRegistry.supports(dto.getStorageType())) {
            throw new BizException("不支持的存储类型：" + dto.getStorageType());
        }
        if (creating && dto.getStatus() != null && dto.getStatus() == 0
                && dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            throw new BizException("停用配置不能设为默认");
        }
    }

    /** 按 Provider 字段元数据校验类型专属必填项 */
    private void validateRequiredFields(FileStorageProvider provider, Map<String, Object> props) {
        for (StorageFieldMeta field : provider.configFields()) {
            if (!field.isRequired() || "switch".equalsIgnoreCase(field.getInputType())) {
                continue;
            }
            Object raw = props == null ? null : props.get(field.getName());
            if (raw == null || String.valueOf(raw).isBlank() || SECRET_MASK.equals(String.valueOf(raw))) {
                throw new BizException(field.getLabel() + "不能为空");
            }
        }
    }

    private void clearDefaultFlag() {
        configMapper.update(null, new LambdaUpdateWrapper<SysStorageConfig>()
                .eq(SysStorageConfig::getIsDefault, 1)
                .set(SysStorageConfig::getIsDefault, 0));
    }

    private long countEnabled() {
        return configMapper.selectCount(new LambdaQueryWrapper<SysStorageConfig>()
                .eq(SysStorageConfig::getStatus, 1));
    }

    private Long findDefaultId() {
        SysStorageConfig cfg = configMapper.selectOne(new LambdaQueryWrapper<SysStorageConfig>()
                .eq(SysStorageConfig::getIsDefault, 1)
                .last("LIMIT 1"));
        return cfg == null ? null : cfg.getId();
    }

    private void evictProviderClient(Long configId) {
        if (s3Provider != null) {
            s3Provider.evictClient(configId);
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

    private ConfigFieldMeta toConfigField(StorageFieldMeta field) {
        return ConfigFieldMeta.builder()
                .name(field.getName())
                .label(field.getLabel())
                .inputType(field.getInputType())
                .required(field.isRequired())
                .placeholder(field.getPlaceholder())
                .defaultValue(field.getDefaultValue())
                .help(field.getHelp())
                .build();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return notBlank(value) ? value.trim() : defaultValue;
    }
}
