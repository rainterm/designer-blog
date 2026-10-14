package com.designer.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.designer.admin.system.model.entity.SysOauthClientScope;
import com.designer.admin.system.model.entity.SysOauthScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysOauthClientScopeMapper extends BaseMapper<SysOauthClientScope> {

    @Select("""
            SELECT s.scope_code FROM sys_oauth_scope s
            INNER JOIN sys_oauth_client_scope cs ON cs.scope_id = s.id
            WHERE cs.client_id = #{clientPk} AND s.status = 1 AND s.deleted = 0
            ORDER BY s.sort ASC, s.id ASC
            """)
    List<String> selectScopeCodesByClientPk(@Param("clientPk") Long clientPk);

    @Select("""
            SELECT s.* FROM sys_oauth_scope s
            INNER JOIN sys_oauth_client_scope cs ON cs.scope_id = s.id
            WHERE cs.client_id = #{clientPk} AND s.status = 1 AND s.deleted = 0
            ORDER BY s.sort ASC, s.id ASC
            """)
    List<SysOauthScope> selectScopesByClientPk(@Param("clientPk") Long clientPk);

    @Select("SELECT scope_id FROM sys_oauth_client_scope WHERE client_id = #{clientPk}")
    List<Long> selectScopeIdsByClientPk(@Param("clientPk") Long clientPk);
}
