package com.designer.admin.system.service.dict.impl;

import com.designer.admin.common.constant.SystemConsts;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.framework.dict.DictLookup;
import com.designer.admin.system.constant.SystemCacheConsts;
import com.designer.admin.system.mapper.dict.SysDictDataMapper;
import com.designer.admin.system.mapper.dict.SysDictTypeMapper;
import com.designer.admin.system.model.dto.dict.DictTypeQueryDTO;
import com.designer.admin.system.model.dto.dict.DictTypeSaveDTO;
import com.designer.admin.system.model.entity.dict.SysDictData;
import com.designer.admin.system.model.entity.dict.SysDictType;
import com.designer.admin.system.model.vo.dict.DictDataExcelVO;
import com.designer.admin.system.model.vo.dict.DictTypeExcelVO;
import com.designer.admin.system.service.dict.SysDictService;
import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Cache;
import org.noear.solon.data.annotation.CacheRemove;
import org.noear.solon.data.annotation.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典服务（类型 + 数据）
 *
 * @author 雨夜终端
 */
@Component
public class SysDictServiceImpl implements SysDictService, DictLookup {

    @Db
    private SysDictTypeMapper dictTypeMapper;
    @Db
    private SysDictDataMapper dictDataMapper;

    // ----------------------------- 字典类型 -----------------------------

    @Override
    public IPage<SysDictType> listTypePage(DictTypeQueryDTO query) {
        LambdaQueryWrapper<SysDictType> w = new LambdaQueryWrapper<SysDictType>()
                .like(notBlank(query.getDictName()), SysDictType::getDictName, query.getDictName())
                .like(notBlank(query.getDictType()), SysDictType::getDictType, query.getDictType())
                .eq(query.getStatus() != null, SysDictType::getStatus, query.getStatus())
                .orderByDesc(SysDictType::getId);
        return dictTypeMapper.selectPage(query.toPage(), w);
    }

    @Override
    public List<SysDictType> listTypeOptions() {
        return dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getStatus, 1)
                .orderByAsc(SysDictType::getId));
    }

    @Override
    @Transaction
    @CacheRemove(tags = SystemCacheConsts.DICT_TYPE_TAG)
    public void addType(DictTypeSaveDTO dto) {
        Long dup = dictTypeMapper.countByDictType(dto.getDictType(), null);
        if (dup != null && dup > 0) {
            throw new BizException("字典类型标识已存在");
        }
        SysDictType type = toTypeEntity(dto);
        if (type.getStatus() == null) {
            type.setStatus(SystemConsts.STATUS_ENABLE);
        }
        dictTypeMapper.insert(type);
    }

    @Override
    @Transaction
    @CacheRemove(tags = SystemCacheConsts.DICT_ALL_TAGS)
    public void updateType(DictTypeSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("字典类型ID不能为空");
        }
        Long dup = dictTypeMapper.countByDictType(dto.getDictType(), dto.getId());
        if (dup != null && dup > 0) {
            throw new BizException("字典类型标识已存在");
        }
        dictTypeMapper.updateById(toTypeEntity(dto));
    }

    @Override
    @Transaction
    @CacheRemove(tags = SystemCacheConsts.DICT_ALL_TAGS)
    public void removeType(List<Long> ids) {
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

    @Override
    public IPage<SysDictData> listDataPage(DictTypeQueryDTO query, String dictType) {
        LambdaQueryWrapper<SysDictData> w = new LambdaQueryWrapper<SysDictData>()
                .eq(notBlank(dictType), SysDictData::getDictType, dictType)
                .orderByAsc(SysDictData::getDictType).orderByAsc(SysDictData::getSort);
        return dictDataMapper.selectPage(query.toPage(), w);
    }

    @Override
    public List<SysDictData> listDataByType(String dictType) {
        return dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSort));
    }

    /**
     * 按类型取「键值 → 标签」映射，供业务模块把库里的码翻成文案。
     *
     * <p>缓存粒度是「一个字典类型一个键」：业务侧一次请求里取几个类型就命中几个键，
     * 不会因为「组合不同」把缓存打散；失效沿用字典数据的 tag，改完字典立刻生效。</p>
     */
    @Override
    @Cache(key = SystemCacheConsts.DICT_LABEL_MAP_KEY_TMPL,
            tags = SystemCacheConsts.DICT_DATA_TAG,
            seconds = SystemCacheConsts.CACHE_SECONDS)
    public Map<String, String> labelMap(String dictType) {
        List<SysDictData> rows = listDataByType(dictType);
        Map<String, String> labels = new LinkedHashMap<>(Math.max(8, rows.size() * 2));
        for (SysDictData row : rows) {
            labels.putIfAbsent(row.getDictValue(), row.getDictLabel());
        }
        return labels;
    }

    @Override
    public List<SysDictData> listDataBatch(List<String> types) {
        if (types == null || types.isEmpty()) {
            return List.of();
        }
        return dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .in(SysDictData::getDictType, types)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSort));
    }

    @Override
    @CacheRemove(tags = SystemCacheConsts.DICT_DATA_TAG)
    public void addData(SysDictData data) {
        if (data.getStatus() == null) {
            data.setStatus(SystemConsts.STATUS_ENABLE);
        }
        dictDataMapper.insert(data);
    }

    @Override
    @CacheRemove(tags = SystemCacheConsts.DICT_DATA_TAG)
    public void updateData(SysDictData data) {
        if (data.getId() == null) {
            throw new BizException("字典数据ID不能为空");
        }
        dictDataMapper.updateById(data);
    }

    @Override
    @Transaction
    @CacheRemove(tags = SystemCacheConsts.DICT_DATA_TAG)
    public void removeData(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            dictDataMapper.deleteByIds(ids);
        }
    }

    @Override
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

    @Override
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

    @Override
    @Transaction
    @CacheRemove(tags = SystemCacheConsts.DICT_TYPE_TAG)
    public int importTypes(List<DictTypeExcelVO> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new BizException("导入数据为空");
        }
        int success = 0;
        for (DictTypeExcelVO row : rows) {
            if (row == null || !notBlank(row.getDictType()) || !notBlank(row.getDictName())) {
                continue;
            }
            Long dup = dictTypeMapper.countByDictType(row.getDictType().trim(), null);
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

    @Override
    @Transaction
    @CacheRemove(tags = SystemCacheConsts.DICT_DATA_TAG)
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
