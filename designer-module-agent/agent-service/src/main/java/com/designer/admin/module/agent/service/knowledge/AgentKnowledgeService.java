package com.designer.admin.module.agent.service.knowledge;

import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeChunkUpdateDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeDocQueryDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeDocSaveDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeQueryDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeRetrieveTestDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeSaveDTO;
import com.designer.admin.module.agent.model.vo.knowledge.KnowledgeChunkVO;
import com.designer.admin.module.agent.model.vo.knowledge.KnowledgeDocVO;
import com.designer.admin.module.agent.model.vo.knowledge.KnowledgeRetrieveTestVO;
import com.designer.admin.module.agent.model.vo.knowledge.KnowledgeVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import org.noear.solon.web.sse.SseEmitter;

/**
 * 知识库管理
 *
 * @author 雨夜终端
 */
public interface AgentKnowledgeService {

    /**
     * 知识库分页。
     *
     * @param query 分页与名称 / 状态筛选条件
     * @return 知识库分页结果
     */
    IPage<KnowledgeVO> page(KnowledgeQueryDTO query);

    /**
     * 已启用知识库列表（下拉等场景）。
     *
     * @return 已启用知识库列表
     */
    List<KnowledgeVO> listEnabled();

    /**
     * 按主键查询知识库详情。
     *
     * @param id 知识库 ID
     * @return 知识库详情
     */
    KnowledgeVO getById(Long id);

    /**
     * 知识库检索测试：按查询向量召回分块，便于调 TopK / 阈值。
     *
     * @param knowledgeId 知识库 ID
     * @param dto         检索测试参数（查询文本、TopK、阈值等）
     * @return 检索测试结果
     */
    KnowledgeRetrieveTestVO retrieveTest(Long knowledgeId, KnowledgeRetrieveTestDTO dto);

    /**
     * 新增知识库。
     *
     * @param dto 保存参数
     * @return 新建后的知识库
     */
    KnowledgeVO save(KnowledgeSaveDTO dto);

    /**
     * 修改知识库。
     *
     * @param dto 保存参数
     * @return 更新后的知识库
     */
    KnowledgeVO update(KnowledgeSaveDTO dto);

    /**
     * 批量删除知识库及其文档与向量。
     *
     * @param ids 知识库 ID 列表
     */
    void remove(List<Long> ids);

    /**
     * 知识库文档分页。
     *
     * @param query 分页与知识库 / 状态 / 关键字筛选条件
     * @return 文档分页结果
     */
    IPage<KnowledgeDocVO> pageDocs(KnowledgeDocQueryDTO query);

    /**
     * 按主键查询文档详情。
     *
     * @param id 文档 ID
     * @return 文档详情
     */
    KnowledgeDocVO getDoc(Long id);

    /**
     * 新增文档（入库后异步/同步索引）。
     *
     * @param dto 保存参数
     * @return 新建后的文档
     */
    KnowledgeDocVO saveDoc(KnowledgeDocSaveDTO dto);

    /**
     * 修改文档元数据或内容。
     *
     * @param dto 保存参数
     * @return 更新后的文档
     */
    KnowledgeDocVO updateDoc(KnowledgeDocSaveDTO dto);

    /**
     * 批量删除文档及其向量。
     *
     * @param ids 文档 ID 列表
     */
    void removeDocs(List<Long> ids);

    /**
     * 列出文档下全部分块。
     *
     * @param docId 文档 ID
     * @return 分块列表
     */
    List<KnowledgeChunkVO> listChunks(Long docId);

    /**
     * 更新指定分块内容并重写向量。
     *
     * @param docId      文档 ID
     * @param chunkIndex 分块下标
     * @param dto        更新内容
     * @return 更新后的分块
     */
    KnowledgeChunkVO updateChunk(Long docId, Integer chunkIndex, KnowledgeChunkUpdateDTO dto);

    /**
     * 同步重建文档索引。
     *
     * @param id 文档 ID
     * @return 索引完成后的文档
     */
    KnowledgeDocVO reindex(Long id);

    /**
     * SSE 重建索引：event = progress / done / error。
     *
     * @param id 文档 ID
     * @return SSE 发射器
     */
    SseEmitter reindexStream(Long id);

}
