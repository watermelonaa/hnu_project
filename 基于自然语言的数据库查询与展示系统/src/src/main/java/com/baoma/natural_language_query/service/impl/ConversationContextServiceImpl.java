package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mongodb.DialogDetail;
import com.baoma.natural_language_query.repository.DialogDetailRepository;
import com.baoma.natural_language_query.service.ConversationContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 对话上下文服务实现
 * 负责从MongoDB读取对话历史，并格式化为LLM可用的消息列表
 */
@Service
public class ConversationContextServiceImpl implements ConversationContextService {
    
    @Autowired
    private DialogDetailRepository dialogDetailRepository;
    
    @Override
    public List<Map<String, String>> getConversationHistory(String conversationId, int maxRounds) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        if (conversationId == null || conversationId.isEmpty()) {
            return messages;
        }
        
        try {
            // 从MongoDB获取对话详情
            DialogDetail dialogDetail = dialogDetailRepository.findByDialogId(conversationId);
            
            if (dialogDetail == null || dialogDetail.getRounds() == null || dialogDetail.getRounds().isEmpty()) {
                System.out.println("对话历史为空: " + conversationId);
                return messages;
            }
            
            // 按轮次排序，取最近的N轮
            List<DialogDetail.Round> rounds = dialogDetail.getRounds().stream()
                    .sorted((a, b) -> b.getRoundNum().compareTo(a.getRoundNum())) // 降序
                    .limit(maxRounds)
                    .sorted((a, b) -> a.getRoundNum().compareTo(b.getRoundNum())) // 再升序，保持时间顺序
                    .collect(Collectors.toList());
            
            System.out.println("读取到 " + rounds.size() + " 轮历史对话");
            
            // 转换为消息格式
            for (DialogDetail.Round round : rounds) {
                // 添加用户消息
                if (round.getUserInput() != null && !round.getUserInput().isEmpty()) {
                    Map<String, String> userMessage = new HashMap<>();
                    userMessage.put("role", "user");
                    userMessage.put("content", round.getUserInput());
                    messages.add(userMessage);
                }
                
                // 添加助手回复（包含SQL和思考过程）
                if (round.getGeneratedSql() != null && !round.getGeneratedSql().isEmpty()) {
                    Map<String, String> assistantMessage = new HashMap<>();
                    assistantMessage.put("role", "assistant");
                    
                    // 构建助手回复内容：包含SQL和简要说明
                    StringBuilder content = new StringBuilder();
                    content.append("Generated SQL: ").append(round.getGeneratedSql());
                    
                    // 如果有AI响应文本，提取简要说明
                    if (round.getAiResponse() != null && !round.getAiResponse().isEmpty()) {
                        String summary = extractSummaryFromResponse(round.getAiResponse());
                        if (summary != null && !summary.isEmpty()) {
                            content.append("\nResult: ").append(summary);
                        }
                    }
                    
                    assistantMessage.put("content", content.toString());
                    messages.add(assistantMessage);
                }
            }
            
            System.out.println("转换为 " + messages.size() + " 条消息");
            return messages;
            
        } catch (Exception e) {
            System.err.println("获取对话历史失败: " + e.getMessage());
            e.printStackTrace();
            return messages;
        }
    }
    
    @Override
    public List<Map<String, String>> getConversationHistory(String conversationId) {
        return getConversationHistory(conversationId, 5); // 默认最近5轮
    }
    
    @Override
    public String getFormattedConversationHistory(String conversationId, int maxRounds) {
        List<Map<String, String>> messages = getConversationHistory(conversationId, maxRounds);
        
        if (messages.isEmpty()) {
            return "";
        }
        
        StringBuilder formatted = new StringBuilder();
        formatted.append("=== Previous Conversation History ===\n");
        
        for (Map<String, String> message : messages) {
            String role = message.get("role");
            String content = message.get("content");
            
            if ("user".equals(role)) {
                formatted.append("User: ").append(content).append("\n");
            } else if ("assistant".equals(role)) {
                formatted.append("Assistant: ").append(content).append("\n");
            }
        }
        
        formatted.append("=== End of History ===\n\n");
        return formatted.toString();
    }
    
    @Override
    public boolean hasHistory(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return false;
        }
        
        try {
            DialogDetail dialogDetail = dialogDetailRepository.findByDialogId(conversationId);
            return dialogDetail != null && 
                   dialogDetail.getRounds() != null && 
                   !dialogDetail.getRounds().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从AI响应中提取摘要信息
     * 避免在历史记录中包含过长的数据
     */
    private String extractSummaryFromResponse(String aiResponse) {
        try {
            // 如果响应包含查询结果，提取行数等摘要信息
            if (aiResponse.contains("rows") || aiResponse.contains("tableData")) {
                // 尝试提取简要信息
                if (aiResponse.length() > 200) {
                    return "Query executed successfully with results";
                }
                return aiResponse;
            }
            
            // 限制长度
            if (aiResponse.length() > 200) {
                return aiResponse.substring(0, 200) + "...";
            }
            
            return aiResponse;
        } catch (Exception e) {
            return "Query executed";
        }
    }
}

