package com.baoma.natural_language_query.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baoma.natural_language_query.dto.QueryRequestDTO;
import com.baoma.natural_language_query.dto.RecommendationRequestDTO; // 【新增导入】
import com.baoma.natural_language_query.entity.mongodb.DialogRecord;
import com.baoma.natural_language_query.entity.mongodb.DialogDetail; // 【新增导入】
import com.baoma.natural_language_query.entity.mysql.DbConnection;
import com.baoma.natural_language_query.entity.mysql.QueryLog; // 【新增导入】
import com.baoma.natural_language_query.entity.mysql.LlmConfig;
import com.baoma.natural_language_query.entity.mysql.OperationLog;
import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.repository.DialogRecordRepository;
import com.baoma.natural_language_query.repository.DialogDetailRepository; // 【新增导入】
import com.baoma.natural_language_query.service.DbConnectionService;
import com.baoma.natural_language_query.service.LLMService;
import com.baoma.natural_language_query.service.LlmConfigService;
import com.baoma.natural_language_query.service.OperationLogService;
import com.baoma.natural_language_query.service.QueryService;
import com.baoma.natural_language_query.service.TablePermissionService;
import com.baoma.natural_language_query.service.UserService;
import com.baoma.natural_language_query.service.ConversationContextService; // 【新增导入】
import com.baoma.natural_language_query.service.QueryLogService; // 【新增导入】
import com.baoma.natural_language_query.utils.DynamicDatabaseExecutor;
import com.baoma.natural_language_query.utils.FollowupQuestionGenerator; // 【新增导入】
import com.baoma.natural_language_query.utils.PromptManager;
import com.baoma.natural_language_query.utils.PromptMeta; // 【新增导入】
import com.baoma.natural_language_query.utils.PromptTemplateType; // 【新增导入】
import com.baoma.natural_language_query.vo.ChartDataVO;
import com.baoma.natural_language_query.vo.DatasetVO;
import com.baoma.natural_language_query.vo.QueryResponseVO;
import com.baoma.natural_language_query.vo.TableDataVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture; // 【新增导入】
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QueryServiceImpl implements QueryService {

    @Autowired private DialogRecordRepository dialogRecordRepository;
    @Autowired private DialogDetailRepository dialogDetailRepository; // 【新增注入】
    @Autowired private LLMService llmService;
    @Autowired private DbConnectionService dbConnectionService;
    @Autowired private DynamicDatabaseExecutor databaseExecutor;
    @Autowired private TablePermissionService tablePermissionService;
    @Autowired private OperationLogService operationLogService;
    @Autowired private LlmConfigService llmConfigService;
    @Autowired private UserService userService;
    @Autowired private PromptManager promptManager;
    @Autowired private FollowupQuestionGenerator followupQuestionGenerator; // 【新增注入】
    @Autowired private ConversationContextService conversationContextService; // 【新增注入】
    @Autowired private QueryLogService queryLogService; // 【新增注入】

    @Override
    public QueryResponseVO executeQuery(QueryRequestDTO request, Long userId) {
        long startTime = System.currentTimeMillis();

        // 验证必要参数
        if (request.getDbConnectionId() == null) {
            throw new RuntimeException("数据库连接ID不能为空");
        }

        // 获取数据库连接配置
        DbConnection dbConnection = dbConnectionService.getById(request.getDbConnectionId());
        if (dbConnection == null) {
            throw new RuntimeException("数据库连接不存在，ID: " + request.getDbConnectionId());
        }
        if ("disabled".equals(dbConnection.getStatus())) {
            throw new RuntimeException("该数据库连接已被禁用");
        }

        System.out.println("=== 查询执行开始 ===");
        System.out.println("数据库连接: " + dbConnection.getName() + " (ID: " + dbConnection.getId() + ")");
        System.out.println("用户提示: " + request.getUserPrompt());

        // ============== 【新增】定义后续问题列表 ==============
        List<String> followupQuestions = new ArrayList<>();

        // ============== 数据库Schema读取 ==============
        String databaseSchemaJson = null;
        try {
            System.out.println("开始读取数据库schema...");

            // 获取表结构信息
            List<DynamicDatabaseExecutor.TableSchema> tableSchemas =
                    databaseExecutor.getDatabaseSchema(dbConnection);

            // 转换为JSON格式
            databaseSchemaJson = convertTableSchemasToJson(tableSchemas);

            System.out.println("✓ 数据库Schema读取成功，包含 " + tableSchemas.size() + " 个表");

        } catch (Exception e) {
            System.err.println("⚠ 数据库Schema读取失败: " + e.getMessage());
            e.printStackTrace();
        }

        //==============多轮对话支持=====================
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = "conv_" + UUID.randomUUID().toString().substring(0, 8);
        }

        // 保存对话记录到 MongoDB
        try {
            if (request.getConversationId() == null || request.getConversationId().isEmpty()) {
                DialogRecord dialogRecord = new DialogRecord();
                dialogRecord.setDialogId(conversationId);
                dialogRecord.setUserId(userId);
                dialogRecord.setTopic(
                        request.getUserPrompt().substring(0, Math.min(20, request.getUserPrompt().length())));
                dialogRecord.setTotalRounds(1);
                dialogRecord.setStartTime(LocalDateTime.now());
                dialogRecord.setLastTime(LocalDateTime.now());

                System.out.println("准备保存新对话记录: dialogId=" + conversationId + ", userId=" + userId);
                DialogRecord saved = dialogRecordRepository.save(dialogRecord);
                System.out.println("✓ 创建新对话记录成功: " + conversationId + ", MongoDB ID: " + saved.getId());
            } else {
                DialogRecord dialogRecord = dialogRecordRepository.findByDialogId(conversationId);
                if (dialogRecord != null) {
                    dialogRecord.setTotalRounds(dialogRecord.getTotalRounds() + 1);
                    dialogRecord.setLastTime(LocalDateTime.now());
                    System.out.println("准备更新对话记录: dialogId=" + conversationId + ", 新轮次=" + dialogRecord.getTotalRounds());
                    DialogRecord saved = dialogRecordRepository.save(dialogRecord);
                    System.out.println(
                            "✓ 更新对话记录成功: " + conversationId + " (轮次: " + dialogRecord.getTotalRounds() + "), MongoDB ID: " + saved.getId());
                } else {
                    System.out.println("警告: 对话记录不存在，dialogId=" + conversationId + "，将创建新记录");
                    DialogRecord newDialogRecord = new DialogRecord();
                    newDialogRecord.setDialogId(conversationId);
                    newDialogRecord.setUserId(userId);
                    newDialogRecord.setTopic(
                            request.getUserPrompt().substring(0, Math.min(20, request.getUserPrompt().length())));
                    newDialogRecord.setTotalRounds(1);
                    newDialogRecord.setStartTime(LocalDateTime.now());
                    newDialogRecord.setLastTime(LocalDateTime.now());
                    DialogRecord saved = dialogRecordRepository.save(newDialogRecord);
                    System.out.println("✓ 创建新对话记录（替代）: " + conversationId + ", MongoDB ID: " + saved.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("✗ 保存对话记录失败: " + e.getMessage());
            e.printStackTrace();
        }

        //====================调用大模型API生成SQL========================
        String generatedSql = "";
        String modelThought = "";
        Map<String, Object> llmResult = new HashMap<>();
        String databaseName = request.getDatabase() != null ? request.getDatabase() : dbConnection.getName();

        try {
            if (databaseSchemaJson != null && !databaseSchemaJson.isEmpty()) {
                System.out.println("✓ 数据库Schema读取成功");

                // 检查是否有对话历史
                boolean hasHistory = conversationContextService.hasHistory(conversationId);
                System.out.println("对话历史检查: " + (hasHistory ? "存在历史记录" : "新对话"));

                if (hasHistory) {
                    // ============== 多轮对话：使用上下文感知模式 ==============
                    System.out.println("使用上下文感知模式生成Prompt...");
                    
                    // 生成上下文感知的Prompt（使用反射或直接指定模板名称）
                    // 由于PromptTemplateType不是public，使用generatePrompt的默认行为
                    com.baoma.natural_language_query.utils.PromptMeta promptMeta = promptManager.generatePrompt(
                            request.getUserPrompt(),
                            databaseSchemaJson
                    );

                    // 获取对话历史（最近5轮）
                    List<Map<String, String>> conversationHistory = 
                            conversationContextService.getConversationHistory(conversationId, 5);
                    
                    System.out.println("读取到 " + conversationHistory.size() + " 条历史消息");

                    // 构建完整的消息列表：system + 历史对话 + 当前用户问题
                    List<Map<String, String>> messages = new ArrayList<>();
                    
                    // 1. 添加系统消息
                    Map<String, String> systemMessage = new HashMap<>();
                    systemMessage.put("role", "system");
                    systemMessage.put("content", promptMeta.getSystem());
                    messages.add(systemMessage);
                    
                    // 2. 添加对话历史
                    messages.addAll(conversationHistory);
                    
                    // 3. 添加当前用户问题
                    Map<String, String> userMessage = new HashMap<>();
                    userMessage.put("role", "user");
                    userMessage.put("content", promptMeta.getUser());
                    messages.add(userMessage);

                    System.out.println("准备调用大模型，消息总数: " + messages.size());

                    // 调用大模型API（包含历史上下文）
                    Map<String, Object> llmResponse = llmService.generateQueryWithConversationHistory(
                            messages,
                            request.getModel()
                    );

                    String gptResponse = (String) llmResponse.get("rawResponse");
                    if (gptResponse == null || gptResponse.isEmpty()) {
                        throw new RuntimeException("大模型返回内容为空");
                    }

                    // 提取SQL和thought
                    generatedSql = promptManager.extractSqlFromResponse(gptResponse);
                    modelThought = extractThoughtFromResponse(gptResponse);

                    llmResult.put("sqlQuery", generatedSql);
                    llmResult.put("thought", modelThought);

                    if (!generatedSql.isEmpty()) {
                        System.out.println("✓ 大模型生成 SQL（上下文感知）: " + generatedSql);
                        System.out.println("✓ 模型思考过程: " + modelThought);
                    } else {
                        throw new RuntimeException("大模型未生成有效的SQL语句");
                    }
                } else {
                    // ============== 首轮对话：使用标准模式 ==============
                    System.out.println("使用标准模式生成Prompt...");
                    
                    // 生成Prompt
                    com.baoma.natural_language_query.utils.PromptMeta promptMeta = promptManager.generatePrompt(
                            request.getUserPrompt(),
                            databaseSchemaJson
                    );

                    // 调用大模型API
                    System.out.println("正在调用大模型API...");
                    Map<String, Object> llmResponse = llmService.generateQueryWithCustomMessages(
                            promptMeta.getSystem(),
                            promptMeta.getUser(),
                            request.getModel()
                    );

                    String gptResponse = (String) llmResponse.get("rawResponse");
                    if (gptResponse == null || gptResponse.isEmpty()) {
                        throw new RuntimeException("大模型返回内容为空");
                    }

                    // 提取SQL和thought
                    generatedSql = promptManager.extractSqlFromResponse(gptResponse);
                    modelThought = extractThoughtFromResponse(gptResponse);

                    llmResult.put("sqlQuery", generatedSql);
                    llmResult.put("thought", modelThought);

                    if (!generatedSql.isEmpty()) {
                        System.out.println("✓ 大模型生成 SQL: " + generatedSql);
                        System.out.println("✓ 模型思考过程: " + modelThought);
                    } else {
                        throw new RuntimeException("大模型未生成有效的SQL语句");
                    }
                }
            } else {
                System.out.println("⚠ 数据库Schema读取失败，降级使用原有llmService");
                throw new RuntimeException("无法读取数据库Schema");
            }
        } catch (Exception e) {
            System.err.println("动态Schema处理失败，降级使用原有llmService: " + e.getMessage());
            e.printStackTrace();

            try {
                llmResult = llmService.generateQueryWithConnection(
                        request.getUserPrompt(),
                        request.getModel(),
                        databaseName,
                        dbConnection
                );

                generatedSql = (String) llmResult.getOrDefault("sqlQuery", "");
                modelThought = "使用原有llmService生成SQL";
                System.out.println("✓ 原有llmService生成 SQL: " + generatedSql);
            } catch (Exception llmServiceException) {
                System.err.println("原有llmService也失败了: " + llmServiceException.getMessage());
                llmResult = new HashMap<>();
                llmResult.put("sqlQuery", "");
                
                // 【修改】提供更详细的错误信息
                String errorDetail = "大模型API调用失败";
                if (llmServiceException.getMessage() != null) {
                    if (llmServiceException.getMessage().contains("timed out")) {
                        errorDetail = "大模型API连接超时，请检查网络连接或稍后重试";
                    } else if (llmServiceException.getMessage().contains("401") || 
                               llmServiceException.getMessage().contains("403")) {
                        errorDetail = "API密钥无效或已过期，请联系管理员更新";
                    } else if (llmServiceException.getMessage().contains("429")) {
                        errorDetail = "API调用频率超限，请稍后重试";
                    } else {
                        errorDetail = "大模型服务异常: " + llmServiceException.getMessage();
                    }
                }
                llmResult.put("thought", errorDetail);
                modelThought = errorDetail;
            }
        }

        // =========================权限检查=============================
        TablePermissionService.PermissionCheckResult permissionResult =
                tablePermissionService.checkSqlPermissions(
                        userId, request.getDbConnectionId(), generatedSql);

        if (!permissionResult.isAllowed()) {
            System.err.println("✗ 权限检查失败: " + permissionResult.getMessage());

            TableDataVO errorTableData = new TableDataVO();
            errorTableData.setHeaders(Arrays.asList("权限错误"));
            errorTableData.setRows(
                    Arrays.asList(Arrays.asList("权限不足: " + permissionResult.getMessage())));

            // 记录查询操作日志（权限拒绝）
            recordOperationLog(
                    userId, request, dbConnection, databaseName, false, permissionResult.getMessage());

            long endTime = System.currentTimeMillis();
            String executionTime = String.format("%.1f秒", (endTime - startTime) / 1000.0);

            // 构建错误响应
            QueryResponseVO response = new QueryResponseVO();
            response.setId("query_" + UUID.randomUUID().toString().substring(0, 8));
            response.setUserPrompt(request.getUserPrompt());
            response.setSqlQuery(generatedSql);
            response.setConversationId(conversationId);
            response.setQueryTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            response.setExecutionTime(executionTime);
            response.setDatabase(databaseName);
            response.setFollowupQuestions(followupQuestions); // 【新增】即使失败也设置空列表

            String modelName = request.getModel();
            try {
                if (request.getModel() != null && !request.getModel().isEmpty()) {
                    Long llmConfigId = Long.parseLong(request.getModel());
                    LlmConfig llmConfig = llmConfigService.getById(llmConfigId);
                    if (llmConfig != null) {
                        modelName = llmConfig.getName() + " (" + llmConfig.getVersion() + ")";
                    }
                }
            } catch (NumberFormatException e) {
                // 保持使用request.getModel()的值
            }
            response.setModel(modelName);
            response.setTableData(errorTableData);

            System.out.println("=== 查询执行完成（权限拒绝）===");
            return response;
        }

        System.out.println("✓ 权限检查通过: " + permissionResult.getMessage());

        // 【新增】如果SQL为空，说明大模型调用失败，直接返回错误
        if (generatedSql == null || generatedSql.trim().isEmpty()) {
            System.err.println("✗ 生成SQL失败: generatedSql为空");
            
            TableDataVO errorTableData = new TableDataVO();
            errorTableData.setHeaders(Arrays.asList("错误"));
            errorTableData.setRows(Arrays.asList(Arrays.asList(
                "大模型调用失败，无法生成SQL查询。" + 
                (modelThought != null && !modelThought.isEmpty() ? "原因: " + modelThought : 
                 "可能原因: 网络超时或API服务不可用。")
            )));

            long endTime = System.currentTimeMillis();
            String executionTime = String.format("%.1f秒", (endTime - startTime) / 1000.0);

            // 构建错误响应
            QueryResponseVO response = new QueryResponseVO();
            response.setId("query_" + UUID.randomUUID().toString().substring(0, 8));
            response.setUserPrompt(request.getUserPrompt());
            response.setSqlQuery("");
            response.setConversationId(conversationId);
            response.setQueryTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            response.setExecutionTime(executionTime);
            response.setDatabase(databaseName);
            response.setDbConnectionId(request.getDbConnectionId());
            response.setFollowupQuestions(new ArrayList<>());

            String modelName = request.getModel();
            Long llmConfigId = null;
            try {
                if (request.getModel() != null && !request.getModel().isEmpty()) {
                    llmConfigId = Long.parseLong(request.getModel());
                    LlmConfig llmConfig = llmConfigService.getById(llmConfigId);
                    if (llmConfig != null) {
                        modelName = llmConfig.getName() + " (" + llmConfig.getVersion() + ")";
                    }
                }
            } catch (NumberFormatException e) {
                llmConfigId = null;
            }
            response.setModel(modelName);
            response.setLlmConfigId(llmConfigId);
            response.setTableData(errorTableData);

            System.out.println("=== 查询执行完成（SQL生成失败）===");
            return response;
        }

        //======================SQL执行与数据处理========================
        TableDataVO realTableData = null;
        boolean executionSuccess = false;
        String errorMessage = null;
        List<Map<String, Object>> queryResultData = new ArrayList<>(); // 【新增】保存原始查询结果

        try {
            if (generatedSql != null && !generatedSql.trim().isEmpty()) {
                // 使用动态数据库执行器执行SQL
                Map<String, Object> queryResult = databaseExecutor.executeQuery(dbConnection, generatedSql);

                // 转换为TableDataVO
                realTableData = new TableDataVO();

                // 处理 headers
                Object headersObj = queryResult.get("headers");
                List<String> headers = new ArrayList<>();
                if (headersObj instanceof List) {
                    for (Object header : (List<?>) headersObj) {
                        headers.add(header != null ? header.toString() : "");
                    }
                }
                realTableData.setHeaders(headers);

                // 处理 rows
                Object rowsObj = queryResult.get("rows");
                List<List<String>> stringRows = new ArrayList<>();
                if (rowsObj instanceof List) {
                    for (Object rowObj : (List<?>) rowsObj) {
                        List<String> rowList = new ArrayList<>();
                        if (rowObj instanceof List) {
                            for (Object cell : (List<?>) rowObj) {
                                rowList.add(cell != null ? cell.toString() : "");
                            }
                        }
                        stringRows.add(rowList);
                    }
                }
                realTableData.setRows(stringRows);

                executionSuccess = true;
                System.out.println("✓ SQL执行成功，返回 " + stringRows.size() + " 行数据");

                // 【新增】保存原始查询结果用于后续问题生成
                queryResultData = convertRowsToMap(stringRows, headers);
            }
        } catch (Exception e) {
            System.err.println("✗ SQL执行失败: " + e.getMessage());
            errorMessage = e.getMessage();
            realTableData = new TableDataVO();
            realTableData.setHeaders(Arrays.asList("错误信息"));
            realTableData.setRows(Arrays.asList(Arrays.asList("SQL执行失败: " + e.getMessage())));
        }

        // ============== 【新增】异步生成后续问题 ==============
        // 在try块外面创建final副本
        final String finalSchemaJson = databaseSchemaJson;
        final List<Map<String, Object>> finalQueryResultData = new ArrayList<>(queryResultData);

        if (executionSuccess && finalSchemaJson != null && !finalSchemaJson.isEmpty()) {
            System.out.println("准备生成后续探索问题...");

            // 异步生成后续问题（不阻塞主流程）
            CompletableFuture<List<String>> followupFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    // 将查询结果转换为字符串摘要
                    String resultSummary = convertResultToSummary(finalQueryResultData);

                    // 调用后续问题生成器
                    List<String> questions = followupQuestionGenerator.generateFollowupQuestions(
                            request.getUserPrompt(),
                            finalSchemaJson,
                            resultSummary
                    );

                    System.out.println("✓ 生成 " + questions.size() + " 个后续问题");
                    return questions;
                } catch (Exception e) {
                    System.err.println("生成后续问题失败: " + e.getMessage());
                    e.printStackTrace();
                    return new ArrayList<String>();
                }
            });

            // 设置回调
            followupFuture.thenAccept(questions -> {
                followupQuestions.clear();
                followupQuestions.addAll(questions);
                System.out.println("后续问题已设置到响应中");
            }).exceptionally(ex -> {
                System.err.println("后续问题生成异常: " + ex.getMessage());
                return null;
            });
        }
     //========================新增结束====================================


        //===============================构建响应对象===================================
        long endTime = System.currentTimeMillis();
        String executionTime = String.format("%.1f秒", (endTime - startTime) / 1000.0);

        // 构建响应
        QueryResponseVO response = new QueryResponseVO();
        response.setId("query_" + UUID.randomUUID().toString().substring(0, 8));
        response.setUserPrompt(request.getUserPrompt());
        response.setSqlQuery(generatedSql);
        response.setConversationId(conversationId);
        response.setQueryTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        response.setExecutionTime(executionTime);
        response.setDatabase(databaseName);
        response.setDbConnectionId(request.getDbConnectionId());
        response.setFollowupQuestions(followupQuestions); // 【新增】设置后续问题

        String modelName = request.getModel();
        Long llmConfigId = null;
        try {
            if (request.getModel() != null && !request.getModel().isEmpty()) {
                llmConfigId = Long.parseLong(request.getModel());
                LlmConfig llmConfig = llmConfigService.getById(llmConfigId);
                if (llmConfig != null) {
                    modelName = llmConfig.getName() + " (" + llmConfig.getVersion() + ")";
                }
            }
        } catch (NumberFormatException e) {
            llmConfigId = null;
        }
        response.setModel(modelName);
        response.setLlmConfigId(llmConfigId);

        // 使用真实的表格数据
        if (executionSuccess && realTableData != null) {
            response.setTableData(realTableData);
        } else if (realTableData != null) {
            response.setTableData(realTableData);
        } else {
            Object tableDataObj = llmResult.get("tableData");
            if (tableDataObj != null) {
                TableDataVO tableData = parseTableData(tableDataObj);
                response.setTableData(tableData);
            }
        }

        //================================图表生成=============================================
        ChartDataVO realChartData = null;
        if (executionSuccess
                && realTableData != null
                && realTableData.getRows() != null
                && !realTableData.getRows().isEmpty()
                && realTableData.getHeaders() != null
                && realTableData.getHeaders().size() >= 2) {
            try {
                realChartData = generateChartFromData(realTableData);
            } catch (Exception e) {
                System.err.println("从真实数据生成图表失败: " + e.getMessage());
            }
        }

        if (realChartData != null) {
            response.setChartData(realChartData);
        } else {
            Object chartDataObj = llmResult.get("chartData");
            if (chartDataObj != null) {
                ChartDataVO chartData = parseChartData(chartDataObj);
                if (chartData != null
                        && chartData.getDatasets() != null
                        && !chartData.getDatasets().isEmpty()) {
                    response.setChartData(chartData);
                }
            }
        }

        //==================================记录查询操作日志=====================================
        try {
            recordOperationLog(userId, request, dbConnection, databaseName, executionSuccess, errorMessage);
        } catch (Exception e) {
            System.err.println("记录查询操作日志失败: " + e.getMessage());
            e.printStackTrace();
        }

        //==================================保存对话详情到MongoDB=====================================
        try {
            saveDialogDetail(conversationId, request.getUserPrompt(), generatedSql, response, executionSuccess);
        } catch (Exception e) {
            System.err.println("保存对话详情失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== 查询执行完成 ===");
        System.out.println("生成 " + followupQuestions.size() + " 个后续问题");
        // ========【新增】输出具体的后续问题内容==========
        if (!followupQuestions.isEmpty()) {
            System.out.println("后续问题内容:");
            for (int i = 0; i < followupQuestions.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + followupQuestions.get(i));
            }
        } else {
            System.out.println("未生成后续问题（可能失败或未启用）");
        }
        //==================================================
        System.out.println("准备返回响应: id=" + response.getId());

        try {
            return response;
        } catch (Exception e) {
            System.err.println("返回响应时出错: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<String> getRecommendations(RecommendationRequestDTO request) {
        try {
            Long userId = 1L; // 默认用户ID，实际应从上下文获取
            
            // 1. 获取数据库连接和Schema
            DbConnection dbConnection = dbConnectionService.getById(request.getDbConnectionId());
            if (dbConnection == null) {
                return getDefaultRecommendations();
            }
            
            List<DynamicDatabaseExecutor.TableSchema> tableSchemas = databaseExecutor.getDatabaseSchema(dbConnection);
            String schemaJson = convertTableSchemasToJson(tableSchemas);
            
            // 2. 获取用户查询历史
            List<QueryLog> historyLogs = queryLogService.listByUserId(userId);
            String historySummary = historyLogs.stream()
                    .limit(10)
                    .map(log -> log.getUserPrompt())
                    .collect(Collectors.joining("\n"));
            
            if (historySummary.isEmpty()) {
                historySummary = "无历史记录";
            }
            
            // 3. 生成Prompt
            PromptMeta promptMeta = promptManager.generatePrompt(
                    historySummary,
                    schemaJson,
                    PromptTemplateType.RECOMMENDATION
            );
            
            // 4. 调用LLM
            Map<String, Object> llmResponse = llmService.generateQueryWithCustomMessages(
                    promptMeta.getSystem(),
                    promptMeta.getUser(),
                    request.getLlmConfigId()
            );
            
            String rawResponse = (String) llmResponse.get("rawResponse");
            if (rawResponse == null || rawResponse.isEmpty()) {
                return getDefaultRecommendations();
            }
            
            // 5. 解析响应
            ObjectMapper mapper = new ObjectMapper();
            List<String> recommendations = mapper.readValue(rawResponse, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            
            return recommendations.stream().limit(4).collect(Collectors.toList());
            
        } catch (Exception e) {
            System.err.println("生成智能推荐失败: " + e.getMessage());
            e.printStackTrace();
            return getDefaultRecommendations();
        }
    }

    private List<String> getDefaultRecommendations() {
        return Arrays.asList(
                "查询最近7天的数据趋势",
                "统计各分类的汇总信息",
                "查找异常记录",
                "分析用户增长情况"
        );
    }

    // 【新增】实现接口方法
    @Override
    public List<String> generateFollowupQuestions(String question, String schema) {
        return followupQuestionGenerator.generateFollowupQuestions(question, schema);
    }

    @Override
    public List<String> generateFollowupQuestionsWithResult(String question, String schema, List<Map<String, Object>> result) {
        String resultSummary = convertResultToSummary(result);
        return followupQuestionGenerator.generateFollowupQuestions(question, schema, resultSummary);
    }

    // 【新增】辅助方法：将查询结果转换为字符串摘要
    private String convertResultToSummary(List<Map<String, Object>> result) {
        if (result == null || result.isEmpty()) {
            return "查询返回空结果";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("查询返回 ").append(result.size()).append(" 行数据\n");

        // 只取前3行作为示例
        int sampleRows = Math.min(3, result.size());
        for (int i = 0; i < sampleRows; i++) {
            Map<String, Object> row = result.get(i);
            sb.append("第 ").append(i + 1).append(" 行: ");
            sb.append("{");

            boolean first = true;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append(": ").append(entry.getValue());
                first = false;
            }
            sb.append("}\n");
        }

        if (result.size() > sampleRows) {
            sb.append("... 还有 ").append(result.size() - sampleRows).append(" 行数据");
        }

        return sb.toString();
    }

    // 【新增】辅助方法：将List<List<String>>转换为List<Map<String, Object>>
    private List<Map<String, Object>> convertRowsToMap(List<List<String>> rows, List<String> headers) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (rows == null || headers == null) {
            return result;
        }

        for (List<String> row : rows) {
            Map<String, Object> rowMap = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String key = headers.get(i);
                String value = i < row.size() ? row.get(i) : "";
                rowMap.put(key, value);
            }
            result.add(rowMap);
        }

        return result;
    }

    /**
     * 将TableSchemas转换为JSON字符串
     */
    private String convertTableSchemasToJson(List<DynamicDatabaseExecutor.TableSchema> tableSchemas)
            throws Exception {

        Map<String, Object> schemaMap = new HashMap<>();
        List<Map<String, Object>> tablesList = new ArrayList<>();

        for (DynamicDatabaseExecutor.TableSchema tableSchema : tableSchemas) {
            Map<String, Object> tableMap = new HashMap<>();
            tableMap.put("name", tableSchema.getTableName());
            tableMap.put("comment", tableSchema.getTableComment());

            List<Map<String, Object>> columnsList = new ArrayList<>();
            for (DynamicDatabaseExecutor.ColumnInfo column : tableSchema.getColumns()) {
                Map<String, Object> columnMap = new HashMap<>();
                columnMap.put("field", column.getColumnName());
                columnMap.put("type", column.getDataType());
                columnMap.put("null", column.isNullable());
                columnMap.put("key", column.isPrimaryKey() ? "PRI" : "");
                columnMap.put("default", null);
                columnMap.put("extra", "");
                columnsList.add(columnMap);
            }

            tableMap.put("columns", columnsList);
            tablesList.add(tableMap);
        }

        schemaMap.put("tables", tablesList);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(schemaMap);
    }

    /**
     * 从GPT返回的JSON中提取thought信息
     */
    private String extractThoughtFromResponse(String gptResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(gptResponse);
            com.fasterxml.jackson.databind.JsonNode thoughtNode = jsonNode.path("thought");
            if (thoughtNode != null && !thoughtNode.isMissingNode()) {
                return thoughtNode.asText();
            }
            return "";
        } catch (Exception e) {
            System.err.println("无法解析GPT返回的thought信息: " + e.getMessage());
            return "";
        }
    }

    /** 记录一次查询操作到操作日志表 */
    private void recordOperationLog(
            Long userId,
            QueryRequestDTO request,
            DbConnection dbConnection,
            String databaseName,
            boolean success,
            String errorMessage) {
        try {
            OperationLog log = new OperationLog();

            // 用户信息
            log.setUserId(userId);
            User user = userService.getById(userId);
            log.setUsername(user != null ? user.getUsername() : "用户#" + userId);

            // 操作与模块
            log.setOperation("查询");
            log.setModule("查询操作");

            // 涉及大模型（从配置表获取名称和版本）
            String llmName = null;
            if (request.getModel() != null) {
                try {
                    LlmConfig config = llmConfigService.getById(Long.valueOf(request.getModel()));
                    if (config != null) {
                        llmName = config.getName() + " (" + config.getVersion() + ")";
                    }
                } catch (NumberFormatException ignored) {
                    // ignore, 使用原始ID
                }
            }
            if (llmName == null) {
                llmName = "模型ID=" + request.getModel();
            }
            log.setRelatedLlm(llmName);

            // 其它信息
            log.setIpAddress("unknown");
            log.setOperateTime(LocalDateTime.now());
            log.setResult(success ? 1 : 0);
            if (!success && errorMessage != null && !errorMessage.isEmpty()) {
                log.setErrorMsg("数据库: " + databaseName + "，错误: " + errorMessage);
            }

            operationLogService.save(log);
        } catch (Exception e) {
            System.err.println("记录查询操作日志失败: " + e.getMessage());
        }
    }

    /** 根据真实表格数据自动生成图表数据 */
    private ChartDataVO generateChartFromData(TableDataVO tableData) {
        List<String> headers = tableData.getHeaders();
        List<List<String>> rows = tableData.getRows();

        if (headers.size() < 2 || rows.isEmpty()) {
            return null;
        }

        List<String> labels =
                rows.stream().map(row -> row.size() > 0 ? row.get(0) : "").collect(Collectors.toList());

        int valueColumnIndex = -1;
        for (int i = 1; i < headers.size(); i++) {
            boolean isNumeric = true;
            for (List<String> row : rows) {
                if (row.size() <= i) continue;
                String val = row.get(i);
                if (val == null || val.isEmpty()) continue;
                try {
                    Double.parseDouble(val.replace(",", ""));
                } catch (NumberFormatException e) {
                    isNumeric = false;
                    break;
                }
            }
            if (isNumeric) {
                valueColumnIndex = i;
                break;
            }
        }

        if (valueColumnIndex == -1) {
            return null;
        }

        ChartDataVO chartData = new ChartDataVO();
        chartData.setType("pie");
        chartData.setLabels(labels);

        DatasetVO dataset = new DatasetVO();
        dataset.setLabel(headers.get(valueColumnIndex));
        
        // 【修改】根据图表类型设置不同的背景颜色
        // 对于柱状图和折线图，使用单一颜色
        // 对于饼状图，为每个数据点生成不同的颜色
        //dataset.setBackgroundColor("rgba(54, 162, 235, 0.6)");
        
        List<String> colors = generatePieChartColors(labels.size());
        dataset.setBackgroundColor(colors);   // 传 List<String> 而非单色字符串
        System.out.println("✓ 为饼状图生成了 " + colors.size() + " 个颜色");

        final int colIdx = valueColumnIndex;
        List<Number> data =
                rows.stream()
                        .map(
                                row -> {
                                    if (row.size() <= colIdx) return 0.0;
                                    String val = row.get(colIdx);
                                    if (val == null || val.isEmpty()) return 0.0;
                                    try {
                                        return Double.parseDouble(val.replace(",", ""));
                                    } catch (NumberFormatException e) {
                                        return 0.0;
                                    }
                                })
                        .collect(Collectors.toList());

        dataset.setData(data);
        chartData.setDatasets(Collections.singletonList(dataset));

        System.out.println("✓ 已根据真实数据自动生成图表");
        return chartData;
    }
    
    /**
     * 生成饼状图颜色数组
     * 为每个扇区生成不同的颜色
     */
    private List<String> generatePieChartColors(int count) {
        // 预定义的颜色方案，使用区分度高的颜色
        String[] baseColors = {
            "rgba(22, 93, 255, 0.7)",    // 蓝色
            "rgba(54, 162, 235, 0.7)",   // 天蓝色
            "rgba(255, 206, 86, 0.7)",   // 黄色
            "rgba(75, 192, 192, 0.7)",   // 青色
            "rgba(153, 102, 255, 0.7)",  // 紫色
            "rgba(255, 159, 64, 0.7)",   // 橙色
            "rgba(255, 99, 132, 0.7)",   // 红色
            "rgba(46, 204, 113, 0.7)",   // 绿色
            "rgba(155, 89, 182, 0.7)",   // 深紫色
            "rgba(52, 152, 219, 0.7)",   // 深蓝色
            "rgba(241, 196, 15, 0.7)",   // 金黄色
            "rgba(230, 126, 34, 0.7)",   // 深橙色
            "rgba(231, 76, 60, 0.7)",    // 深红色
            "rgba(26, 188, 156, 0.7)",   // 青绿色
            "rgba(142, 68, 173, 0.7)",   // 深紫罗兰色
        };
        
        List<String> colors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            colors.add(baseColors[i % baseColors.length]);
        }
        
        return colors;
    }

    /** 解析表格数据 */
    @SuppressWarnings("unchecked")
    private TableDataVO parseTableData(Object tableDataObj) {
        TableDataVO tableData = new TableDataVO();

        if (tableDataObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) tableDataObj;

            Object headersObj = map.get("headers");
            if (headersObj instanceof List) {
                List<String> headers =
                        ((List<?>) headersObj).stream().map(String::valueOf).collect(Collectors.toList());
                tableData.setHeaders(headers);
            }

            Object rowsObj = map.get("rows");
            if (rowsObj instanceof List) {
                List<List<String>> rows = new ArrayList<>();
                for (Object row : (List<?>) rowsObj) {
                    if (row instanceof List) {
                        List<String> rowList =
                                ((List<?>) row).stream().map(String::valueOf).collect(Collectors.toList());
                        rows.add(rowList);
                    } else {
                        rows.add(Collections.emptyList());
                    }
                }
                tableData.setRows(rows);
            }
        } else if (tableDataObj instanceof JSONObject) {
            JSONObject json = (JSONObject) tableDataObj;
            JSONArray headersArray = json.getJSONArray("headers");
            JSONArray rowsArray = json.getJSONArray("rows");

            List<String> headers =
                    headersArray != null ? headersArray.toJavaList(String.class) : Collections.emptyList();

            List<List<String>> rows = new ArrayList<>();
            if (rowsArray != null) {
                for (Object row : rowsArray) {
                    if (row instanceof JSONArray) {
                        rows.add(((JSONArray) row).toJavaList(String.class));
                    } else {
                        rows.add(Collections.emptyList());
                    }
                }
            }

            tableData.setHeaders(headers);
            tableData.setRows(rows);
        }

        return tableData;
    }

    /** 解析图表数据 */
    @SuppressWarnings("unchecked")
    private ChartDataVO parseChartData(Object chartDataObj) {
        ChartDataVO chartData = new ChartDataVO();

        if (chartDataObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) chartDataObj;
            chartData.setType((String) map.getOrDefault("type", "bar"));

            Object labelsObj = map.get("labels");
            if (labelsObj instanceof List) {
                List<String> labels =
                        ((List<?>) labelsObj).stream().map(String::valueOf).collect(Collectors.toList());
                chartData.setLabels(labels);
            }

            Object datasetsObj = map.get("datasets");
            if (datasetsObj instanceof List) {
                List<DatasetVO> datasets =
                        ((List<?>) datasetsObj)
                                .stream()
                                .map(
                                        datasetObj -> {
                                            DatasetVO dataset = new DatasetVO();
                                            if (datasetObj instanceof Map) {
                                                Map<String, Object> datasetMap = (Map<String, Object>) datasetObj;
                                                dataset.setLabel((String) datasetMap.get("label"));

                                                Object dataObj = datasetMap.get("data");
                                                if (dataObj instanceof List) {
                                                    List<Number> data =
                                                            ((List<?>) dataObj)
                                                                    .stream()
                                                                    .map(
                                                                            item -> {
                                                                                if (item instanceof Number) {
                                                                                    return ((Number) item).doubleValue();
                                                                                }
                                                                                return 0.0;
                                                                            })
                                                                    .collect(Collectors.toList());
                                                    dataset.setData(data);
                                                }

                                                // 【修改】处理背景颜色：支持字符串或数组
                                                Object bgColor = datasetMap.get("backgroundColor");
                                                dataset.setBackgroundColor(bgColor);
                                            }
                                            return dataset;
                                        })
                                .collect(Collectors.toList());
                chartData.setDatasets(datasets);
            }
        } else if (chartDataObj instanceof JSONObject) {
            JSONObject json = (JSONObject) chartDataObj;
            chartData.setType(json.getString("type"));

            JSONArray labelsArray = json.getJSONArray("labels");
            if (labelsArray != null) {
                chartData.setLabels(labelsArray.toJavaList(String.class));
            }

            JSONArray datasetsArray = json.getJSONArray("datasets");
            if (datasetsArray != null) {
                List<DatasetVO> datasets =
                        datasetsArray.stream()
                                .map(
                                        item -> {
                                            JSONObject datasetJson = (JSONObject) item;
                                            DatasetVO dataset = new DatasetVO();
                                            dataset.setLabel(datasetJson.getString("label"));

                                            JSONArray dataArray = datasetJson.getJSONArray("data");
                                            if (dataArray != null) {
                                                List<Number> data =
                                                        dataArray.stream()
                                                                .map(
                                                                        obj -> {
                                                                            if (obj instanceof Number) {
                                                                                return ((Number) obj).doubleValue();
                                                                            }
                                                                            return 0.0;
                                                                        })
                                                                .collect(Collectors.toList());
                                                dataset.setData(data);
                                            }

                                            // 【修改】处理背景颜色：支持字符串或数组
                                            Object bgColor = datasetJson.get("backgroundColor");
                                            dataset.setBackgroundColor(bgColor);
                                            return dataset;
                                        })
                                .collect(Collectors.toList());
                chartData.setDatasets(datasets);
            }
        }

        // 【新增】为饼状图设置颜色数组
        if ("pie".equalsIgnoreCase(chartData.getType()) && chartData.getDatasets() != null && !chartData.getDatasets().isEmpty()) {
            for (DatasetVO dataset : chartData.getDatasets()) {
                // 如果backgroundColor不是数组，或者为null，则生成颜色数组
                if (dataset.getBackgroundColor() == null || 
                    (dataset.getBackgroundColor() instanceof String)) {
                    int dataSize = dataset.getData() != null ? dataset.getData().size() : 0;
                    if (dataSize > 0) {
                        List<String> colors = generatePieChartColors(dataSize);
                        dataset.setBackgroundColor(colors);
                        System.out.println("✓ 为饼状图生成了 " + dataSize + " 个颜色");
                    }
                }
            }
        }

        return chartData;
    }
    
    /**
     * 保存对话详情到MongoDB
     * 包含用户输入、生成的SQL、AI响应等信息
     */
    private void saveDialogDetail(String conversationId, String userInput, String generatedSql, 
                                   QueryResponseVO response, boolean executionSuccess) {
        try {
            if (conversationId == null || conversationId.isEmpty()) {
                System.out.println("对话ID为空，跳过保存DialogDetail");
                return;
            }

            // 查找或创建DialogDetail
            DialogDetail dialogDetail = dialogDetailRepository.findByDialogId(conversationId);
            if (dialogDetail == null) {
                System.out.println("创建新的DialogDetail: " + conversationId);
                dialogDetail = new DialogDetail();
                dialogDetail.setDialogId(conversationId);
                dialogDetail.setRounds(new ArrayList<>());
            }

            // 计算当前轮次
            int currentRound = dialogDetail.getRounds() != null ? dialogDetail.getRounds().size() + 1 : 1;
            System.out.println("保存第 " + currentRound + " 轮对话");

            // 创建新的对话轮次
            DialogDetail.Round round = new DialogDetail.Round();
            round.setRoundNum(currentRound);
            round.setUserInput(userInput);
            round.setGeneratedSql(generatedSql);
            round.setRoundTime(LocalDateTime.now());

            // 构建AI响应（包含查询结果摘要）
            StringBuilder aiResponseBuilder = new StringBuilder();
            if (executionSuccess) {
                aiResponseBuilder.append("Query executed successfully. ");
                if (response.getTableData() != null && response.getTableData().getRows() != null) {
                    int rowCount = response.getTableData().getRows().size();
                    aiResponseBuilder.append("Returned ").append(rowCount).append(" rows. ");
                }
                
                // 保存完整的响应数据（JSON格式）
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    String responseJson = objectMapper.writeValueAsString(response);
                    round.setAiResponse(responseJson);
                } catch (Exception e) {
                    round.setAiResponse(aiResponseBuilder.toString());
                }
            } else {
                aiResponseBuilder.append("Query execution failed.");
                round.setAiResponse(aiResponseBuilder.toString());
            }

            // 添加到轮次列表
            if (dialogDetail.getRounds() == null) {
                dialogDetail.setRounds(new ArrayList<>());
            }
            dialogDetail.getRounds().add(round);

            // 保存到MongoDB
            DialogDetail saved = dialogDetailRepository.save(dialogDetail);
            System.out.println("✓ DialogDetail保存成功: " + conversationId + ", 轮次: " + currentRound + 
                             ", MongoDB ID: " + saved.getId());

        } catch (Exception e) {
            System.err.println("保存DialogDetail失败: " + e.getMessage());
            e.printStackTrace();
            // 不抛出异常，避免影响主流程
        }
    }

}