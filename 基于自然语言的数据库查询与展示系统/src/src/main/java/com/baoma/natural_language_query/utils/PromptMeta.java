package com.baoma.natural_language_query.utils;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * Prompt 元数据封装类
 * 用于保存一次自然语言查询请求在 Prompt 层面的所有静态与动态信息，
 * 包括系统提示、用户输入、模板版本、Schema 摘要、时间戳、返回内容、
 * 模板类型、策略标识以及可扩展的附加字段。
 *
 * 本类由 Lombok @Data 自动生成 toString/equals/hashCode 以及
 * 所有字段的 Getter/Setter；下方仅对需要特殊处理的字段提供显式实现。
 */

@Data
public class PromptMeta {

    /** 系统级提示词（System Prompt） */
    private String system;

    /** 用户原始查询（User Prompt） */
    private String user;

    /** 模型返回的完整响应文本 */
    private String response;

    /** 当前使用的 Prompt 模板版本号*/
    private String templateVersion;

    /** Prompt 模板类型，例如“sql”、“api”、“text2chart”等 */
    private String templateType;

    /** 本次查询所依赖的 Schema 摘要（哈希值），用于缓存与一致性校验 */
    private String schemaHash;

    /** Schema 复杂度描述，如“简单”、“中等”、“复杂”，用于路由不同策略 */
    private String schemaComplexity;

    /** Prompt 生成时间戳（ISO-8601 格式） */
    private String timestamp;

    /** 当前 Manager 版本号，用于追踪代码变更 */
    private String managerVersion;

    /** 本次查询采用的路由/重试策略名，如“v1-fast”、“v2-accurate” */
    private String strategy;

    /** 标记当前 Prompt 是否通过合法性校验 */
    private boolean isValid;

    /** 可扩展的附加元数据，运行时可动态追加 K-V 信息 */
    private Map<String, String> additionalMetadata = new HashMap<>();

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    /**
     * 便捷方法：向附加元数据中添加单个键值对
     *
     * @param key   字段名
     * @param value 字段值
     */
    
    public void setAdditionalMetadata(String key, String value) {
        this.additionalMetadata.put(key, value);
    }
}