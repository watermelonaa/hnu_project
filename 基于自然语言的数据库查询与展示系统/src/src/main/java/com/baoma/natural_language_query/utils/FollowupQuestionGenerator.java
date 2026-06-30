/**
 * FollowupQuestionGenerator.java
 *
 * 功能：调用大模型，根据当前用户问题、数据库 Schema 及可选的查询结果，
 *       自动生成最多 maxQuestions 条后续探索性问题，帮助用户继续深度分析数据。
 *
 * 主要流程：
 *  1. 启用开关(enabled)判断是否需要生成；
 *  2. 构造包含 Schema、当前问题、查询结果摘要的提示词；
 *  3. 通过 REST 调用 LLM 接口获取推荐问题(JSON 数组格式)；
 *  4. 解析并返回 List<String>；若失败则触发降级策略。
 *
 * 降级策略：
 *  - 解析失败时，用正则提取问号结尾的句子；
 *  - LLM 异常时，根据关键词模板返回通用/领域相关探索问题。
 */
package com.baoma.natural_language_query.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class FollowupQuestionGenerator {

    /** LLM 接口密钥 */
    @Value("${llm.api.key:}")
    private String apiKey;

    /** LLM 接口地址 */
    @Value("${llm.api.url:}")
    private String apiUrl;

    /** 指定模型*/
    @Value("${llm.model:}")
    private String model;

    /** 功能总开关，默认开启 */
    @Value("${followup.enabled:true}")
    private boolean enabled;

    /** 最多返回几条后续问题，默认 3 */
    @Value("${followup.max.questions:3}")
    private int maxQuestions;

    // --------------------- 工具实例 ---------------------
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===================== 对外接口 =====================

    /**
     * 生成后续探索问题（带查询结果版）
     *
     * @param currentQuestion 用户当前提出的问题
     * @param schemaText      数据库 Schema 文本
     * @param sqlResult       当前 SQL 查询结果（可选），用于提高问题相关性
     * @return 推荐的后续问题列表，长度不超过 maxQuestions；若禁用或异常则返回空列表
     */
    public List<String> generateFollowupQuestions(String currentQuestion,
                                                  String schemaText,
                                                  String sqlResult) {
        if (!enabled) {
            return new ArrayList<>();
        }

        try {
            String prompt = buildPrompt(currentQuestion, schemaText, sqlResult);
            String response = callLLM(prompt);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("生成后续问题失败", e);
            return generateFallbackQuestions(currentQuestion);
        }
    }

    /**
     * 生成后续探索问题（无查询结果简化版）
     *
     * @param currentQuestion 用户当前问题
     * @param schemaText      数据库 Schema
     * @return 推荐的后续问题列表
     */
    public List<String> generateFollowupQuestions(String currentQuestion, String schemaText) {
        return generateFollowupQuestions(currentQuestion, schemaText, null);
    }

    // ===================== 内部实现 =====================

    /**
     * 构造 LLM 提示词
     */
    private String buildPrompt(String currentQuestion, String schemaText, String sqlResult) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a data analysis assistant. Given the following context:\n\n");

        // 1. 数据库 Schema（过长则截断）
        prompt.append("DATABASE SCHEMA:\n```sql\n")
                .append(schemaText.length() > 2000 ? schemaText.substring(0, 2000) + "..." : schemaText)
                .append("\n```\n\n");

        // 2. 当前问题
        prompt.append("CURRENT USER QUESTION:\n")
                .append(currentQuestion)
                .append("\n\n");

        // 3. 若存在查询结果，提供摘要
        if (sqlResult != null && !sqlResult.trim().isEmpty()) {
            prompt.append("QUERY RESULT (first few rows):\n")
                    .append(getResultSummary(sqlResult))
                    .append("\n\n");
        }

        // 4. 任务指令
        prompt.append("TASK: Generate ").append(maxQuestions)
                .append(" relevant follow-up questions that a user might ask to explore the data further.\n")
                .append("The questions should be:\n")
                .append("1. Directly related to the current question and database schema\n")
                .append("2. Cover different analytical angles (trends, comparisons, details, etc.)\n")
                .append("3. Natural and conversational\n")
                .append("4. Useful for deeper data exploration\n")
                .append("5. Must not be identical or semantically similar to the current question.\\n")
                .append("6. Ensure that every follow-up question can be answered with a single SELECT statement against the current database.\n\n")
                .append("OUTPUT FORMAT: Return a JSON array of strings:\n")
                .append("[\"question1\", \"question2\", \"question3\"]\n")
                .append("Only return the JSON array, no other text.");

        return prompt.toString();
    }

    /**
     * 对查询结果做长度截断，避免提示词超限
     */
    private String getResultSummary(String sqlResult) {
        if (sqlResult.length() > 500) {
            return sqlResult.substring(0, 500) + "...\n(truncated for brevity)";
        }
        return sqlResult;
    }

    /**
     * 调用 LLM 接口获取推荐问题
     */
    private String callLLM(String prompt) {
        try {
            // 构造请求体（兼容 OpenAI 风格）
            com.fasterxml.jackson.databind.node.ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);  // 稍高温度增加多样性
            requestBody.put("max_tokens", 500);

            com.fasterxml.jackson.databind.node.ArrayNode messages = objectMapper.createArrayNode();
            com.fasterxml.jackson.databind.node.ObjectNode message = objectMapper.createObjectNode();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBody.set("messages", messages);

            // 设置请求头
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + apiKey);

            org.springframework.http.HttpEntity<String> request =
                    new org.springframework.http.HttpEntity<>(requestBody.toString(), headers);

            // 发送请求并提取回复内容
            String response = restTemplate.postForObject(apiUrl, request, String.class);
            com.fasterxml.jackson.databind.node.ObjectNode responseJson =
                    (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(response);

            return responseJson
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (Exception e) {
            log.error("调用LLM生成后续问题失败", e);
            throw new RuntimeException("生成后续问题失败", e);
        }
    }

    /**
     * 将 LLM 返回解析为 List<String>
     */
    private List<String> parseResponse(String response) {
        try {
            // 期望格式：["q1", "q2", ...]
            List<String> questions = objectMapper.readValue(
                    response,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            // 添加打印：在终端依次打印推荐问题
            System.out.println("=== 生成的推荐问题 ===");
            for (int i = 0; i < questions.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, questions.get(i));
            }
            System.out.println("=====================");

            return questions;

        } catch (Exception e) {
            System.out.println("无法解析LLM响应为JSON，尝试文本提取");
            return extractQuestionsFromText(response);
        }
    }

    /**
     * 备用方法：从文本提取问题
     */
    private List<String> extractQuestionsFromText(String text) {
        List<String> questions = new ArrayList<>();

        // 简单的基于标点符号的提取
        String[] lines = text.split("[\\n.!?]");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("1.") || trimmed.startsWith("2.") || trimmed.startsWith("3.")) {
                // 移除编号
                trimmed = trimmed.replaceAll("^\\d+\\.\\s*", "");
            }

            if (trimmed.endsWith("?") && trimmed.length() > 10) {
                questions.add(trimmed);
            }
        }

        // 限制数量
        List<String> result = questions.subList(0, Math.min(questions.size(), maxQuestions));

        // 添加打印（如果在备用路径中也希望打印）
        if (!result.isEmpty()) {
            log.info("=== 从文本提取的推荐问题 ===");
            for (int i = 0; i < result.size(); i++) {
                log.info("{}. {}", i + 1, result.get(i));
            }
            log.info("===========================");
        }

        return result;
    }


    /**
     * 降级策略：基于关键词生成通用/领域相关探索问题
     */
    private List<String> generateFallbackQuestions(String currentQuestion) {
        List<String> questions = new ArrayList<>();

        String lowerQuestion = currentQuestion.toLowerCase();

        // 1. 电影相关查询
        if (lowerQuestion.contains("电影") || lowerQuestion.contains("film") || lowerQuestion.contains("movie")) {
            questions.add("租赁次数最多的10部电影是哪些？");
            questions.add("收入最高的10部电影是哪些？");
            questions.add("评分最高的10部电影是哪些？");
            questions.add("库存最多的10部电影是哪些？");
            questions.add("哪些类型的电影最受欢迎？");
            questions.add("按发行年份统计电影数量？");
            questions.add("电影的平均时长是多少？");
            questions.add("时长与评分有什么关系？");
            questions.add("不同评级的电影数量各有多少？");
            questions.add("哪些电影的库存量为0？");
        }
        // 2. 演员相关查询
        else if (lowerQuestion.contains("演员") || lowerQuestion.contains("actor") || lowerQuestion.contains("actress")) {
            questions.add("出演电影最多的10位演员是谁？");
            questions.add("哪些演员在动作片中出演最多？");
            questions.add("同一部电影中合作最多的演员组合是哪些？");
            questions.add("演员的姓氏分布情况如何？");
            questions.add("每年新增演员数量有多少？");
            questions.add("哪些演员出现在评分最高的10部电影中？");
            questions.add("演员参演电影的评分平均值排名？");
            questions.add("哪些演员参演的电影租赁次数最多？");
            questions.add("演员参演电影类型的多样性排名？");
            questions.add("姓'李'的演员有哪些？");
        }
        // 3. 客户相关查询
        else if (lowerQuestion.contains("客户") || lowerQuestion.contains("customer") || lowerQuestion.contains("租客")) {
            questions.add("消费金额最高的10位客户是谁？");
            questions.add("租赁次数最多的10位客户是谁？");
            questions.add("客户的主要分布城市是哪些？");
            questions.add("活跃客户和不活跃客户的比例是多少？");
            questions.add("哪些城市客户消费金额最高？");
            questions.add("客户的租赁偏好电影类型是什么？");
            questions.add("哪些客户租过特定电影？（如：ACADEMY DINOSAUR）");
            questions.add("客户的平均租赁次数是多少？");
            questions.add("新客户和老客户的消费对比？");
            questions.add("客户租赁电影的平均评分是多少？");
        }
        // 4. 租赁相关查询
        else if (lowerQuestion.contains("租赁") || lowerQuestion.contains("rental") || lowerQuestion.contains("出租")) {
            questions.add("每日租赁数量最高的10天是哪些？");
            questions.add("每月租赁数量趋势是怎样的？");
            questions.add("一天中哪个时间段租赁最频繁？");
            questions.add("平均租赁时长是多少天？");
            questions.add("逾期归还的租赁比例是多少？");
            questions.add("哪个店铺的租赁业务最好？");
            questions.add("周末和工作日的租赁对比？");
            questions.add("租赁次数最多的月份是哪个？");
            questions.add("同一客户连续租赁的记录有哪些？");
            questions.add("租赁收入按月份的变化趋势？");
        }
        // 5. 支付相关查询
        else if (lowerQuestion.contains("支付") || lowerQuestion.contains("payment") || lowerQuestion.contains("收入") || lowerQuestion.contains("revenue")) {
            questions.add("每日收入最高的10天是哪些？");
            questions.add("月收入趋势是怎样的？");
            questions.add("收入最高的店铺是哪个？");
            questions.add("平均每笔支付的金额是多少？");
            questions.add("哪种支付方式使用最频繁？");
            questions.add("收入最高的电影类型是哪些？");
            questions.add("逾期费用的总收入有多少？");
            questions.add("客户平均消费金额是多少？");
            questions.add("收入最高的员工是谁？");
            questions.add("按季度统计收入情况？");
        }
        // 6. 分类相关查询
        else if (lowerQuestion.contains("分类") || lowerQuestion.contains("category") || lowerQuestion.contains("类型") || lowerQuestion.contains("genre")) {
            questions.add("电影分类的数量分布情况？");
            questions.add("最受欢迎的电影分类排名？");
            questions.add("各分类电影的平均评分？");
            questions.add("各分类的租赁收入排名？");
            questions.add("分类与电影时长的关系？");
            questions.add("哪个分类的库存量最大？");
            questions.add("客户最常租赁的分类是哪些？");
            questions.add("高评分电影集中在哪些分类？");
            questions.add("分类与租赁次数的关系？");
            questions.add("每个分类的代表电影有哪些？");
        }
        // 7. 时长相关查询
        else if (lowerQuestion.contains("时长") || lowerQuestion.contains("length") || lowerQuestion.contains("时间长度")) {
            questions.add("电影的平均时长是多少分钟？");
            questions.add("不同时长区间的电影数量分布？");
            questions.add("时长与评分的关系分析？");
            questions.add("最受欢迎的电影时长区间？");
            questions.add("不同分类电影的平均时长对比？");
            questions.add("时长与租赁次数的关系？");
            questions.add("超长电影（>120分钟）有哪些？");
            questions.add("短片（<60分钟）有哪些？");
            questions.add("时长与库存量的关系？");
            questions.add("时长与收入的关系？");
        }
        // 8. 店铺相关查询
        else if (lowerQuestion.contains("店铺") || lowerQuestion.contains("store") || lowerQuestion.contains("shop")) {
            questions.add("哪个店铺的业绩最好？");
            questions.add("各店铺的员工配置情况？");
            questions.add("店铺间的客户数量对比？");
            questions.add("店铺的库存充足率对比？");
            questions.add("店铺的客户满意度对比？");
            questions.add("店铺的地域分布情况？");
            questions.add("店铺的租赁次数排名？");
            questions.add("店铺的收入变化趋势？");
            questions.add("店铺的客户平均消费对比？");
            questions.add("店铺的活跃客户比例？");
        }
        // 9. 员工相关查询
        else if (lowerQuestion.contains("员工") || lowerQuestion.contains("staff") || lowerQuestion.contains("职员")) {
            questions.add("员工的服务客户数量排名？");
            questions.add("员工的租赁业绩对比？");
            questions.add("员工的工作时长统计？");
            questions.add("新老员工的比例是多少？");
            questions.add("员工管理的客户消费排名？");
            questions.add("员工处理的租赁数量排名？");
            questions.add("员工的平均客户评分？");
            questions.add("员工负责店铺的业绩对比？");
            questions.add("员工的工作效率分析？");
            questions.add("员工的客户留存率？");
        }
        // 10. 库存相关查询
        else if (lowerQuestion.contains("库存") || lowerQuestion.contains("inventory") || lowerQuestion.contains("存货")) {
            questions.add("库存最多的10部电影是哪些？");
            questions.add("各店铺的库存分布情况？");
            questions.add("库存周转率最高的电影？");
            questions.add("库存不足（少于5份）的电影？");
            questions.add("最畅销电影的库存变化？");
            questions.add("库存与租赁次数的关系？");
            questions.add("各分类的库存量对比？");
            questions.add("高库存低租赁的电影？");
            questions.add("库存的地理分布情况？");
            questions.add("库存的月度变化趋势？");
        }
        // 通用问题（针对任何查询）
        else {
            questions.add("租赁次数最多的10部电影是哪些？");
            questions.add("收入最高的10部电影是哪些？");
            questions.add("出演电影最多的10位演员是谁？");
            questions.add("消费金额最高的10位客户是谁？");
            questions.add("评分最高的10部电影是哪些？");
            questions.add("库存最多的10部电影是哪些？");
            questions.add("租赁数量最高的10天是哪些？");
            questions.add("收入最高的10天是哪些？");
            questions.add("评分最高的10部动作片是哪些？");
            questions.add("消费金额最高的10个城市是哪些？");
            questions.add("库存最多的10个店铺是哪些？");
        }
        // 随机选择，避免总是返回相同的问题
        Collections.shuffle(questions);
        // 返回指定数量的问题，但至少返回一个
        return questions.subList(0, Math.min(Math.max(questions.size(), 1), maxQuestions));
    }


}