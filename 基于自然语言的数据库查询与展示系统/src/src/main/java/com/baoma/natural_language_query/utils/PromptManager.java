package com.baoma.natural_language_query.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * Prompt生成策略接口 - 支持不同的prompt生成策略
 */
interface PromptGenerationStrategy {
    String generateSystemPrompt();
    String generateUserPrompt(String schema, String question);
    String getStrategyName();
}

/**
 * Prompt验证器接口 - 验证prompt的合法性和质量
 */
interface PromptValidator {
    boolean validatePrompt(PromptMeta promptMeta);
    String getValidationReport();
}

// =============== 具体策略实现 ===============

/**
 * 标准prompt策略实现
 */
class StandardPromptStrategy implements PromptGenerationStrategy {
    @Override
    public String generateSystemPrompt() {
        return PromptTemplateType.STANDARD.generateSystemPrompt();
    }
    
    @Override
    public String generateUserPrompt(String schema, String question) {
        return PromptTemplateType.STANDARD.generateUserPrompt(schema, question);
    }
    
    @Override
    public String getStrategyName() {
        return "StandardPromptStrategy";
    }
}

/**
 * 增强prompt实现 - 包含更多约束和指导
 */
class EnhancedPromptStrategy implements PromptGenerationStrategy {
    @Override
    public String generateSystemPrompt() {
        return PromptTemplateType.ENHANCED.generateSystemPrompt();
    }
    
    @Override
    public String generateUserPrompt(String schema, String question) {
        String basePrompt = PromptTemplateType.ENHANCED.generateUserPrompt(schema, question);
        // 添加schema摘要
        String schemaSummary = generateSchemaSummary(schema);
        return basePrompt + "\n\nSchema Summary:\n" + schemaSummary;
    }
    
    private String generateSchemaSummary(String schema) {
        // 提取表名和字段信息
        String[] lines = schema.split("\n");
        int tableCount = 0;
        StringBuilder summary = new StringBuilder();
        
        for (String line : lines) {
            if (line.toLowerCase().contains("create table")) {
                tableCount++;
                String tableName = extractTableName(line);
                summary.append("Table ").append(tableCount).append(": ").append(tableName).append("\n");
            }
        }
        
        return "Total Tables: " + tableCount + "\n" + summary.toString();
    }
    
    private String extractTableName(String createTableStatement) {
        // 简单提取表名
        String[] parts = createTableStatement.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("table") && i + 1 < parts.length) {
                return parts[i + 1].replace("`", "").replace("(", "").trim();
            }
        }
        return "unknown";
    }
    
    @Override
    public String getStrategyName() {
        return "EnhancedPromptStrategy";
    }
}

/**
 * 安全策略实现 - 包含额外的安全检查
 */
class SecurePromptStrategy implements PromptGenerationStrategy {
    @Override
    public String generateSystemPrompt() {
        return "You are a security-conscious SQL expert. Additional constraints:\n" +
               PromptTemplateType.STANDARD.generateSystemPrompt() + "\n" +
               "- NEVER include sensitive information in queries\n" +
               "- Add LIMIT clauses to prevent large result sets\n" +
               "- Validate all user input would be parameterized\n" +
               "- Check for potential SQL injection patterns";
    }
    
    @Override
    public String generateUserPrompt(String schema, String question) {
        String sanitizedQuestion = sanitizeInput(question);
        return PromptTemplateType.STANDARD.generateUserPrompt(schema, sanitizedQuestion) +
               "\n\nSecurity Note: All queries should be parameterizable and safe from SQL injection.";
    }
    
    private String sanitizeInput(String input) {
        // 简单的输入清理
        return input.replace("--", "").replace(";", "").trim();
    }
    
    @Override
    public String getStrategyName() {
        return "SecurePromptStrategy";
    }
}

// =============== 验证器实现 ===============

/**
 * 基础验证器 - 检查prompt的基本完整性
 */
class BasicPromptValidator implements PromptValidator {
    @Override
    public boolean validatePrompt(PromptMeta promptMeta) {
        if (promptMeta == null) {
            return false;
        }
        
        String systemPrompt = promptMeta.getSystem();
        String userPrompt = promptMeta.getUser();
        
        // 检查必要字段
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            return false;
        }
        
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否包含必要关键词
        String requiredKeywords = "SELECT";
        if (!systemPrompt.toUpperCase().contains(requiredKeywords) && 
            !userPrompt.toUpperCase().contains(requiredKeywords)) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getValidationReport() {
        return "BasicPromptValidator: Validates prompt existence, non-emptiness, and SQL keyword presence.";
    }
}

/**
 * 长度验证器 - 检查prompt长度是否在合理范围内
 */
class LengthPromptValidator implements PromptValidator {
    private static final int MIN_SYSTEM_LENGTH = 50;
    private static final int MAX_SYSTEM_LENGTH = 20000;
    private static final int MIN_USER_LENGTH = 20;
    private static final int MAX_USER_LENGTH = 50000;
    
    @Override
    public boolean validatePrompt(PromptMeta promptMeta) {
        if (promptMeta == null) {
            return false;
        }
        
        String systemPrompt = promptMeta.getSystem();
        String userPrompt = promptMeta.getUser();
        
        if (systemPrompt == null || userPrompt == null) {
            return false;
        }
        
        int systemLength = systemPrompt.length();
        int userLength = userPrompt.length();
        
        boolean systemValid = systemLength >= MIN_SYSTEM_LENGTH && systemLength <= MAX_SYSTEM_LENGTH;
        boolean userValid = userLength >= MIN_USER_LENGTH && userLength <= MAX_USER_LENGTH;
        
        return systemValid && userValid;
    }
    
    @Override
    public String getValidationReport() {
        return String.format(
            "LengthPromptValidator: Validates prompt lengths. System: %d-%d chars, User: %d-%d chars.",
            MIN_SYSTEM_LENGTH, MAX_SYSTEM_LENGTH, MIN_USER_LENGTH, MAX_USER_LENGTH
        );
    }
    
    public ValidationDetails getDetailedReport(PromptMeta promptMeta) {
        ValidationDetails details = new ValidationDetails();
        if (promptMeta != null && promptMeta.getSystem() != null && promptMeta.getUser() != null) {
            details.systemLength = promptMeta.getSystem().length();
            details.userLength = promptMeta.getUser().length();
            details.systemValid = details.systemLength >= MIN_SYSTEM_LENGTH && details.systemLength <= MAX_SYSTEM_LENGTH;
            details.userValid = details.userLength >= MIN_USER_LENGTH && details.userLength <= MAX_USER_LENGTH;
        }
        return details;
    }
    
    class ValidationDetails {
        int systemLength;
        int userLength;
        boolean systemValid;
        boolean userValid;
        
        @Override
        public String toString() {
            return String.format("System: %d chars (%s), User: %d chars (%s)",
                systemLength, systemValid ? "VALID" : "INVALID",
                userLength, userValid ? "VALID" : "INVALID");
        }
    }
}

/**
 * 内容验证器 - 检查prompt内容的质量
 */
class ContentPromptValidator implements PromptValidator {
    @Override
    public boolean validatePrompt(PromptMeta promptMeta) {
        if (promptMeta == null) {
            return false;
        }
        
        String systemPrompt = promptMeta.getSystem();
        String userPrompt = promptMeta.getUser();
        
        // 检查系统prompt是否包含必要指令
        boolean hasConstraints = containsRequiredConstraints(systemPrompt);
        
        // 检查用户prompt是否包含schema和question
        boolean hasSchema = userPrompt.contains("{schema}") || 
                           userPrompt.toLowerCase().contains("schema") ||
                           userPrompt.toLowerCase().contains("table");
        boolean hasQuestion = userPrompt.contains("{question}") || 
                             userPrompt.toLowerCase().contains("question") ||
                             userPrompt.toLowerCase().contains("query");
        
        // 检查JSON输出格式要求
        boolean hasJsonFormat = systemPrompt.contains("JSON") || 
                               systemPrompt.contains("json") ||
                               systemPrompt.contains("{") && systemPrompt.contains("}");
        
        return hasConstraints && hasSchema && hasQuestion && hasJsonFormat;
    }
    
    private boolean containsRequiredConstraints(String systemPrompt) {
        String upperPrompt = systemPrompt.toUpperCase();
        boolean hasSelect = upperPrompt.contains("SELECT");
        boolean hasNoDML = upperPrompt.contains("UPDATE") || upperPrompt.contains("DELETE") || 
                          upperPrompt.contains("DROP") || upperPrompt.contains("INSERT");
        // 注意：这里检查是否明确禁止DML操作
        boolean prohibitsDML = upperPrompt.contains("NO UPDATE") || 
                              upperPrompt.contains("NO DELETE") ||
                              upperPrompt.contains("ONLY SELECT");
        
        return hasSelect && (hasNoDML || prohibitsDML);
    }
    
    @Override
    public String getValidationReport() {
        return "ContentPromptValidator: Validates prompt content includes required elements (constraints, schema, question, JSON format).";
    }
    
    public QualityMetrics calculateQualityMetrics(PromptMeta promptMeta) {
        QualityMetrics metrics = new QualityMetrics();
        if (promptMeta != null && promptMeta.getSystem() != null && promptMeta.getUser() != null) {
            String system = promptMeta.getSystem();
            String user = promptMeta.getUser();
            
            // 计算约束完整性得分
            metrics.constraintScore = calculateConstraintScore(system);
            
            // 计算清晰度得分
            metrics.clarityScore = calculateClarityScore(system, user);
            
            // 计算完整性得分
            metrics.completenessScore = calculateCompletenessScore(user);
        }
        return metrics;
    }
    
    private int calculateConstraintScore(String systemPrompt) {
        int score = 0;
        String upper = systemPrompt.toUpperCase();
        if (upper.contains("SELECT")) score += 25;
        if (upper.contains("ONLY SELECT")) score += 25;
        if (upper.contains("NO UPDATE") || upper.contains("NO DELETE")) score += 25;
        if (upper.contains("JSON")) score += 25;
        return score;
    }
    
    private int calculateClarityScore(String systemPrompt, String userPrompt) {
        // 简单基于长度和结构的清晰度评估
        int score = 50; // 基础分
        
        // 系统prompt检查
        if (systemPrompt.contains("\n")) score += 10; // 有换行，结构清晰
        if (systemPrompt.contains(":")) score += 10; // 有冒号，说明清晰
        if (systemPrompt.split("\\.").length > 3) score += 10; // 句子较多
        
        // 用户prompt检查
        if (userPrompt.contains("\n")) score += 10;
        if (userPrompt.contains(":")) score += 10;
        
        return Math.min(score, 100);
    }
    
    private int calculateCompletenessScore(String userPrompt) {
        int score = 0;
        if (userPrompt.contains("{schema}") || userPrompt.contains("schema")) score += 50;
        if (userPrompt.contains("{question}") || userPrompt.contains("question")) score += 50;
        return score;
    }
    
    class QualityMetrics {
        int constraintScore;     // 约束完整性得分 0-100
        int clarityScore;        // 清晰度得分 0-100
        int completenessScore;   // 完整性得分 0-100
        
        public int getOverallScore() {
            return (constraintScore + clarityScore + completenessScore) / 3;
        }
        
        @Override
        public String toString() {
            return String.format("Quality Metrics - Constraints: %d/100, Clarity: %d/100, Completeness: %d/100, Overall: %d/100",
                constraintScore, clarityScore, completenessScore, getOverallScore());
        }
    }
}


// =============== 结束新增接口和抽象层 ===============

@Slf4j
@Component
public class PromptManager {
    
    // 从配置文件读取（现在这些配置是可选的，因为主要使用llmService）
    @Value("${llm.api.key:}")
    private String apiKey;
    
    @Value("${llm.api.url:}")
    private String apiUrl;
    
    @Value("${llm.model:}")
    private String model;
   
    @Value("${prompt.manager.backup.api.url:${llm.api.url:}}")
    private String backupApiUrl;
    
    @Value("${prompt.manager.retry.count:3}")
    private int retryCount;
    
    @Value("${prompt.manager.enable.detailed.logging:false}")
    private boolean enableDetailedLogging;
    
    @Value("${prompt.manager.version.identifier:v1.0.0-stable}")
    private String versionIdentifier;
    
    @Value("${prompt.manager.feature.flags:}")
    private String featureFlags;
  

    // ==========新增策略相关字段===========
    private static final int MAX_SCHEMA_LENGTH = 20000;
    private static final int MIN_SCHEMA_LENGTH = 10;
    private PromptGenerationStrategy currentStrategy;
    private List<PromptValidator> validators;
    private PromptTemplateType currentTemplateType;
    // ================================

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
 
    /**
     * 生成prompt（可选模板）
     */
    @PostConstruct
    public void init() {
        // 默认使用标准策略
        this.currentStrategy = new StandardPromptStrategy();
        this.currentTemplateType = PromptTemplateType.STANDARD;
        
        // 初始化验证器
        this.validators = new ArrayList<>();
        this.validators.add(new BasicPromptValidator());
        this.validators.add(new LengthPromptValidator());
        this.validators.add(new ContentPromptValidator());
    }
    
    /**
     * 生成prompt - 使用策略模式
     */
    public PromptMeta generatePrompt(String question, String schemaText) {
        this.currentTemplateType = getRecommendedTemplateType(schemaText);
        System.out.println("推荐prompt模板类型: " + this.currentTemplateType.getDescription());
        return generatePrompt(question, schemaText, this.currentTemplateType);
    }
    
    /**
     * 使用指定模板类型生成prompt
     */
    public PromptMeta generatePrompt(String question, String schemaText, PromptTemplateType templateType) {
        validateSchemaLength(schemaText);
        String systemPrompt = templateType.generateSystemPrompt();
        String userPrompt = templateType.generateUserPrompt(schemaText, question);

        // =============== 添加中间处理 ===============
        String normalizedSchema = normalizeSchemaText(schemaText);
        String schemaComplexity = estimateSchemaComplexity(normalizedSchema);
        // =============== 结束新增中间处理 ===============

        PromptMeta meta = new PromptMeta();
        meta.setSystem(systemPrompt);
        meta.setUser(userPrompt);
        meta.setTemplateVersion(templateType.getVersion());
        meta.setSchemaHash(hashSchema(schemaText));
        meta.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        meta.setTemplateType(templateType.name());
        // =============== 添加元数据 ===============
        meta.setSchemaComplexity(schemaComplexity);
        meta.setManagerVersion(versionIdentifier);
        // =============== 结束新增元数据 ===============

        // 验证prompt
        boolean isValid = validatePrompt(meta);
        meta.setValid(isValid);
        
        if (!isValid) {
            log.warn("Generated prompt failed validation: {}", getValidationReport(meta));
        }
        
        return meta;
    }
    
    /**
     * 使用策略生成prompt
     */
    public PromptMeta generatePromptWithStrategy(String question, String schemaText, PromptGenerationStrategy strategy) {
        String systemPrompt = strategy.generateSystemPrompt();
        String userPrompt = strategy.generateUserPrompt(schemaText, question);
        
        PromptMeta meta = new PromptMeta();
        meta.setSystem(systemPrompt);
        meta.setUser(userPrompt);
        meta.setTemplateVersion("custom");
        meta.setSchemaHash(hashSchema(schemaText));
        meta.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        meta.setStrategy(strategy.getStrategyName());
        
        return meta;
    }
    
    /**
     * 验证prompt
     */
    public boolean validatePrompt(PromptMeta promptMeta) {
        for (PromptValidator validator : validators) {
            if (!validator.validatePrompt(promptMeta)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 获取详细的验证报告
     */
    public String getValidationReport(PromptMeta promptMeta) {
        StringBuilder report = new StringBuilder("Prompt Validation Report:\n");
        
        for (PromptValidator validator : validators) {
            boolean isValid = validator.validatePrompt(promptMeta);
            report.append("- ").append(validator.getValidationReport())
                  .append(": ").append(isValid ? "PASS" : "FAIL").append("\n");
        }
        
        // 添加详细的验证信息
        if (validators.get(1) instanceof LengthPromptValidator) {
            LengthPromptValidator.ValidationDetails details = 
                ((LengthPromptValidator) validators.get(1)).getDetailedReport(promptMeta);
            report.append("Length Details: ").append(details.toString()).append("\n");
        }
        
        if (validators.get(2) instanceof ContentPromptValidator) {
            ContentPromptValidator.QualityMetrics metrics = 
                ((ContentPromptValidator) validators.get(2)).calculateQualityMetrics(promptMeta);
            report.append("Quality Metrics: ").append(metrics.toString()).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * 切换模板类型
     */
    public void switchTemplateType(PromptTemplateType templateType) {
        this.currentTemplateType = templateType;
        log.info("Switched to template type: {}", templateType.getDescription());
    }
    
    /**
     * 切换策略
     */
    public void switchStrategy(PromptGenerationStrategy strategy) {
        this.currentStrategy = strategy;
        log.info("Switched to strategy: {}", strategy.getStrategyName());
    }

    
    /**
     * 基于schema复杂度获取推荐的模板类型
     */
    public PromptTemplateType getRecommendedTemplateType(String schemaText) {
        
        String normalizedSchema = normalizeSchemaText(schemaText);
        String schemaComplexity = estimateSchemaComplexity(normalizedSchema);
        if (schemaComplexity.equals("SIMPLE")) {
            return PromptTemplateType.STANDARD;
        } else if (schemaComplexity.equals("MEDIUM")) {
            return PromptTemplateType.ENHANCED;
        } else {
            return PromptTemplateType.EXPERIMENTAL;
        }
    }
    

    /**
     * 调用大模型API
     */
    public String callGPT(PromptMeta promptMeta) {

        try {
            // 构建请求
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", 0);
            requestBody.put("max_tokens", 1000);
            
           
           
            
            com.fasterxml.jackson.databind.node.ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", promptMeta.getSystem());
            messages.add(systemMessage);
            
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", promptMeta.getUser());
            messages.add(userMessage);
            
            requestBody.set("messages", messages);
            
            // 设置请求头
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + apiKey);
            
       
            
            org.springframework.http.HttpEntity<String> request = 
                new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);
            
            // 发送请求
            String response = restTemplate.postForObject(apiUrl, request, String.class);
            ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response);
            
            String answer = responseJson
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();
            

        
            
            promptMeta.setResponse(answer);
            savePromptArchive(promptMeta);
            
            
            return answer;
            
        } catch (Exception e) {
            log.error("调用大模型API失败", e);
            handleApiCallFailure(e);
            return "{\"sql\": \"\", \"thought\": \"调用大模型失败: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 解析GPT返回的JSON，提取SQL
     */
    public String extractSqlFromResponse(String gptResponse) {
        preExtractionValidation(gptResponse);
      
        try {
            ObjectNode jsonResponse = (ObjectNode) objectMapper.readTree(gptResponse);
            String sql = jsonResponse.path("sql").asText();
            
            // =============== 添加后处理 ===============
            sql = SqlManager.postProcessExtractedSql(sql);
            SqlManager.validateExtractedSql(sql);
            // =============== 结束后处理 ===============
            
            return sql;
        } catch (Exception e) {
            log.warn("无法解析GPT返回的JSON，尝试直接提取", e);
            // 简单查找SQL语句（备用方案）
            String sql = extractSqlByRegex(gptResponse);
            
            return sql;
        }
    }
    
    private String extractSqlByRegex(String text) {
        // 简单正则匹配SELECT语句
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "SELECT.*?(?:;|$)", 
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }
        return "";
    }
    
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * 保存prompt存档
     */
    private void savePromptArchive(PromptMeta promptMeta) {
        try {
            String archivePath = "prompt_archive.jsonl";
            String line = objectMapper.writeValueAsString(promptMeta);
            java.nio.file.Files.write(
                java.nio.file.Paths.get(archivePath),
                (line + "\n").getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
            
        } catch (Exception e) {
            log.error("保存prompt存档失败", e);
        }
    }
    
//==============schema处理相关方法================
    /**
     * 计算schema哈希
     */
    private String hashSchema(String schemaText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(schemaText.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).substring(0, 16);
        } catch (Exception e) {
            return "hash_error";
        }
    }

    /**
     * 验证schema长度
     */
    private void validateSchemaLength(String schemaText) {
        int length = schemaText.length();
        if (length > PromptManager.MAX_SCHEMA_LENGTH) {
            log.debug("Schema length exceeds maximum recommended length");
        } else if (length < PromptManager.MIN_SCHEMA_LENGTH) {
            log.debug("Schema length is below minimum recommended length");
        }
    return;
    }
     
    /**
     * 规范化schema文本
     */
    private String normalizeSchemaText(String schemaText) {
        if (schemaText == null || schemaText.trim().isEmpty()) {
            return "";
        }
        String result = schemaText.trim();
        
        // 1. 统一换行符
        result = result.replace("\r\n", "\n").replace("\r", "\n");
        // 2. 去除多余的空格和制表符
        result = result.replaceAll("[ \\t]+", " ");
        result = result.replaceAll("\n[ \\t]+\n", "\n\n");
        // 3. 格式化CREATE TABLE语句
        String[] lines = result.split("\n");
        StringBuilder formatted = new StringBuilder();
        boolean inCreateTable = false;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.toUpperCase().startsWith("CREATE TABLE")) {
                inCreateTable = true;
                formatted.append(trimmedLine).append("\n");
            } else if (inCreateTable && trimmedLine.endsWith(");")) {
                formatted.append("    ").append(trimmedLine).append("\n");
                inCreateTable = false;
            } else if (inCreateTable && !trimmedLine.isEmpty()) {
                // 缩进字段定义
                formatted.append("    ").append(trimmedLine).append("\n");
            } else {
                formatted.append(trimmedLine).append("\n");
            }
        } 
        // 4. 移除最后的换行符
        if (formatted.length() > 0 && formatted.charAt(formatted.length() - 1) == '\n') {
            formatted.deleteCharAt(formatted.length() - 1);
        }   
        return formatted.toString();
    }
    
    /**
     * 估算schema复杂度
     */
    private String estimateSchemaComplexity(String schemaText) {
        int tableCount = (int) java.util.regex.Pattern.compile("CREATE TABLE", 
            java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(schemaText)
            .results()
            .count();
        if (tableCount <= 10) {
            return "LOW";
        } else if (tableCount <= 16) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    
    }
    
//=====================API调用==========================

    /**
     * API调用后清理
     */
    private void postApiCallCleanup() {
        long cleanupStartTime = System.currentTimeMillis();
        try {
        
            long cleanupTime = System.currentTimeMillis() - cleanupStartTime;
            if (enableDetailedLogging) {
                log.debug("Post-API-call cleanup completed in {} ms", cleanupTime);
                
                // 记录内存信息
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                long maxMemory = runtime.maxMemory();
                log.debug("Memory usage: {}/{} MB ({}%)", 
                    usedMemory / 1024 / 1024,
                    maxMemory / 1024 / 1024,
                    (usedMemory * 100) / maxMemory);
            }
        } catch (Exception e) {
            log.warn("Error during post-API-call cleanup", e);
        }
    }
    
    /**
     * 记录API调用失败
     */
    private void handleApiCallFailure(Exception e) {
        if (enableDetailedLogging) {
            log.debug("Handling API call failure, exception type: {}", e.getClass().getSimpleName());
        }
    }
    
    /**
     * 提取GPT返回的JSON中SQL前验证非空
     */
    private void preExtractionValidation(String gptResponse) {
        boolean isEmpty = gptResponse == null || gptResponse.trim().isEmpty();
        if (isEmpty) {
            log.debug("LLM response is empty before extraction");
        }
    }
    
    
}