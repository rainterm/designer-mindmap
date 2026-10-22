package com.designer.admin.module.agent.constant;

import java.math.BigDecimal;

/**
 * 智能体模块数值默认值、上限与编码前缀。
 *
 * <p>启用/停用状态复用 {@link com.designer.admin.common.constant.SystemConsts}，
 * 封闭字符串类型见 {@code com.designer.admin.module.agent.enums}。</p>
 *
 * @author 雨夜终端
 */
public final class AgentConsts {

    /** 默认温度 */
    public static final BigDecimal DEFAULT_TEMPERATURE = new BigDecimal("0.7000");

    /** 默认 Top P */
    public static final BigDecimal DEFAULT_TOP_P = new BigDecimal("1.0000");

    /** 默认 RAG / 知识库相似度阈值 */
    public static final BigDecimal DEFAULT_SCORE_THRESHOLD = new BigDecimal("0.3500");

    /** 默认 RAG / 知识库相似度阈值（double） */
    public static final double DEFAULT_SCORE_THRESHOLD_D = 0.35d;

    /** 知识库实体默认相似度阈值（与 RAG 默认值不同） */
    public static final BigDecimal DEFAULT_KNOWLEDGE_SCORE_THRESHOLD = new BigDecimal("0.5000");

    /** 默认 Top K（最终进入上下文的条数） */
    public static final int DEFAULT_TOP_K = 5;

    /**
     * 开启 Rerank 时，向量召回候选数相对最终 Top K 的默认倍数。
     * 即默认 fetchK = topK × {@value}（并受 {@link #MAX_TOP_K} 限制）。
     */
    public static final int DEFAULT_RECALL_MULTIPLIER = 3;

    /** Top K / 召回候选 / 记忆轮数上限 */
    public static final int MAX_TOP_K = 50;

    /** 默认对话记忆轮数 */
    public static final int DEFAULT_MEMORY_ROUNDS = 10;

    /** 记忆轮数上限（与 {@link #MAX_TOP_K} 同值） */
    public static final int MAX_MEMORY_ROUNDS = 50;

    /** 默认分块大小 */
    public static final int DEFAULT_CHUNK_SIZE = 500;

    /** 默认分块重叠 */
    public static final int DEFAULT_CHUNK_OVERLAP = 50;

    /** 分块最小窗口 */
    public static final int MIN_CHUNK_SIZE = 64;

    /** 智能体编码前缀（服务端自动生成） */
    public static final String CODE_PREFIX_AGENT = "agent_";

    /** 知识库编码前缀（服务端自动生成） */
    public static final String CODE_PREFIX_KNOWLEDGE = "kb_";

    /** 草稿占位名称 / 编码前缀 */
    public static final String DRAFT = "draft";

    /** 知识库文档未命名标题 */
    public static final String DOC_UNTITLED = "未命名文档";

    /** 文件类型：二进制未识别 */
    public static final String FILE_TYPE_BIN = "bin";

    /** 工作流上下文键：输入 */
    public static final String CTX_INPUT = "input";

    /** 工作流上下文键：输出 */
    public static final String CTX_OUTPUT = "output";

    /** MySQL 默认端口 */
    public static final int DEFAULT_MYSQL_PORT = 3306;

    /** PostgreSQL 默认端口 */
    public static final int DEFAULT_POSTGRES_PORT = 5432;

    /** 文档错误信息截断长度 */
    public static final int ERROR_MSG_MAX_LEN = 480;

    private AgentConsts() {
    }
}
