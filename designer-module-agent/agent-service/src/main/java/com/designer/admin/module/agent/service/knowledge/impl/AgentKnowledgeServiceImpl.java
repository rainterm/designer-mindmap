package com.designer.admin.module.agent.service.knowledge.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ibatis.solon.annotation.Db;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.data.annotation.Transaction;
import org.noear.solon.web.sse.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.designer.admin.common.constant.SystemConsts;
import com.designer.admin.common.exception.BizException;
import com.designer.admin.common.util.JsonUtil;
import com.designer.admin.module.agent.constant.knowledge.KnowledgeDocStatus;
import com.designer.admin.module.agent.constant.knowledge.KnowledgeSourceType;
import com.designer.admin.module.agent.constant.session.AgentSseEvent;
import com.designer.admin.module.agent.util.AgentAsyncExecutor;
import com.designer.admin.module.agent.mapper.knowledge.AgentKnowledgeDocMapper;
import com.designer.admin.module.agent.mapper.knowledge.AgentKnowledgeMapper;
import com.designer.admin.module.agent.mapper.provider.AgentProviderMapper;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeChunkUpdateDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeDocQueryDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeDocSaveDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeQueryDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeRetrieveTestDTO;
import com.designer.admin.module.agent.model.dto.knowledge.KnowledgeSaveDTO;
import com.designer.admin.module.agent.model.entity.knowledge.AgentKnowledge;
import com.designer.admin.module.agent.model.entity.knowledge.AgentKnowledgeDoc;
import com.designer.admin.module.agent.model.entity.provider.AgentProvider;
import com.designer.admin.module.agent.model.vo.knowledge.KnowledgeChunkVO;
import com.designer.admin.module.agent.model.vo.knowledge.KnowledgeDocVO;
import com.designer.admin.module.agent.model.vo.knowledge.KnowledgeRetrieveTestVO;
import com.designer.admin.module.agent.model.vo.knowledge.KnowledgeVO;
import com.designer.admin.module.agent.service.knowledge.AgentEmbeddingClient;
import com.designer.admin.module.agent.service.knowledge.AgentKnowledgeIndexService;
import com.designer.admin.module.agent.service.knowledge.AgentKnowledgeRetrieveService;
import com.designer.admin.module.agent.service.knowledge.AgentKnowledgeService;
import com.designer.admin.module.agent.service.knowledge.AgentMilvusStore;
import com.designer.admin.module.agent.service.knowledge.RagRetrieveOptions;
import com.designer.admin.module.agent.service.knowledge.RagRetrieveResult;
import com.designer.admin.module.agent.constant.AgentConsts;
import com.designer.admin.module.agent.service.knowledge.AgentTextChunker;
import com.designer.admin.module.agent.util.AgentEntityUtil;
import com.designer.admin.module.agent.util.AgentJsonUtil;
import com.designer.admin.module.agent.util.AgentSseUtil;
import org.noear.solon.Utils;

/**
 * 知识库管理
 *
 * @author 雨夜终端
 */
@Component
public class AgentKnowledgeServiceImpl implements AgentKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(AgentKnowledgeServiceImpl.class);

    @Db
    private AgentKnowledgeMapper knowledgeMapper;
    @Db
    private AgentKnowledgeDocMapper docMapper;
    @Db
    private AgentProviderMapper providerMapper;
    @Inject
    private AgentKnowledgeIndexService indexService;
    @Inject
    private AgentAsyncExecutor asyncExecutor;
    @Inject
    private AgentKnowledgeRetrieveService retrieveService;

    @Override
    public IPage<KnowledgeVO> page(KnowledgeQueryDTO query) {
        IPage<AgentKnowledge> page = knowledgeMapper.selectPage(query.toPage(), new LambdaQueryWrapper<AgentKnowledge>()
                .and(Utils.isNotBlank(query.getName()), w -> w
                        .like(AgentKnowledge::getName, query.getName())
                        .or()
                        .like(AgentKnowledge::getCode, query.getName()))
                .eq(query.getStatus() != null, AgentKnowledge::getStatus, query.getStatus())
                .orderByDesc(AgentKnowledge::getId));
        return page.convert(this::toVo);
    }

    @Override
    public List<KnowledgeVO> listEnabled() {
        return knowledgeMapper.selectList(new LambdaQueryWrapper<AgentKnowledge>()
                        .eq(AgentKnowledge::getStatus, SystemConsts.STATUS_ENABLE)
                        .orderByDesc(AgentKnowledge::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public KnowledgeVO getById(Long id) {
        return toVo(requireKb(id));
    }

    @Override
    public KnowledgeRetrieveTestVO retrieveTest(Long knowledgeId, KnowledgeRetrieveTestDTO dto) {
        AgentKnowledge kb = requireKb(knowledgeId);
        if (kb.getEmbeddingProviderId() == null
                || kb.getEmbeddingModel() == null
                || kb.getEmbeddingModel().isBlank()) {
            throw new BizException("请先配置知识库 Embedding 供应商与模型");
        }
        String query = dto == null || dto.getQuery() == null ? "" : dto.getQuery().trim();
        if (query.isBlank()) {
            throw new BizException("请输入测试查询");
        }
        int topK = dto.getTopK() == null || dto.getTopK() < 1 ? AgentConsts.DEFAULT_TOP_K : Math.min(AgentConsts.MAX_TOP_K, dto.getTopK());
        double threshold = dto.getScoreThreshold() == null
                ? RagRetrieveOptions.DEFAULT_SCORE_THRESHOLD
                : dto.getScoreThreshold().doubleValue();
        if (threshold < 0) {
            threshold = 0;
        }
        if (threshold > 1) {
            threshold = 1;
        }
        Long rerankProviderId = dto.getRerankProviderId();
        String rerankModel = dto.getRerankModel() == null ? "" : dto.getRerankModel().trim();
        int recallTopK = dto.getRecallTopK() == null || dto.getRecallTopK() < 0
                ? 0 : Math.min(AgentConsts.MAX_TOP_K, dto.getRecallTopK());
        RagRetrieveOptions opts = new RagRetrieveOptions(
                kb.getEmbeddingProviderId(),
                kb.getEmbeddingModel(),
                rerankProviderId,
                rerankModel,
                topK,
                recallTopK,
                threshold);
        long t0 = System.currentTimeMillis();
        RagRetrieveResult result = retrieveService.retrieveDetailed(
                List.of(knowledgeId), query, 50_000, opts);
        KnowledgeRetrieveTestVO vo = new KnowledgeRetrieveTestVO();
        vo.setQuery(query);
        vo.setTopK(topK);
        vo.setRecallTopK(opts.resolveFetchK());
        vo.setScoreThreshold(threshold);
        vo.setRerankProviderId(rerankProviderId);
        vo.setRerankModel(rerankModel);
        vo.setRerankEnabled(opts.hasRerank());
        vo.setHitCount(result.hitCount());
        vo.setDurationMs(System.currentTimeMillis() - t0);
        List<KnowledgeRetrieveTestVO.Hit> hits = new ArrayList<>();
        for (RagRetrieveResult.Hit h : result.hits()) {
            KnowledgeRetrieveTestVO.Hit row = new KnowledgeRetrieveTestVO.Hit();
            row.setKbName(h.kbName());
            row.setKnowledgeId(h.knowledgeId());
            row.setDocId(h.docId());
            row.setDocTitle(h.docTitle());
            row.setChunkIndex(h.chunkIndex());
            row.setScore(h.score());
            row.setContent(h.content());
            hits.add(row);
        }
        vo.setHits(hits);
        return vo;
    }

    @Override
    public KnowledgeVO save(KnowledgeSaveDTO dto) {
        if (Utils.isBlank(dto.getCode())) {
            dto.setCode(nextKnowledgeCode());
        }
        validateKb(dto, null);
        AgentKnowledge entity = toKbEntity(dto);
        if (entity.getStatus() == null) {
            entity.setStatus(SystemConsts.STATUS_ENABLE);
        }
        knowledgeMapper.insert(entity);
        return toVo(entity);
    }

    @Override
    public KnowledgeVO update(KnowledgeSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("知识库ID不能为空");
        }
        AgentKnowledge existing = requireKb(dto.getId());
        if (Utils.isBlank(dto.getCode())) {
            dto.setCode(existing.getCode());
        }
        validateKb(dto, dto.getId());
        boolean embeddingChanged = !Objects.equals(existing.getEmbeddingProviderId(), dto.getEmbeddingProviderId())
                || !Objects.equals(
                existing.getEmbeddingModel() == null ? "" : existing.getEmbeddingModel(),
                dto.getEmbeddingModel() == null ? "" : dto.getEmbeddingModel().trim());
        String newStrategy = AgentTextChunker.normalizeStrategy(dto.getChunkStrategy());
        String oldStrategy = AgentTextChunker.normalizeStrategy(existing.getChunkStrategy());
        int newSize = dto.getChunkSize() == null ? AgentConsts.DEFAULT_CHUNK_SIZE : dto.getChunkSize();
        int oldSize = existing.getChunkSize() == null ? AgentConsts.DEFAULT_CHUNK_SIZE : existing.getChunkSize();
        int newOverlap = dto.getChunkOverlap() == null ? AgentConsts.DEFAULT_CHUNK_OVERLAP : dto.getChunkOverlap();
        int oldOverlap = existing.getChunkOverlap() == null ? AgentConsts.DEFAULT_CHUNK_OVERLAP : existing.getChunkOverlap();
        boolean chunkChanged = !newStrategy.equals(oldStrategy) || newSize != oldSize || newOverlap != oldOverlap;
        AgentKnowledge entity = toKbEntity(dto);
        if (embeddingChanged) {
            entity.setEmbeddingDim(0);
        } else {
            entity.setEmbeddingDim(existing.getEmbeddingDim());
        }
        knowledgeMapper.updateById(entity);
        if (embeddingChanged) {
            indexService.reindexAllReady(entity.getId());
        } else if (chunkChanged) {
            indexService.rechunkAllReady(entity.getId());
        }
        return toVo(knowledgeMapper.selectById(entity.getId()));
    }

    @Override
    @Transaction
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            indexService.dropKnowledgeVectors(id);
            docMapper.delete(new LambdaQueryWrapper<AgentKnowledgeDoc>()
                    .eq(AgentKnowledgeDoc::getKnowledgeId, id));
        }
        AgentEntityUtil.deleteBatchIds(knowledgeMapper, ids);
    }

    // ---------- docs ----------

    @Override
    public IPage<KnowledgeDocVO> pageDocs(KnowledgeDocQueryDTO query) {
        if (query.getKnowledgeId() == null) {
            throw new BizException("知识库ID不能为空");
        }
        return docMapper.selectPage(query.toPage(), new LambdaQueryWrapper<AgentKnowledgeDoc>()
                .eq(AgentKnowledgeDoc::getKnowledgeId, query.getKnowledgeId())
                .and(Utils.isNotBlank(query.getKeyword()), w -> w
                        .like(AgentKnowledgeDoc::getTitle, query.getKeyword())
                        .or()
                        .like(AgentKnowledgeDoc::getFileName, query.getKeyword())
                        .or()
                        .like(AgentKnowledgeDoc::getUrl, query.getKeyword()))
                .eq(Utils.isNotBlank(query.getStatus()), AgentKnowledgeDoc::getStatus, query.getStatus())
                .eq(Utils.isNotBlank(query.getSourceType()), AgentKnowledgeDoc::getSourceType, query.getSourceType())
                .orderByDesc(AgentKnowledgeDoc::getUpdateTime)
                .orderByDesc(AgentKnowledgeDoc::getId))
                .convert(this::toDocVo);
    }

    @Override
    public KnowledgeDocVO getDoc(Long id) {
        return toDocVo(requireDoc(id));
    }

    @Override
    public KnowledgeDocVO saveDoc(KnowledgeDocSaveDTO dto) {
        if (dto.getKnowledgeId() == null) {
            throw new BizException("知识库ID不能为空");
        }
        requireKb(dto.getKnowledgeId());
        String sourceType = Utils.isNotBlank(dto.getSourceType()) ? dto.getSourceType() : KnowledgeSourceType.TEXT.getCode();
        if (KnowledgeSourceType.UPLOAD.eq(sourceType) && Utils.isBlank(dto.getUrl())) {
            throw new BizException("请先上传原文件");
        }
        if (KnowledgeSourceType.URL.eq(sourceType) && Utils.isBlank(dto.getUrl())) {
            throw new BizException("请填写网页 URL");
        }
        String content = dto.getContent() == null ? "" : dto.getContent();
        int charCount = content.length() > 0 ? content.length()
                : (dto.getCharCount() == null ? 0 : dto.getCharCount());
        boolean canIndex = !content.isBlank();
        String status = Utils.isNotBlank(dto.getStatus())
                ? dto.getStatus()
                : KnowledgeDocStatus.PENDING.getCode();

        AgentKnowledgeDoc doc = new AgentKnowledgeDoc();
        doc.setKnowledgeId(dto.getKnowledgeId());
        doc.setTitle(firstNonBlank(dto.getTitle(), dto.getFileName(), AgentConsts.DOC_UNTITLED));
        doc.setSourceType(sourceType);
        doc.setFileName(dto.getFileName() == null ? "" : dto.getFileName());
        doc.setFileType(Utils.isNotBlank(dto.getFileType())
                ? dto.getFileType()
                : (KnowledgeSourceType.URL.eq(sourceType) ? KnowledgeSourceType.URL.getCode() : (KnowledgeSourceType.TEXT.eq(sourceType) ? KnowledgeSourceType.TEXT.getCode() : AgentConsts.FILE_TYPE_BIN)));
        doc.setFileSize(dto.getFileSize() == null ? 0L : dto.getFileSize());
        doc.setContent(content);
        doc.setUrl(dto.getUrl() == null ? "" : dto.getUrl().trim());
        doc.setStatus(status);
        doc.setChunkCount(0);
        doc.setIndexProgress(0);
        doc.setCharCount(charCount);
        doc.setErrorMsg(content.isBlank() && KnowledgeSourceType.UPLOAD.eq(sourceType)
                ? "原文件已保存；二进制文件暂未抽取正文，可编辑补充文本后重索引"
                : (canIndex ? "待索引" : ""));
        docMapper.insert(doc);
        return toDocVo(doc);
    }

    @Override
    public KnowledgeDocVO updateDoc(KnowledgeDocSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("文档ID不能为空");
        }
        AgentKnowledgeDoc doc = requireDoc(dto.getId());
        if (dto.getTitle() != null) {
            doc.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            doc.setContent(dto.getContent());
            doc.setCharCount(dto.getContent().length());
            if (!dto.getContent().isBlank() && (KnowledgeDocStatus.PENDING.eq(doc.getStatus()) || KnowledgeDocStatus.FAILED.eq(doc.getStatus()))) {
                doc.setStatus(KnowledgeDocStatus.READY.getCode());
                doc.setErrorMsg("");
            }
        }
        if (dto.getUrl() != null) {
            doc.setUrl(dto.getUrl());
        }
        if (dto.getFileName() != null) {
            doc.setFileName(dto.getFileName());
        }
        if (dto.getFileType() != null) {
            doc.setFileType(dto.getFileType());
        }
        if (dto.getFileSize() != null) {
            doc.setFileSize(dto.getFileSize());
        }
        if (dto.getStatus() != null) {
            doc.setStatus(dto.getStatus());
        }
        if (dto.getErrorMsg() != null) {
            doc.setErrorMsg(dto.getErrorMsg());
        }
        docMapper.updateById(doc);
        return toDocVo(doc);
    }

    @Override
    public void removeDocs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            AgentKnowledgeDoc doc = docMapper.selectById(id);
            if (doc != null) {
                indexService.deleteDocVectors(doc.getKnowledgeId(), doc.getId());
            }
        }
        AgentEntityUtil.deleteBatchIds(docMapper, ids);
    }

    @Override
    public List<KnowledgeChunkVO> listChunks(Long docId) {
        requireDoc(docId);
        List<AgentMilvusStore.ChunkRow> rows = indexService.listChunks(docId);
        List<KnowledgeChunkVO> out = new ArrayList<>(rows.size());
        for (AgentMilvusStore.ChunkRow row : rows) {
            out.add(toChunkVo(row));
        }
        return out;
    }

    @Override
    public KnowledgeChunkVO updateChunk(Long docId, Integer chunkIndex, KnowledgeChunkUpdateDTO dto) {
        requireDoc(docId);
        if (chunkIndex == null || chunkIndex < 0) {
            throw new BizException("分块序号无效");
        }
        if (dto == null || dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BizException("分块内容不能为空");
        }
        return toChunkVo(indexService.updateChunk(docId, chunkIndex, dto.getContent()));
    }

    @Override
    public KnowledgeDocVO reindex(Long id) {
        requireDoc(id);
        indexService.indexDocAsync(id);
        return toDocVo(docMapper.selectById(id));
    }

    @Override
    public SseEmitter reindexStream(Long id) {
        requireDoc(id);
        AtomicBoolean closed = new AtomicBoolean(false);
        final SseEmitter[] box = new SseEmitter[1];
        SseEmitter emitter = new SseEmitter(-1L)
                .onCompletion(() -> closed.set(true))
                .onError(e -> closed.set(true))
                .onInited(s -> {
                    box[0] = s;
                    asyncExecutor.execute(() -> runReindexStream(s, id, closed));
                });
        box[0] = emitter;
        return emitter;
    }

    private void runReindexStream(SseEmitter emitter, Long id, AtomicBoolean closed) {
        try {
            AgentKnowledgeDoc result = indexService.indexDoc(id, doc ->
                    AgentSseUtil.safeSend(emitter, closed, AgentSseEvent.PROGRESS.getCode(), JsonUtil.toJson(toProgressPayload(doc))));
            AgentSseUtil.safeSend(emitter, closed, AgentSseEvent.DONE.getCode(), JsonUtil.toJson(toProgressPayload(result)));
        } catch (Exception e) {
            log.warn("SSE 索引失败 docId={}: {}", id, e.getMessage());
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("msg", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            AgentKnowledgeDoc doc = docMapper.selectById(id);
            if (doc != null) {
                err.putAll(toProgressPayload(doc));
            }
            AgentSseUtil.safeSend(emitter, closed, AgentSseEvent.ERROR.getCode(), JsonUtil.toJson(err));
        } finally {
            if (!closed.get()) {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }

    private static Map<String, Object> toProgressPayload(AgentKnowledgeDoc doc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", doc.getId());
        m.put("knowledgeId", doc.getKnowledgeId());
        m.put("status", doc.getStatus());
        m.put("indexProgress", doc.getIndexProgress() == null ? 0 : doc.getIndexProgress());
        m.put("chunkCount", doc.getChunkCount() == null ? 0 : doc.getChunkCount());
        m.put("charCount", doc.getCharCount() == null ? 0 : doc.getCharCount());
        m.put("errorMsg", doc.getErrorMsg() == null ? "" : doc.getErrorMsg());
        m.put("title", doc.getTitle());
        // SSE 走 JsonUtil，需显式格式化，避免 LocalDateTime 被序列成时间戳
        m.put("updateTime", formatDateTime(doc.getUpdateTime()));
        return m;
    }

    private static String formatDateTime(java.time.LocalDateTime t) {
        if (t == null) {
            return "";
        }
        return t.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void validateKb(KnowledgeSaveDTO dto, Long excludeId) {
        if (Utils.isBlank(dto.getName())) {
            throw new BizException("知识库名称不能为空");
        }
        if (dto.getEmbeddingProviderId() == null) {
            throw new BizException("请选择 Embedding 供应商");
        }
        if (Utils.isBlank(dto.getEmbeddingModel())) {
            throw new BizException("请选择 Embedding 模型");
        }
        AgentEmbeddingClient.assertOpenAiCompatEmbedding(dto.getEmbeddingModel().trim());
        AgentProvider provider = providerMapper.selectById(dto.getEmbeddingProviderId());
        if (provider == null || provider.getStatus() == null || provider.getStatus() != SystemConsts.STATUS_ENABLE) {
            throw new BizException("Embedding 供应商不可用");
        }
        if (Utils.isNotBlank(dto.getCode())) {
            Long cnt = knowledgeMapper.selectCount(new LambdaQueryWrapper<AgentKnowledge>()
                    .eq(AgentKnowledge::getCode, dto.getCode().trim())
                    .ne(excludeId != null, AgentKnowledge::getId, excludeId));
            if (cnt != null && cnt > 0) {
                throw new BizException("知识库编码已存在");
            }
        }
    }

    /** 库表仍保留唯一 code，前端不展示时由服务端生成。 */
    private String nextKnowledgeCode() {
        for (int i = 0; i < 5; i++) {
            String code = AgentConsts.CODE_PREFIX_KNOWLEDGE + Long.toString(System.currentTimeMillis(), 36)
                    + Integer.toString((int) (Math.random() * 36 * 36), 36);
            Long cnt = knowledgeMapper.selectCount(new LambdaQueryWrapper<AgentKnowledge>()
                    .eq(AgentKnowledge::getCode, code));
            if (cnt == null || cnt == 0) {
                return code;
            }
        }
        return AgentConsts.CODE_PREFIX_KNOWLEDGE + java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private AgentKnowledge requireKb(Long id) {
        return AgentEntityUtil.require(knowledgeMapper.selectById(id), "知识库不存在");
    }

    private AgentKnowledgeDoc requireDoc(Long id) {
        return AgentEntityUtil.require(docMapper.selectById(id), "文档不存在");
    }

    private AgentKnowledge toKbEntity(KnowledgeSaveDTO dto) {
        AgentKnowledge e = new AgentKnowledge();
        e.setId(dto.getId());
        e.setName(dto.getName().trim());
        e.setCode(Utils.isNotBlank(dto.getCode()) ? dto.getCode().trim() : "");
        e.setDescription(dto.getDescription() == null ? "" : dto.getDescription());
        e.setTagsJson(AgentJsonUtil.toJson(dto.getTags() == null ? List.of() : dto.getTags()));
        e.setEmbeddingProviderId(dto.getEmbeddingProviderId());
        e.setEmbeddingModel(Utils.isNotBlank(dto.getEmbeddingModel()) ? dto.getEmbeddingModel().trim() : "");
        e.setEmbeddingDim(0);
        e.setChunkStrategy(AgentTextChunker.normalizeStrategy(dto.getChunkStrategy()));
        e.setChunkSize(dto.getChunkSize() == null ? AgentConsts.DEFAULT_CHUNK_SIZE : dto.getChunkSize());
        e.setChunkOverlap(dto.getChunkOverlap() == null ? AgentConsts.DEFAULT_CHUNK_OVERLAP : dto.getChunkOverlap());
        e.setTopK(dto.getTopK() == null ? AgentConsts.DEFAULT_TOP_K : dto.getTopK());
        e.setScoreThreshold(dto.getScoreThreshold() == null
                ? AgentConsts.DEFAULT_KNOWLEDGE_SCORE_THRESHOLD : dto.getScoreThreshold());
        e.setStatus(dto.getStatus());
        return e;
    }

    private KnowledgeDocVO toDocVo(AgentKnowledgeDoc e) {
        if (e == null) {
            return null;
        }
        KnowledgeDocVO vo = new KnowledgeDocVO();
        vo.setId(e.getId());
        vo.setKnowledgeId(e.getKnowledgeId());
        vo.setTitle(e.getTitle());
        vo.setSourceType(e.getSourceType());
        vo.setFileName(e.getFileName());
        vo.setFileType(e.getFileType());
        vo.setFileSize(e.getFileSize());
        vo.setContent(e.getContent());
        vo.setUrl(e.getUrl());
        vo.setStatus(e.getStatus());
        vo.setChunkCount(e.getChunkCount());
        vo.setIndexProgress(e.getIndexProgress());
        vo.setCharCount(e.getCharCount());
        vo.setErrorMsg(e.getErrorMsg());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    private KnowledgeVO toVo(AgentKnowledge e) {
        List<AgentKnowledgeDoc> docs = docMapper.selectList(new LambdaQueryWrapper<AgentKnowledgeDoc>()
                .eq(AgentKnowledgeDoc::getKnowledgeId, e.getId()));
        int ready = 0;
        int chunkCount = 0;
        int charCount = 0;
        boolean indexing = false;
        boolean failed = false;
        for (AgentKnowledgeDoc d : docs) {
            if (KnowledgeDocStatus.READY.eq(d.getStatus())) {
                ready++;
            }
            if (KnowledgeDocStatus.INDEXING.eq(d.getStatus())) {
                indexing = true;
            }
            if (KnowledgeDocStatus.FAILED.eq(d.getStatus())) {
                failed = true;
            }
            chunkCount += d.getChunkCount() == null ? 0 : d.getChunkCount();
            charCount += d.getCharCount() == null ? 0 : d.getCharCount();
        }
        String indexStatus = "empty";
        if (indexing) {
            indexStatus = KnowledgeDocStatus.INDEXING.getCode();
        } else if (ready > 0) {
            indexStatus = KnowledgeDocStatus.READY.getCode();
        } else if (failed) {
            indexStatus = KnowledgeDocStatus.FAILED.getCode();
        }

        KnowledgeVO vo = new KnowledgeVO();
        vo.setId(e.getId());
        vo.setName(e.getName());
        vo.setCode(e.getCode());
        vo.setDescription(e.getDescription());
        vo.setTags(AgentJsonUtil.parseStringList(e.getTagsJson()));
        vo.setEmbeddingProviderId(e.getEmbeddingProviderId());
        vo.setEmbeddingModel(e.getEmbeddingModel());
        vo.setEmbeddingDim(e.getEmbeddingDim());
        vo.setChunkStrategy(AgentTextChunker.normalizeStrategy(e.getChunkStrategy()));
        vo.setChunkSize(e.getChunkSize());
        vo.setChunkOverlap(e.getChunkOverlap());
        vo.setTopK(e.getTopK());
        vo.setScoreThreshold(e.getScoreThreshold());
        vo.setStatus(e.getStatus());
        vo.setDocCount(docs.size());
        vo.setChunkCount(chunkCount);
        vo.setCharCount(charCount);
        vo.setReadyDocCount(ready);
        vo.setIndexStatus(indexStatus);
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    private static KnowledgeChunkVO toChunkVo(AgentMilvusStore.ChunkRow row) {
        KnowledgeChunkVO vo = new KnowledgeChunkVO();
        vo.setId(row.id());
        vo.setDocId(row.docId());
        vo.setChunkIndex(row.chunkIndex());
        vo.setContent(row.content());
        vo.setCharCount(row.content() == null ? 0 : row.content().length());
        return vo;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (Utils.isNotBlank(v)) {
                return v.trim();
            }
        }
        return "";
    }
}
