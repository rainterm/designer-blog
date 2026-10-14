package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.designer.admin.common.constant.SystemConstants;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.TreeUtils;
import com.designer.admin.system.mapper.SysDeptMapper;
import com.designer.admin.system.mapper.SysUserMapper;
import com.designer.admin.system.model.dto.DeptSaveDTO;
import com.designer.admin.system.model.entity.SysDept;
import com.designer.admin.system.model.entity.SysUser;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.List;

/**
 * 部门树 CRUD
 */
@Component
public class SysDeptService {

    @Db
    private SysDeptMapper deptMapper;
    @Db
    private SysUserMapper userMapper;

    public List<SysDept> tree(String deptName, Integer status) {
        List<SysDept> list = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .like(notBlank(deptName), SysDept::getDeptName, deptName)
                .eq(status != null, SysDept::getStatus, status)
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getSort));
        return TreeUtils.buildTree(list, 0L);
    }

    /** 启用部门树（下拉） */
    public List<SysDept> options() {
        List<SysDept> list = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, SystemConstants.STATUS_ENABLE)
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getSort));
        return TreeUtils.buildTree(list, 0L);
    }

    public SysDept getById(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BizException("部门不存在");
        }
        return dept;
    }

    @Transaction
    public void save(DeptSaveDTO dto) {
        SysDept dept = toEntity(dto);
        Long parentId = dept.getParentId() == null ? 0L : dept.getParentId();
        dept.setParentId(parentId);
        dept.setAncestors(buildAncestors(parentId));
        if (dept.getStatus() == null) {
            dept.setStatus(SystemConstants.STATUS_ENABLE);
        }
        if (dept.getSort() == null) {
            dept.setSort(0);
        }
        deptMapper.insert(dept);
    }

    @Transaction
    public void update(DeptSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("部门ID不能为空");
        }
        SysDept exist = getById(dto.getId());
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        if (dto.getId().equals(parentId)) {
            throw new BizException("上级部门不能是自己");
        }
        // 不能把父节点设为自己的子孙
        if (parentId != 0L) {
            SysDept parent = getById(parentId);
            String ancestors = parent.getAncestors() == null ? "" : parent.getAncestors();
            if (("," + ancestors + ",").contains("," + dto.getId() + ",")) {
                throw new BizException("上级部门不能是自己的下级");
            }
        }

        String oldAncestors = exist.getAncestors();
        String newAncestors = buildAncestors(parentId);

        SysDept dept = toEntity(dto);
        dept.setParentId(parentId);
        dept.setAncestors(newAncestors);
        deptMapper.updateById(dept);

        // 同步更新子孙 ancestors
        if (!newAncestors.equals(oldAncestors)) {
            List<SysDept> children = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                    .likeRight(SysDept::getAncestors, oldAncestors + "," + exist.getId()));
            for (SysDept child : children) {
                String childAncestors = child.getAncestors();
                if (childAncestors != null && childAncestors.startsWith(oldAncestors)) {
                    child.setAncestors(newAncestors + childAncestors.substring(oldAncestors.length()));
                    deptMapper.updateById(child);
                }
            }
        }
    }

    @Transaction
    public void remove(Long id) {
        Long childCnt = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (childCnt != null && childCnt > 0) {
            throw new BizException("存在下级部门，不允许删除");
        }
        Long userCnt = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (userCnt != null && userCnt > 0) {
            throw new BizException("部门下存在用户，不允许删除");
        }
        deptMapper.deleteById(id);
    }

    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        SysDept parent = getById(parentId);
        String pa = parent.getAncestors();
        if (pa == null || pa.isBlank()) {
            return "0," + parentId;
        }
        return pa + "," + parentId;
    }

    private SysDept toEntity(DeptSaveDTO dto) {
        SysDept dept = new SysDept();
        dept.setId(dto.getId());
        dept.setParentId(dto.getParentId());
        dept.setDeptName(dto.getDeptName());
        dept.setSort(dto.getSort());
        dept.setStatus(dto.getStatus());
        return dept;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
