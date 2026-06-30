package com.baoma.natural_language_query.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baoma.natural_language_query.entity.mysql.DbConnection;
import com.baoma.natural_language_query.entity.mysql.LlmConfig;
import com.baoma.natural_language_query.service.DatabaseSchemaService;
import com.baoma.natural_language_query.service.LLMService;
import com.baoma.natural_language_query.service.LlmConfigService;
import com.baoma.natural_language_query.service.TokenConsumeService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 大模型调用服务实现 支持所有兼容 OpenAI Chat Completions API 的模型 */
@Service
public class LLMServiceImpl implements LLMService {

  @Autowired private LlmConfigService llmConfigService;

  @Autowired private DatabaseSchemaService databaseSchemaService;
  
  @Autowired private TokenConsumeService tokenConsumeService;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

  @Override
  public Map<String, Object> generateQuery(
      String prompt, String modelConfigId, String databaseName) {
    return generateQueryWithSchema(prompt, modelConfigId, databaseName, null);
  }

  @Override
  public Map<String, Object> generateQueryWithSchema(
      String prompt, String modelConfigId, String databaseName, String schemaInfo) {
    try {
      // 根据配置ID从数据库获取模型配置
      LlmConfig config = llmConfigService.getById(Long.valueOf(modelConfigId));
      if (config == null) {
        throw new RuntimeException("模型配置不存在，ID: " + modelConfigId);
      }
      if (config.getIsDisabled() == 1) {
        throw new RuntimeException("该模型配置已被禁用");
      }

      // 统一调用大模型API
      return callLlmApi(prompt, config, databaseName, schemaInfo);
    } catch (NumberFormatException e) {
      throw new RuntimeException("无效的模型配置ID: " + modelConfigId);
    } catch (Exception e) {
      throw new RuntimeException("模型调用失败: " + e.getMessage(), e);
    }
  }

  /** 统一调用大模型API 支持所有兼容 OpenAI Chat Completions API 的模型 */
  private Map<String, Object> callLlmApi(
      String prompt, LlmConfig config, String databaseName, String schemaInfo) throws Exception {
    String apiKey = config.getApiKey().trim();
    String url = config.getApiUrl().trim();
    String modelName = config.getVersion().trim();

    // 打印调试信息
    System.out.println("=== LLM API 调用信息 ===");
    System.out.println("配置名称: " + config.getName() + " (ID: " + config.getId() + ")");
    System.out.println("API URL: " + url);
    System.out.println("模型名称: " + modelName);
    System.out.println(
        "API Key 前10位: " + (apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : apiKey));

    // 构建请求体（OpenAI Chat Completions API 格式）
    JSONObject requestBody = new JSONObject();
    requestBody.put("model", modelName);
    requestBody.put(
        "messages",
        Arrays.asList(
            Map.of("role", "user", "content", generatePrompt(prompt, databaseName, schemaInfo))));
    requestBody.put("response_format", Map.of("type", "json_object"));
    requestBody.put("temperature", 0.0);

    System.out.println("请求体: " + requestBody.toJSONString());

    // 发送HTTP请求
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
            .timeout(
                Duration.ofSeconds(config.getTimeout() != null ? config.getTimeout() / 1000 : 60))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    System.out.println("响应状态码: " + response.statusCode());
    System.out.println("响应内容: " + response.body());
    System.out.println("=========================");

    if (response.statusCode() != 200) {
      throw new RuntimeException("API调用失败: " + response.statusCode() + ", 响应: " + response.body());
    }

    JSONObject jsonResponse = JSON.parseObject(response.body());

    // 解析响应（OpenAI格式）
    if (!jsonResponse.containsKey("choices") || jsonResponse.getJSONArray("choices").isEmpty()) {
      throw new RuntimeException("API响应格式错误：缺少choices字段");
    }
    
    // 提取token消耗信息 (callLlmApi)
    recordTokenUsage(jsonResponse, config);

    JSONObject choice = jsonResponse.getJSONArray("choices").getJSONObject(0);
    if (!choice.containsKey("message")) {
      throw new RuntimeException("API响应格式错误：缺少message字段");
    }

    String content = choice.getJSONObject("message").getString("content");
    if (content == null || content.isEmpty()) {
      throw new RuntimeException("API返回内容为空");
    }

    // 清理可能的markdown代码块标记
    String cleanedContent = content.replaceAll("^```json\\n|```$", "").trim();
    return parseJsonResponse(cleanedContent);
  }

  /** 生成统一的Prompt（包含数据库结构信息） */
  private String generatePrompt(String prompt, String databaseName, String schemaInfo) {
    StringBuilder promptBuilder = new StringBuilder();

    promptBuilder.append("你是数据查询助手，需将用户请求转换为指定JSON格式。\n");
    promptBuilder.append("连接的数据库为\"").append(databaseName).append("\"，仅生成该数据库的SQL。\n");
    promptBuilder.append("响应必须是单个有效的JSON对象，不包含任何额外文本或格式（如```json）。\n\n");

    // 如果有表结构信息，添加到Prompt中
    if (schemaInfo != null && !schemaInfo.isEmpty()) {
      promptBuilder.append("=== 数据库表结构信息 ===\n");
      promptBuilder.append(schemaInfo);
      promptBuilder.append("\n请根据上述真实的表结构生成SQL，确保使用正确的表名和列名。\n");
      promptBuilder.append("注意：必须使用实际存在的列名，不要猜测或假设列名。\n\n");
    }

    promptBuilder.append("用户请求：\"").append(prompt).append("\"\n\n");
    promptBuilder.append("规则：\n");
    promptBuilder.append("- 数据查询（可SQL回答）：success=true，生成SQL、表格数据和图表数据\n");
    promptBuilder.append("- 非数据查询：success=false，表格数据用[\"Message\"]和[\"抱歉，仅支持数据查询\"]\n");
    promptBuilder.append("- 必须使用上述表结构中实际存在的列名\n");
    promptBuilder.append("- SQL语句必须符合MySQL语法\n\n");
    promptBuilder.append("返回JSON格式：\n");
    promptBuilder.append("{\n");
    promptBuilder.append("  \"success\": true/false,\n");
    promptBuilder.append("  \"sqlQuery\": \"SQL语句\",\n");
    promptBuilder.append("  \"tableData\": {\n");
    promptBuilder.append("    \"headers\": [\"列1\", \"列2\"],\n");
    promptBuilder.append("    \"rows\": [[\"值1\", \"值2\"]]\n");
    promptBuilder.append("  },\n");
    promptBuilder.append("  \"chartData\": {\n");
    promptBuilder.append("    \"type\": \"bar/line/pie\",\n");
    promptBuilder.append("    \"labels\": [\"标签1\"],\n");
    promptBuilder.append("    \"datasets\": [{\n");
    promptBuilder.append("      \"label\": \"数据标签\",\n");
    promptBuilder.append("      \"data\": [1, 2, 3],\n");
    promptBuilder.append("      \"backgroundColor\": \"rgba(22, 93, 255, 0.6)\"\n");
    promptBuilder.append("    }]\n");
    promptBuilder.append("  }\n");
    promptBuilder.append("}");

    return promptBuilder.toString();
  }

  /** 解析JSON响应 */
  private Map<String, Object> parseJsonResponse(String jsonContent) {
    try {
      JSONObject json = JSON.parseObject(jsonContent);
      Map<String, Object> result = new HashMap<>();
      result.put("success", json.getBooleanValue("success"));
      result.put("sqlQuery", json.getString("sqlQuery"));
      result.put("tableData", json.getJSONObject("tableData"));
      result.put("chartData", json.getJSONObject("chartData"));
      return result;
    } catch (Exception e) {
      throw new RuntimeException("解析模型响应失败: " + e.getMessage(), e);
    }
  }

  @Override
  public Map<String, Object> generateQueryWithConnection(
      String prompt, String modelConfigId, String databaseName, DbConnection dbConnection) {
    try {
      // 获取数据库表结构
      System.out.println("✓ 开始获取数据库表结构信息...");
      String schemaInfo = databaseSchemaService.getDatabaseSchema(dbConnection);
      System.out.println("✓ 已获取数据库表结构信息");

      // 根据配置ID从数据库获取模型配置
      LlmConfig config = llmConfigService.getById(Long.valueOf(modelConfigId));
      if (config == null) {
        throw new RuntimeException("模型配置不存在，ID: " + modelConfigId);
      }
      if (config.getIsDisabled() == 1) {
        throw new RuntimeException("该模型配置已被禁用");
      }

      // 使用包含表结构的 prompt 调用大模型
      return callLlmApiWithSchema(prompt, config, databaseName, schemaInfo);
    } catch (NumberFormatException e) {
      throw new RuntimeException("无效的模型配置ID: " + modelConfigId);
    } catch (Exception e) {
      throw new RuntimeException("模型调用失败: " + e.getMessage(), e);
    }
  }

  /** 调用大模型API（包含表结构信息） */
  private Map<String, Object> callLlmApiWithSchema(
      String prompt, LlmConfig config, String databaseName, String schemaInfo) throws Exception {
    String apiKey = config.getApiKey().trim();
    String url = config.getApiUrl().trim();
    String modelName = config.getVersion().trim();

    // 打印调试信息
    System.out.println("=== LLM API 调用信息（含表结构） ===");
    System.out.println("配置名称: " + config.getName() + " (ID: " + config.getId() + ")");
    System.out.println("API URL: " + url);
    System.out.println("模型名称: " + modelName);

    // 构建包含表结构的 prompt
    String enhancedPrompt = generatePrompt(prompt, databaseName, schemaInfo);

    // 构建请求体（OpenAI Chat Completions API 格式）
    JSONObject requestBody = new JSONObject();
    requestBody.put("model", modelName);
    requestBody.put("messages", Arrays.asList(Map.of("role", "user", "content", enhancedPrompt)));
    requestBody.put("response_format", Map.of("type", "json_object"));
    requestBody.put("temperature", 0.0);

    System.out.println("发送请求到大模型...");

    // 发送HTTP请求
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
            .timeout(
                Duration.ofSeconds(config.getTimeout() != null ? config.getTimeout() / 1000 : 60))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    System.out.println("响应状态码: " + response.statusCode());
    System.out.println("=========================");

    if (response.statusCode() != 200) {
      throw new RuntimeException("API调用失败: " + response.statusCode() + ", 响应: " + response.body());
    }

    JSONObject jsonResponse = JSON.parseObject(response.body());

    // 解析响应（OpenAI格式）
    if (!jsonResponse.containsKey("choices") || jsonResponse.getJSONArray("choices").isEmpty()) {
      throw new RuntimeException("API响应格式错误：缺少choices字段");
    }
    
    // 提取token消耗信息 (callLlmApiWithSchema)
    recordTokenUsage(jsonResponse, config);

    JSONObject choice = jsonResponse.getJSONArray("choices").getJSONObject(0);
    if (!choice.containsKey("message")) {
      throw new RuntimeException("API响应格式错误：缺少message字段");
    }

    String content = choice.getJSONObject("message").getString("content");
    if (content == null || content.isEmpty()) {
      throw new RuntimeException("API返回内容为空");
    }

    // 清理可能的markdown代码块标记
    String cleanedContent = content.replaceAll("^```json\\n|```$", "").trim();
    return parseJsonResponse(cleanedContent);
  }

  @Override
  public Map<String, Object> generateQueryWithCustomMessages(
      String systemMessage, String userMessage, String modelConfigId) {
    try {
      // 根据配置ID从数据库获取模型配置
      LlmConfig config = llmConfigService.getById(Long.valueOf(modelConfigId));
      if (config == null) {
        throw new RuntimeException("模型配置不存在，ID: " + modelConfigId);
      }
      if (config.getIsDisabled() == 1) {
        throw new RuntimeException("该模型配置已被禁用");
      }

      // 调用大模型API（使用自定义消息）
      return callLlmApiWithCustomMessages(systemMessage, userMessage, config);
    } catch (NumberFormatException e) {
      throw new RuntimeException("无效的模型配置ID: " + modelConfigId);
    } catch (Exception e) {
      throw new RuntimeException("模型调用失败: " + e.getMessage(), e);
    }
  }

  /** 使用自定义的system和user消息调用大模型API */
  private Map<String, Object> callLlmApiWithCustomMessages(
      String systemMessage, String userMessage, LlmConfig config) throws Exception {
    String apiKey = config.getApiKey().trim();
    String url = config.getApiUrl().trim();
    String modelName = config.getVersion().trim();

    // 打印调试信息
    System.out.println("=== LLM API 调用信息（自定义消息） ===");
    System.out.println("配置名称: " + config.getName() + " (ID: " + config.getId() + ")");
    System.out.println("API URL: " + url);
    System.out.println("模型名称: " + modelName);

    // 构建请求体（OpenAI Chat Completions API 格式）
    JSONObject requestBody = new JSONObject();
    requestBody.put("model", modelName);
    
    // 构建包含system和user的消息数组
    java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
    if (systemMessage != null && !systemMessage.isEmpty()) {
      messages.add(Map.of("role", "system", "content", systemMessage));
    }
    messages.add(Map.of("role", "user", "content", userMessage));
    requestBody.put("messages", messages);
    
    // 不强制JSON格式，因为PromptManager期望的格式可能不同
    requestBody.put("temperature", 0.0);

    System.out.println("发送请求到大模型...");

    // 发送HTTP请求
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
            .timeout(
                Duration.ofSeconds(config.getTimeout() != null ? config.getTimeout() / 1000 : 60))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    System.out.println("响应状态码: " + response.statusCode());
    System.out.println("=========================");

    if (response.statusCode() != 200) {
      throw new RuntimeException("API调用失败: " + response.statusCode() + ", 响应: " + response.body());
    }

    JSONObject jsonResponse = JSON.parseObject(response.body());

    // 解析响应（OpenAI格式）
    if (!jsonResponse.containsKey("choices") || jsonResponse.getJSONArray("choices").isEmpty()) {
      throw new RuntimeException("API响应格式错误：缺少choices字段");
    }
    
    // 提取token消耗信息 (callLlmApiWithCustomMessages)
    recordTokenUsage(jsonResponse, config);

    JSONObject choice = jsonResponse.getJSONArray("choices").getJSONObject(0);
    if (!choice.containsKey("message")) {
      throw new RuntimeException("API响应格式错误：缺少message字段");
    }

    String content = choice.getJSONObject("message").getString("content");
    if (content == null || content.isEmpty()) {
      throw new RuntimeException("API返回内容为空");
    }

    // 清理可能的markdown代码块标记
    String cleanedContent = content.replaceAll("^```json\\n|```$", "").trim();
    
    // 返回原始响应内容，让调用者自己解析
    Map<String, Object> result = new HashMap<>();
    result.put("rawResponse", cleanedContent);
    return result;
  }

  @Override
  public Map<String, Object> generateQueryWithConversationHistory(
      java.util.List<java.util.Map<String, String>> messages, String modelConfigId) {
    try {
      // 根据配置ID从数据库获取模型配置
      LlmConfig config = llmConfigService.getById(Long.valueOf(modelConfigId));
      if (config == null) {
        throw new RuntimeException("模型配置不存在，ID: " + modelConfigId);
      }
      if (config.getIsDisabled() == 1) {
        throw new RuntimeException("该模型配置已被禁用");
      }

      // 调用大模型API（使用多轮对话消息）
      return callLlmApiWithMessages(messages, config);
    } catch (NumberFormatException e) {
      throw new RuntimeException("无效的模型配置ID: " + modelConfigId);
    } catch (Exception e) {
      throw new RuntimeException("模型调用失败: " + e.getMessage(), e);
    }
  }

  /** 使用完整消息列表调用大模型API（支持多轮对话） */
  private Map<String, Object> callLlmApiWithMessages(
      java.util.List<java.util.Map<String, String>> messages, LlmConfig config) throws Exception {
    String apiKey = config.getApiKey().trim();
    String url = config.getApiUrl().trim();
    String modelName = config.getVersion().trim();

    // 打印调试信息
    System.out.println("=== LLM API 调用信息（多轮对话） ===");
    System.out.println("配置名称: " + config.getName() + " (ID: " + config.getId() + ")");
    System.out.println("API URL: " + url);
    System.out.println("模型名称: " + modelName);
    System.out.println("消息数量: " + messages.size());

    // 构建请求体（OpenAI Chat Completions API 格式）
    JSONObject requestBody = new JSONObject();
    requestBody.put("model", modelName);
    requestBody.put("messages", messages);
    requestBody.put("temperature", 0.0);

    System.out.println("发送请求到大模型（包含对话历史）...");

    // 发送HTTP请求
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
            .timeout(
                Duration.ofSeconds(config.getTimeout() != null ? config.getTimeout() / 1000 : 60))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    System.out.println("响应状态码: " + response.statusCode());
    System.out.println("=========================");

    if (response.statusCode() != 200) {
      throw new RuntimeException("API调用失败: " + response.statusCode() + ", 响应: " + response.body());
    }

    JSONObject jsonResponse = JSON.parseObject(response.body());

    // 解析响应（OpenAI格式）
    if (!jsonResponse.containsKey("choices") || jsonResponse.getJSONArray("choices").isEmpty()) {
      throw new RuntimeException("API响应格式错误：缺少choices字段");
    }
    
    // 提取token消耗信息 (callLlmApiWithMessages)
    recordTokenUsage(jsonResponse, config);

    JSONObject choice = jsonResponse.getJSONArray("choices").getJSONObject(0);
    if (!choice.containsKey("message")) {
      throw new RuntimeException("API响应格式错误：缺少message字段");
    }

    String content = choice.getJSONObject("message").getString("content");
    if (content == null || content.isEmpty()) {
      throw new RuntimeException("API返回内容为空");
    }

    // 清理可能的markdown代码块标记
    String cleanedContent = content.replaceAll("^```json\\n|```$", "").trim();
    
    // 返回原始响应内容，让调用者自己解析
    Map<String, Object> result = new HashMap<>();
    result.put("rawResponse", cleanedContent);
    return result;
  }
  
  /**
   * 记录token消耗到数据库
   */
  private void recordTokenUsage(JSONObject jsonResponse, LlmConfig config) {
    try {
      if (!jsonResponse.containsKey("usage")) {
        System.out.println("API响应中没有usage字段，跳过token记录");
        return;
      }
      
      JSONObject usage = jsonResponse.getJSONObject("usage");
      Integer promptTokens = usage.getInteger("prompt_tokens");
      Integer completionTokens = usage.getInteger("completion_tokens");
      Integer totalTokens = usage.getInteger("total_tokens");
      
      if (totalTokens == null || totalTokens == 0) {
        return;
      }
      
      System.out.println("Token消耗: prompt=" + promptTokens + ", completion=" + completionTokens + ", total=" + totalTokens);
      
      // 获取或创建今日token消耗记录
      LocalDate today = LocalDate.now();
      String llmName = config.getName();
      
      com.baoma.natural_language_query.entity.mysql.TokenConsume tokenConsume = 
          tokenConsumeService.getByLlmNameAndDate(llmName, today);
      
      if (tokenConsume == null) {
        // 创建新记录
        tokenConsume = new com.baoma.natural_language_query.entity.mysql.TokenConsume();
        tokenConsume.setLlmName(llmName);
        tokenConsume.setTotalTokens(totalTokens);
        tokenConsume.setPromptTokens(promptTokens);
        tokenConsume.setCompletionTokens(completionTokens);
        tokenConsume.setConsumeDate(today);
        tokenConsumeService.save(tokenConsume);
        System.out.println("✓ 创建今日token消耗记录: " + llmName + ", total=" + totalTokens);
      } else {
        // 更新现有记录
        tokenConsume.setTotalTokens(tokenConsume.getTotalTokens() + totalTokens);
        tokenConsume.setPromptTokens(tokenConsume.getPromptTokens() + promptTokens);
        tokenConsume.setCompletionTokens(tokenConsume.getCompletionTokens() + completionTokens);
        tokenConsumeService.updateById(tokenConsume);
        System.out.println("✓ 更新今日token消耗: " + llmName + ", 累计total=" + tokenConsume.getTotalTokens());
      }
    } catch (Exception e) {
      System.err.println("记录token消耗失败: " + e.getMessage());
      e.printStackTrace();
      // 不抛出异常，避免影响主流程
    }
  }
}
