package com.designer.admin.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.designer.admin.common.core.PageResult;
import com.designer.admin.common.core.R;
import com.designer.admin.common.util.ExcelUtil;
import com.designer.admin.framework.log.BusinessType;
import com.designer.admin.framework.log.OperLog;
import com.designer.admin.system.model.dto.DictTypeQueryDTO;
import com.designer.admin.system.model.dto.DictTypeSaveDTO;
import com.designer.admin.system.model.entity.SysDictData;
import com.designer.admin.system.model.entity.SysDictType;
import com.designer.admin.system.model.vo.DictDataExcelVO;
import com.designer.admin.system.model.vo.DictTypeExcelVO;
import com.designer.admin.system.service.SysDictService;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Delete;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.annotation.Put;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

/**
 * 字典管理：类型与数据两组 CRUD + 启用下拉
 */
@Valid
@Controller
@Mapping("/admin-api/system/dict")
public class SysDictController {

    @Inject
    private SysDictService dictService;

    // ----------------------------- 字典类型 -----------------------------

    /**
     * 分页查询字典类型
     */
    @Get
    @Mapping("/type/page")
    @SaCheckPermission("system:dict:list")
    public R<PageResult<SysDictType>> typePage(DictTypeQueryDTO query) {
        return R.ok(PageResult.of(dictService.typePage(query)));
    }

    /**
     * 字典类型下拉（仅启用）
     */
    @Get
    @Mapping("/type/options")
    public R<List<SysDictType>> typeOptions() {
        return R.ok(dictService.typeOptions());
    }

    /**
     * 新增字典类型；会校验 dictType 唯一性
     */
    @Post
    @Mapping("/type")
    @OperLog(title = "字典管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:dict:add")
    public R<Void> typeAdd(@Body @Validated DictTypeSaveDTO dto) {
        dictService.typeSave(dto);
        return R.ok();
    }

    /**
     * 更新字典类型
     */
    @Put
    @Mapping("/type")
    @OperLog(title = "字典管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:dict:edit")
    public R<Void> typeUpdate(@Body @Validated DictTypeSaveDTO dto) {
        dictService.typeUpdate(dto);
        return R.ok();
    }

    /**
     * 批量删除字典类型（级联删除其下字典数据）
     */
    @Delete
    @Mapping("/type")
    @OperLog(title = "字典管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:dict:remove")
    public R<Void> typeRemove(List<Long> ids) {
        dictService.typeRemove(ids);
        return R.ok();
    }

    @Get
    @Mapping("/type/export")
    @OperLog(title = "字典管理", businessType = BusinessType.EXPORT, saveResult = false)
    @SaCheckPermission("system:dict:export")
    public void typeExport(Context ctx, DictTypeQueryDTO query) throws Exception {
        ExcelUtil.write(ctx, "字典类型.xlsx", "字典类型", DictTypeExcelVO.class, dictService.exportTypeRows(query));
    }

    @Get
    @Mapping("/type/importTemplate")
    @SaCheckPermission("system:dict:import")
    public void typeImportTemplate(Context ctx) throws Exception {
        ExcelUtil.writeTemplate(ctx, "字典类型导入模板.xlsx", "字典类型", DictTypeExcelVO.class);
    }

    @Post
    @Mapping("/type/import")
    @OperLog(title = "字典管理", businessType = BusinessType.IMPORT, saveParam = false, saveResult = false)
    @SaCheckPermission("system:dict:import")
    public R<Map<String, Object>> typeImport(UploadedFile file) {
        int count = dictService.importTypes(ExcelUtil.read(file, DictTypeExcelVO.class));
        return R.ok(Map.of("success", count));
    }

    // ----------------------------- 字典数据 -----------------------------

    /**
     * 分页查询字典数据
     */
    @Get
    @Mapping("/data/page")
    @SaCheckPermission("system:dict:list")
    public R<PageResult<SysDictData>> dataPage(DictTypeQueryDTO query, String dictType) {
        return R.ok(PageResult.of(dictService.dataPage(query, dictType)));
    }

    @Get
    @Mapping("/data/export")
    @OperLog(title = "字典管理", businessType = BusinessType.EXPORT, saveResult = false)
    @SaCheckPermission("system:dict:export")
    public void dataExport(Context ctx, String dictType) throws Exception {
        ExcelUtil.write(ctx, "字典数据.xlsx", "字典数据", DictDataExcelVO.class, dictService.exportDataRows(dictType));
    }

    @Get
    @Mapping("/data/importTemplate")
    @SaCheckPermission("system:dict:import")
    public void dataImportTemplate(Context ctx) throws Exception {
        ExcelUtil.writeTemplate(ctx, "字典数据导入模板.xlsx", "字典数据", DictDataExcelVO.class);
    }

    @Post
    @Mapping("/data/import")
    @OperLog(title = "字典管理", businessType = BusinessType.IMPORT, saveParam = false, saveResult = false)
    @SaCheckPermission("system:dict:import")
    public R<Map<String, Object>> dataImport(UploadedFile file) {
        int count = dictService.importData(ExcelUtil.read(file, DictDataExcelVO.class));
        return R.ok(Map.of("success", count));
    }

    /**
     * 批量取多个字典类型的数据
     */
    @Get
    @Mapping("/data/batch")
    public R<List<SysDictData>> dataBatch(List<String> types) {
        return R.ok(dictService.dataBatch(types));
    }

    /**
     * 按字典类型取启用数据（前端下拉常用，仅需登录）
     */
    @Get
    @Mapping("/data/{dictType}")
    public R<List<SysDictData>> dataByType(String dictType) {
        return R.ok(dictService.dataByType(dictType));
    }

    /**
     * 新增字典数据
     */
    @Post
    @Mapping("/data")
    @OperLog(title = "字典管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:dict:add")
    public R<Void> dataAdd(@Body @Validated SysDictData data) {
        dictService.dataSave(data);
        return R.ok();
    }

    /**
     * 更新字典数据
     */
    @Put
    @Mapping("/data")
    @OperLog(title = "字典管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:dict:edit")
    public R<Void> dataUpdate(@Body @Validated SysDictData data) {
        dictService.dataUpdate(data);
        return R.ok();
    }

    /**
     * 批量删除字典数据
     */
    @Delete
    @Mapping("/data")
    @OperLog(title = "字典管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:dict:remove")
    public R<Void> dataRemove(List<Long> ids) {
        dictService.dataRemove(ids);
        return R.ok();
    }
}