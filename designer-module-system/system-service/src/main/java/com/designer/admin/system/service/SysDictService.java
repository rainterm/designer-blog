package com.designer.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.system.model.dto.DictTypeQueryDTO;
import com.designer.admin.system.model.dto.DictTypeSaveDTO;
import com.designer.admin.system.model.entity.SysDictData;
import com.designer.admin.system.model.entity.SysDictType;
import com.designer.admin.system.model.vo.DictDataExcelVO;
import com.designer.admin.system.model.vo.DictTypeExcelVO;
import com.designer.admin.system.mapper.SysDictDataMapper;
import com.designer.admin.system.mapper.SysDictTypeMapper;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * 字典服务（类型 + 数据）
 */
@Component
public class SysDictService {

    @Db
    private SysDictTypeMapper dictTypeMapper;
    @Db
    private SysDictDataMapper dictDataMapper;

    // ----------------------------- 字典类型 -----------------------------

    public IPage<SysDictType> typePage(DictTypeQueryDTO query) {
        LambdaQueryWrapper<SysDictType> w = new LambdaQueryWrapper<SysDictType>()
                .like(notBlank(query.getDictName()), SysDictType::getDictName, query.getDictName())
                .like(notBlank(query.getDictType()), SysDictType::getDictType, query.getDictType())
                .eq(query.getStatus() != null, SysDictType::getStatus, query.getStatus())
                .orderByDesc(SysDictType::getId);
        return dictTypeMapper.selectPage(query.toPage(), w);
    }

    public List<SysDictType> typeOptions() {
        return dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getStatus, 1)
                .orderByAsc(SysDictType::getId));
    }

    /**
     * 新增字典类型：校验 dictType 唯一；状态为空时默认启用。
     */
    @Transaction
    public void typeSave(DictTypeSaveDTO dto) {
        Long dup = dictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dto.getDictType()));
        if (dup != null && dup > 0) {
            throw new BizException("字典类型标识已存在");
        }
        SysDictType type = toTypeEntity(dto);
        if (type.getStatus() == null) {
            type.setStatus(1);
        }
        dictTypeMapper.insert(type);
    }

    @Transaction
    public void typeUpdate(DictTypeSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("字典类型ID不能为空");
        }
        Long dup = dictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dto.getDictType())
                .ne(SysDictType::getId, dto.getId()));
        if (dup != null && dup > 0) {
            throw new BizException("字典类型标识已存在");
        }
        dictTypeMapper.updateById(toTypeEntity(dto));
    }

    /**
     * 批量删除字典类型：级联删除其下所有字典数据（按 dictType 关联删除）。
     */
    @Transaction
    public void typeRemove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            SysDictType type = dictTypeMapper.selectById(id);
            if (type != null) {
                dictDataMapper.delete(new LambdaQueryWrapper<SysDictData>()
                        .eq(SysDictData::getDictType, type.getDictType()));
            }
        }
        dictTypeMapper.deleteByIds(ids);
    }

    // ----------------------------- 字典数据 -----------------------------

    public IPage<SysDictData> dataPage(DictTypeQueryDTO query, String dictType) {
        LambdaQueryWrapper<SysDictData> w = new LambdaQueryWrapper<SysDictData>()
                .eq(notBlank(dictType), SysDictData::getDictType, dictType)
                .orderByAsc(SysDictData::getDictType).orderByAsc(SysDictData::getSort);
        return dictDataMapper.selectPage(query.toPage(), w);
    }

    public List<SysDictData> dataByType(String dictType) {
        return dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSort));
    }

    public List<SysDictData> dataBatch(List<String> types) {
        if (types == null || types.isEmpty()) {
            return List.of();
        }
        return dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .in(SysDictData::getDictType, types)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSort));
    }

    public void dataSave(SysDictData data) {
        if (data.getStatus() == null) {
            data.setStatus(1);
        }
        dictDataMapper.insert(data);
    }

    public void dataUpdate(SysDictData data) {
        if (data.getId() == null) {
            throw new BizException("字典数据ID不能为空");
        }
        dictDataMapper.updateById(data);
    }

    @Transaction
    public void dataRemove(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            dictDataMapper.deleteByIds(ids);
        }
    }

    public List<DictTypeExcelVO> exportTypeRows(DictTypeQueryDTO query) {
        LambdaQueryWrapper<SysDictType> w = new LambdaQueryWrapper<SysDictType>()
                .like(notBlank(query.getDictName()), SysDictType::getDictName, query.getDictName())
                .like(notBlank(query.getDictType()), SysDictType::getDictType, query.getDictType())
                .eq(query.getStatus() != null, SysDictType::getStatus, query.getStatus())
                .orderByDesc(SysDictType::getId)
                .last("LIMIT 5000");
        List<SysDictType> list = dictTypeMapper.selectList(w);
        List<DictTypeExcelVO> rows = new ArrayList<>(list.size());
        for (SysDictType type : list) {
            DictTypeExcelVO row = new DictTypeExcelVO();
            row.setDictName(type.getDictName());
            row.setDictType(type.getDictType());
            row.setStatus(type.getStatus());
            row.setRemark(type.getRemark());
            rows.add(row);
        }
        return rows;
    }

    public List<DictDataExcelVO> exportDataRows(String dictType) {
        LambdaQueryWrapper<SysDictData> w = new LambdaQueryWrapper<SysDictData>()
                .eq(notBlank(dictType), SysDictData::getDictType, dictType)
                .orderByAsc(SysDictData::getDictType).orderByAsc(SysDictData::getSort)
                .last("LIMIT 10000");
        List<SysDictData> list = dictDataMapper.selectList(w);
        List<DictDataExcelVO> rows = new ArrayList<>(list.size());
        for (SysDictData data : list) {
            DictDataExcelVO row = new DictDataExcelVO();
            row.setDictType(data.getDictType());
            row.setDictLabel(data.getDictLabel());
            row.setDictValue(data.getDictValue());
            row.setSort(data.getSort());
            row.setIsDefault(data.getIsDefault());
            row.setStatus(data.getStatus());
            row.setRemark(data.getRemark());
            rows.add(row);
        }
        return rows;
    }

    /** 导入字典类型：dictType 已存在则跳过 */
    @Transaction
    public int importTypes(List<DictTypeExcelVO> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new BizException("导入数据为空");
        }
        int success = 0;
        for (DictTypeExcelVO row : rows) {
            if (row == null || !notBlank(row.getDictType()) || !notBlank(row.getDictName())) {
                continue;
            }
            Long dup = dictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                    .eq(SysDictType::getDictType, row.getDictType().trim()));
            if (dup != null && dup > 0) {
                continue;
            }
            SysDictType type = new SysDictType();
            type.setDictName(row.getDictName().trim());
            type.setDictType(row.getDictType().trim());
            type.setStatus(row.getStatus() == null ? 1 : row.getStatus());
            type.setRemark(row.getRemark() == null ? "" : row.getRemark());
            dictTypeMapper.insert(type);
            success++;
        }
        if (success == 0) {
            throw new BizException("没有可导入的新字典类型");
        }
        return success;
    }

    /** 导入字典数据：同类型下 dictValue 已存在则跳过；类型不存在则报错 */
    @Transaction
    public int importData(List<DictDataExcelVO> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new BizException("导入数据为空");
        }
        int success = 0;
        int line = 1;
        for (DictDataExcelVO row : rows) {
            line++;
            if (row == null || !notBlank(row.getDictType()) || !notBlank(row.getDictLabel())
                    || !notBlank(row.getDictValue())) {
                continue;
            }
            String dictType = row.getDictType().trim();
            Long typeExists = dictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                    .eq(SysDictType::getDictType, dictType));
            if (typeExists == null || typeExists == 0) {
                throw new BizException("第 " + line + " 行字典类型不存在：" + dictType);
            }
            Long dup = dictDataMapper.selectCount(new LambdaQueryWrapper<SysDictData>()
                    .eq(SysDictData::getDictType, dictType)
                    .eq(SysDictData::getDictValue, row.getDictValue().trim()));
            if (dup != null && dup > 0) {
                continue;
            }
            SysDictData data = new SysDictData();
            data.setDictType(dictType);
            data.setDictLabel(row.getDictLabel().trim());
            data.setDictValue(row.getDictValue().trim());
            data.setSort(row.getSort() == null ? 0 : row.getSort());
            data.setIsDefault(row.getIsDefault() == null ? 0 : row.getIsDefault());
            data.setStatus(row.getStatus() == null ? 1 : row.getStatus());
            data.setRemark(row.getRemark() == null ? "" : row.getRemark());
            dictDataMapper.insert(data);
            success++;
        }
        if (success == 0) {
            throw new BizException("没有可导入的新字典数据");
        }
        return success;
    }

    private SysDictType toTypeEntity(DictTypeSaveDTO dto) {
        SysDictType type = new SysDictType();
        type.setId(dto.getId());
        type.setDictName(dto.getDictName());
        type.setDictType(dto.getDictType());
        type.setStatus(dto.getStatus());
        type.setRemark(dto.getRemark());
        return type;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
