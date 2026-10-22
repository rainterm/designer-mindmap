package com.designer.admin.system.service.dict;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.system.model.dto.dict.DictTypeQueryDTO;
import com.designer.admin.system.model.dto.dict.DictTypeSaveDTO;
import com.designer.admin.system.model.entity.dict.SysDictData;
import com.designer.admin.system.model.entity.dict.SysDictType;
import com.designer.admin.system.model.vo.dict.DictDataExcelVO;
import com.designer.admin.system.model.vo.dict.DictTypeExcelVO;

import java.util.List;

/**
 * 字典服务（类型 + 数据）
 *
 * @author 雨夜终端
 */
public interface SysDictService {

    /**
     * 字典类型分页。
     *
     * @param query 分页与名称 / 类型编码 / 状态筛选条件
     * @return 字典类型分页结果
     */
    IPage<SysDictType> listTypePage(DictTypeQueryDTO query);

    /**
     * 全部字典类型（下拉选项用）。
     *
     * @return 字典类型列表
     */
    List<SysDictType> listTypeOptions();

    /**
     * 新增字典类型，并校验类型编码唯一。
     *
     * @param dto 保存参数
     */
    void addType(DictTypeSaveDTO dto);

    /**
     * 修改字典类型。
     *
     * @param dto 保存参数
     */
    void updateType(DictTypeSaveDTO dto);

    /**
     * 批量删除字典类型，同时清理其下的字典数据。
     *
     * @param ids 字典类型 ID 列表
     */
    void removeType(List<Long> ids);

    /**
     * 查询指定字典类型下的字典数据分页。
     *
     * @param query    分页与筛选条件
     * @param dictType 字典类型编码
     * @return 字典数据分页结果
     */
    IPage<SysDictData> listDataPage(DictTypeQueryDTO query, String dictType);

    /**
     * 按类型编码查询启用中的字典数据，按排序升序。
     *
     * @param dictType 字典类型编码
     * @return 字典数据列表
     */
    List<SysDictData> listDataByType(String dictType);

    /**
     * 批量按类型编码查询字典数据，供前端一次性拉取多个字典。
     *
     * @param types 字典类型编码列表
     * @return 字典数据列表
     */
    List<SysDictData> listDataBatch(List<String> types);

    /**
     * 新增字典数据。
     *
     * @param data 字典数据
     */
    void addData(SysDictData data);

    /**
     * 修改字典数据。
     *
     * @param data 字典数据
     */
    void updateData(SysDictData data);

    /**
     * 批量删除字典数据。
     *
     * @param ids 字典数据 ID 列表
     */
    void removeData(List<Long> ids);

    /**
     * 导出字典类型数据行（Excel）。
     *
     * @param query 筛选条件
     * @return 可写入 Excel 的数据行
     */
    List<DictTypeExcelVO> exportTypeRows(DictTypeQueryDTO query);

    /**
     * 导出指定类型的字典数据行（Excel）。
     *
     * @param dictType 字典类型编码；为空导出全部
     * @return 可写入 Excel 的数据行
     */
    List<DictDataExcelVO> exportDataRows(String dictType);

    /**
     * 导入字典类型，跳过空行与已存在的类型编码。
     *
     * @param rows Excel 解析出的数据行
     * @return 成功导入条数
     */
    int importTypes(List<DictTypeExcelVO> rows);

    /**
     * 导入字典数据，跳过空行与重复值；类型编码不存在时中止并提示行号。
     *
     * @param rows Excel 解析出的数据行
     * @return 成功导入条数
     */
    int importData(List<DictDataExcelVO> rows);
}
