package com.designer.admin.system.service;

import cn.dev33.satoken.secure.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.mapper.SysOauthClientMapper;
import com.designer.admin.system.mapper.SysOauthClientScopeMapper;
import com.designer.admin.system.mapper.SysOauthScopeMapper;
import com.designer.admin.system.model.dto.OauthClientQueryDTO;
import com.designer.admin.system.model.dto.OauthClientSaveDTO;
import com.designer.admin.system.model.entity.SysOauthClient;
import com.designer.admin.system.model.entity.SysOauthClientScope;
import com.designer.admin.system.model.entity.SysOauthScope;
import com.designer.admin.system.model.vo.OauthClientCreatedVO;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Component
public class SysOauthClientService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Db
    private SysOauthClientMapper clientMapper;
    @Db
    private SysOauthClientScopeMapper clientScopeMapper;
    @Db
    private SysOauthScopeMapper scopeMapper;

    public IPage<SysOauthClient> page(OauthClientQueryDTO query) {
        IPage<SysOauthClient> page = clientMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysOauthClient>()
                .like(notBlank(query.getClientId()), SysOauthClient::getClientId, query.getClientId())
                .like(notBlank(query.getClientName()), SysOauthClient::getClientName, query.getClientName())
                .eq(query.getStatus() != null, SysOauthClient::getStatus, query.getStatus())
                .orderByDesc(SysOauthClient::getId));
        for (SysOauthClient c : page.getRecords()) {
            fillScopes(c);
        }
        return page;
    }

    public SysOauthClient getById(Long id) {
        SysOauthClient client = clientMapper.selectById(id);
        if (client != null) {
            fillScopes(client);
        }
        return client;
    }

    public SysOauthClient getEnabledByClientId(String clientId) {
        if (!notBlank(clientId)) {
            return null;
        }
        return clientMapper.selectOne(new LambdaQueryWrapper<SysOauthClient>()
                .eq(SysOauthClient::getClientId, clientId.trim())
                .eq(SysOauthClient::getStatus, 1)
                .last("LIMIT 1"));
    }

    public List<String> listAuthorizedScopeCodes(Long clientPk) {
        return clientScopeMapper.selectScopeCodesByClientPk(clientPk);
    }

    @Transaction
    public OauthClientCreatedVO save(OauthClientSaveDTO dto) {
        String clientId = normalizeClientId(dto.getClientId());
        assertClientIdUnique(clientId, null);
        String plainSecret = generateSecret();
        SysOauthClient entity = toEntity(dto, clientId);
        entity.setClientSecret(BCrypt.hashpw(plainSecret));
        clientMapper.insert(entity);
        replaceScopes(entity.getId(), dto.getScopeIds());
        return toCreatedVo(entity, plainSecret);
    }

    @Transaction
    public void update(OauthClientSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("客户端ID不能为空");
        }
        SysOauthClient existing = clientMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BizException("OAuth 客户端不存在");
        }
        String clientId = normalizeClientId(dto.getClientId());
        assertClientIdUnique(clientId, dto.getId());
        SysOauthClient entity = toEntity(dto, clientId);
        entity.setClientSecret(existing.getClientSecret());
        clientMapper.updateById(entity);
        replaceScopes(entity.getId(), dto.getScopeIds());
    }

    @Transaction
    public OauthClientCreatedVO resetSecret(Long id) {
        SysOauthClient existing = clientMapper.selectById(id);
        if (existing == null) {
            throw new BizException("OAuth 客户端不存在");
        }
        String plainSecret = generateSecret();
        SysOauthClient patch = new SysOauthClient();
        patch.setId(id);
        patch.setClientSecret(BCrypt.hashpw(plainSecret));
        clientMapper.updateById(patch);
        existing.setClientSecret(null);
        return toCreatedVo(existing, plainSecret);
    }

    @Transaction
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            clientScopeMapper.delete(new LambdaQueryWrapper<SysOauthClientScope>()
                    .eq(SysOauthClientScope::getClientId, id));
        }
        clientMapper.deleteByIds(ids);
    }

    private void fillScopes(SysOauthClient client) {
        List<Long> scopeIds = clientScopeMapper.selectScopeIdsByClientPk(client.getId());
        client.setScopeIds(scopeIds);
        client.setScopeCodes(clientScopeMapper.selectScopeCodesByClientPk(client.getId()));
    }

    private void replaceScopes(Long clientPk, List<Long> scopeIds) {
        clientScopeMapper.delete(new LambdaQueryWrapper<SysOauthClientScope>()
                .eq(SysOauthClientScope::getClientId, clientPk));
        if (scopeIds == null || scopeIds.isEmpty()) {
            return;
        }
        for (Long scopeId : scopeIds) {
            if (scopeId == null) {
                continue;
            }
            SysOauthScope scope = scopeMapper.selectById(scopeId);
            if (scope == null || scope.getStatus() == null || scope.getStatus() != 1) {
                throw new BizException("无效或已停用的 Scope：" + scopeId);
            }
            SysOauthClientScope rel = new SysOauthClientScope();
            rel.setClientId(clientPk);
            rel.setScopeId(scopeId);
            clientScopeMapper.insert(rel);
        }
    }

    private SysOauthClient toEntity(OauthClientSaveDTO dto, String clientId) {
        SysOauthClient entity = new SysOauthClient();
        entity.setId(dto.getId());
        entity.setClientId(clientId);
        entity.setClientName(dto.getClientName() == null ? "" : dto.getClientName().trim());
        int ttl = dto.getAccessTokenTtl() == null ? 7200 : dto.getAccessTokenTtl();
        if (ttl < 60) {
            throw new BizException("access_token 有效期不能小于 60 秒");
        }
        entity.setAccessTokenTtl(ttl);
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        return entity;
    }

    private void assertClientIdUnique(String clientId, Long excludeId) {
        Long count = clientMapper.selectCount(new LambdaQueryWrapper<SysOauthClient>()
                .eq(SysOauthClient::getClientId, clientId)
                .ne(excludeId != null, SysOauthClient::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("client_id 已存在：" + clientId);
        }
    }

    private static String normalizeClientId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException("client_id 不能为空");
        }
        String id = raw.trim().toLowerCase(Locale.ROOT);
        if (!id.matches("^[a-z][a-z0-9_-]{2,63}$")) {
            throw new BizException("client_id 需小写字母开头，3-64 位字母数字_-");
        }
        return id;
    }

    private static String generateSecret() {
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    private static OauthClientCreatedVO toCreatedVo(SysOauthClient entity, String plainSecret) {
        OauthClientCreatedVO vo = new OauthClientCreatedVO();
        vo.setId(entity.getId());
        vo.setClientId(entity.getClientId());
        vo.setClientName(entity.getClientName());
        vo.setClientSecret(plainSecret);
        return vo;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
