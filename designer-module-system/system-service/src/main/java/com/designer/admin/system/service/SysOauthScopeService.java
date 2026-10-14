package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.mapper.SysOauthClientScopeMapper;
import com.designer.admin.system.mapper.SysOauthScopeMapper;
import com.designer.admin.system.model.dto.OauthScopeQueryDTO;
import com.designer.admin.system.model.dto.OauthScopeSaveDTO;
import com.designer.admin.system.model.entity.SysOauthClientScope;
import com.designer.admin.system.model.entity.SysOauthScope;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.List;
import java.util.Locale;

@Component
public class SysOauthScopeService {

    @Db
    private SysOauthScopeMapper scopeMapper;
    @Db
    private SysOauthClientScopeMapper clientScopeMapper;

    public IPage<SysOauthScope> page(OauthScopeQueryDTO query) {
        return scopeMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysOauthScope>()
                .like(notBlank(query.getScopeCode()), SysOauthScope::getScopeCode, query.getScopeCode())
                .like(notBlank(query.getScopeName()), SysOauthScope::getScopeName, query.getScopeName())
                .eq(query.getStatus() != null, SysOauthScope::getStatus, query.getStatus())
                .orderByAsc(SysOauthScope::getSort)
                .orderByAsc(SysOauthScope::getId));
    }

    public List<SysOauthScope> listEnabled() {
        return scopeMapper.selectList(new LambdaQueryWrapper<SysOauthScope>()
                .eq(SysOauthScope::getStatus, 1)
                .orderByAsc(SysOauthScope::getSort)
                .orderByAsc(SysOauthScope::getId));
    }

    public List<SysOauthScope> listAllEnabledPatterns() {
        return listEnabled();
    }

    public SysOauthScope getById(Long id) {
        return scopeMapper.selectById(id);
    }

    @Transaction
    public SysOauthScope save(OauthScopeSaveDTO dto) {
        SysOauthScope entity = toEntity(dto);
        assertCodeUnique(entity.getScopeCode(), null);
        scopeMapper.insert(entity);
        return entity;
    }

    @Transaction
    public void update(OauthScopeSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("Scope ID不能为空");
        }
        if (scopeMapper.selectById(dto.getId()) == null) {
            throw new BizException("Scope 不存在");
        }
        SysOauthScope entity = toEntity(dto);
        assertCodeUnique(entity.getScopeCode(), dto.getId());
        scopeMapper.updateById(entity);
    }

    @Transaction
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            Long bound = clientScopeMapper.selectCount(new LambdaQueryWrapper<SysOauthClientScope>()
                    .eq(SysOauthClientScope::getScopeId, id));
            if (bound != null && bound > 0) {
                SysOauthScope scope = scopeMapper.selectById(id);
                String name = scope == null ? String.valueOf(id) : scope.getScopeCode();
                throw new BizException("Scope【" + name + "】已授权给客户端，无法删除");
            }
        }
        scopeMapper.deleteByIds(ids);
    }

    private SysOauthScope toEntity(OauthScopeSaveDTO dto) {
        SysOauthScope entity = new SysOauthScope();
        entity.setId(dto.getId());
        entity.setScopeCode(normalizeCode(dto.getScopeCode()));
        entity.setScopeName(dto.getScopeName() == null ? "" : dto.getScopeName().trim());
        entity.setPathPattern(normalizePath(dto.getPathPattern()));
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        return entity;
    }

    private void assertCodeUnique(String code, Long excludeId) {
        Long count = scopeMapper.selectCount(new LambdaQueryWrapper<SysOauthScope>()
                .eq(SysOauthScope::getScopeCode, code)
                .ne(excludeId != null, SysOauthScope::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("Scope 标识已存在：" + code);
        }
    }

    private static String normalizeCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException("Scope 标识不能为空");
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePath(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException("路径模式不能为空");
        }
        String p = raw.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return p;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
