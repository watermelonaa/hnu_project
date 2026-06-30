package com.baoma.natural_language_query.service;

import java.util.List;
import java.util.Map;

/**
 * 对话上下文服务接口
 * 提供对话记忆功能，读取和管理对话历史
 */
public interface ConversationContextService {
    
    /**
     * 获取对话的历史上下文（最近N轮对话）
     * 
     * @param conversationId 对话ID
     * @param maxRounds 最多返回多少轮对话（默认5轮）
     * @return 格式化的对话历史消息列表 [{"role": "user/assistant", "content": "..."}]
     */
    List<Map<String, String>> getConversationHistory(String conversationId, int maxRounds);
    
    /**
     * 获取对话的历史上下文（默认最近5轮）
     */
    List<Map<String, String>> getConversationHistory(String conversationId);
    
    /**
     * 获取格式化的对话历史文本（用于嵌入Prompt）
     * 
     * @param conversationId 对话ID
     * @param maxRounds 最多包含多少轮对话
     * @return 格式化的文本，例如：
     *         "Previous conversation:
     *          User: 查询订单数量
     *          Assistant: SELECT COUNT(*) FROM orders (显示了100条订单)
     *          User: 只看2023年的
     *          Assistant: SELECT COUNT(*) FROM orders WHERE year = 2023"
     */
    String getFormattedConversationHistory(String conversationId, int maxRounds);
    
    /**
     * 检查对话是否存在历史记录
     */
    boolean hasHistory(String conversationId);
}

