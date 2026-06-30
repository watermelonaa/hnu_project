package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.dto.CollectionRecordDTO;
import com.baoma.natural_language_query.entity.mongodb.CollectionRecord;
import com.baoma.natural_language_query.entity.mongodb.QueryCollection;
import com.baoma.natural_language_query.entity.mysql.QueryLog;
import com.baoma.natural_language_query.service.CollectionRecordService;
import com.baoma.natural_language_query.service.QueryCollectionService;
import com.baoma.natural_language_query.service.QueryLogService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/collection-record")
public class CollectionRecordController {

  @Autowired private CollectionRecordService collectionRecordService;

  @Autowired private QueryCollectionService queryCollectionService;

  @Autowired private QueryLogService queryLogService;

  @GetMapping("/list/query/{queryId}")
  public Result<List<CollectionRecord>> listByQueryId(@PathVariable String queryId) {
    // 将字符串转换为 ObjectId
    ObjectId queryIdObjectId;
    try {
      queryIdObjectId = new ObjectId(queryId);
    } catch (IllegalArgumentException e) {
      return Result.error("无效的收藏夹ID格式: " + queryId);
    }
    return Result.success(collectionRecordService.listByQueryId(queryIdObjectId));
  }

  @GetMapping("/list/user/{userId}")
  public Result<List<CollectionRecord>> listByUserId(@PathVariable Long userId) {
    return Result.success(collectionRecordService.listByUserId(userId));
  }

  @GetMapping("/list/db/{dbConnectionId}")
  public Result<List<CollectionRecord>> listByDbConnectionId(@PathVariable Long dbConnectionId) {
    return Result.success(collectionRecordService.listByDbConnectionId(dbConnectionId));
  }

  @GetMapping("/{id}")
  public Result<CollectionRecord> getById(@PathVariable String id) {
    return Result.success(collectionRecordService.getById(id));
  }

  @PostMapping
  public Result<CollectionRecord> save(@RequestBody CollectionRecordDTO dto) {
    try {
      System.out.println("收到收藏请求: collectionId=" + dto.getCollectionId() 
          + " (type: " + (dto.getCollectionId() != null ? dto.getCollectionId().getClass().getSimpleName() : "null") + ")"
          + ", queryLogId=" + dto.getQueryLogId() 
          + ", userId=" + dto.getUserId());

      // 验证必填字段 - 收藏功能必须由用户主动触发，collectionId 不能为空
      if (dto.getCollectionId() == null) {
        throw new IllegalArgumentException("收藏功能必须选择收藏夹，collectionId 不能为空。请确保用户主动点击收藏按钮。");
      }
      
      // 处理 collectionId：可能是 String（MongoDB ObjectId）或 Long
      String collectionIdStr;
      if (dto.getCollectionId() instanceof String) {
        collectionIdStr = (String) dto.getCollectionId();
        if (collectionIdStr.trim().isEmpty()) {
          throw new IllegalArgumentException("collectionId 不能为空");
        }
      } else if (dto.getCollectionId() instanceof Number) {
        Long collectionIdLong = ((Number) dto.getCollectionId()).longValue();
        if (collectionIdLong <= 0) {
          throw new IllegalArgumentException("collectionId 必须大于0");
        }
        collectionIdStr = String.valueOf(collectionIdLong);
      } else {
        throw new IllegalArgumentException("collectionId 必须是 String 或 Number 类型");
      }
      
      if (dto.getQueryLogId() == null || dto.getQueryLogId() <= 0) {
        throw new IllegalArgumentException("queryLogId 不能为空且必须大于0");
      }

      // 获取收藏夹信息（collectionId 已转换为 String）
      QueryCollection collection = queryCollectionService.getById(collectionIdStr);
      if (collection == null) {
        throw new IllegalArgumentException("收藏夹不存在，ID: " + dto.getCollectionId());
      }
      // Lombok @Data 会自动生成 getter 方法，但这里直接使用字段名（如果编译失败，说明 Lombok 未生效）
      System.out.println("找到收藏夹: id=" + collectionIdStr + ", groupName=" + (collection != null ? "已找到" : "未找到"));

      // 获取查询日志信息（如果提供了 queryLogId）
      QueryLog queryLog = null;
      String conversationId = null;
      if (dto.getQueryLogId() != null) {
        queryLog = queryLogService.getById(dto.getQueryLogId());
        if (queryLog != null) {
          conversationId = queryLog.getDialogId();
          System.out.println("从QueryLog获取对话ID: " + conversationId);
        } else {
          System.out.println("警告: 查询日志不存在，ID: " + dto.getQueryLogId() + "，将使用DTO中的信息");
        }
      }

      // 构建收藏记录
      CollectionRecord collectionRecord = new CollectionRecord();
      
      // 设置收藏夹ID（queryId）- MongoDB schema 要求 ObjectId 类型
      // 将字符串转换为 ObjectId
      ObjectId queryIdObjectId;
      try {
        queryIdObjectId = new ObjectId(collectionIdStr);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("无效的收藏夹ID格式: " + collectionIdStr + "，必须是有效的 ObjectId");
      }
      collectionRecord.setQueryId(queryIdObjectId);
      
      // 设置用户ID
      if (dto.getUserId() != null) {
        collectionRecord.setUserId(dto.getUserId());
      } else if (queryLog != null) {
        collectionRecord.setUserId(queryLog.getUserId());
      } else {
        throw new IllegalArgumentException("userId 不能为空");
      }

      // 设置SQL内容
      if (dto.getSqlContent() != null && !dto.getSqlContent().trim().isEmpty()) {
        collectionRecord.setSqlContent(dto.getSqlContent());
      } else {
        // 如果DTO中没有sqlContent，说明前端只发送了queryLogId
        // 这里需要从DialogDetail获取，但暂时先要求前端发送完整数据
        throw new IllegalArgumentException("sqlContent 不能为空，请在前端发送完整查询数据");
      }

      // 设置用户提示
      if (dto.getUserPrompt() != null && !dto.getUserPrompt().trim().isEmpty()) {
        collectionRecord.setUserPrompt(dto.getUserPrompt());
      }

      // 设置查询结果（确保包含完整信息：tableData, chartData, database, model, executionTime, conversationId）
      if (dto.getQueryResult() != null) {
        Map<String, Object> queryResult = new HashMap<>(dto.getQueryResult());
        // 确保包含对话ID
        if (conversationId != null && !conversationId.isEmpty()) {
          queryResult.put("conversationId", conversationId);
        }
        collectionRecord.setQueryResult(queryResult);
      } else {
        // 如果DTO中没有queryResult，创建一个包含基本信息的Map
        Map<String, Object> queryResult = new HashMap<>();
        if (conversationId != null && !conversationId.isEmpty()) {
          queryResult.put("conversationId", conversationId);
        }
        collectionRecord.setQueryResult(queryResult);
      }

      // 设置数据库连接ID
      if (dto.getDbConnectionId() != null) {
        collectionRecord.setDbConnectionId(dto.getDbConnectionId());
      } else if (queryLog != null && queryLog.getDataSourceId() != null) {
        collectionRecord.setDbConnectionId(queryLog.getDataSourceId());
      } else {
        throw new IllegalArgumentException("dbConnectionId 不能为空");
      }

      // 设置大模型配置ID（MongoDB schema 要求必填）
      if (dto.getLlmConfigId() != null) {
        collectionRecord.setLlmConfigId(dto.getLlmConfigId());
      } else {
        throw new IllegalArgumentException("llmConfigId 不能为空");
      }

      // 设置创建时间
      collectionRecord.setCreateTime(LocalDateTime.now());
      
      // 验证所有必填字段
      if (collectionRecord.getQueryId() == null) {
        throw new IllegalArgumentException("queryId 不能为空");
      }
      if (collectionRecord.getUserId() == null) {
        throw new IllegalArgumentException("userId 不能为空");
      }
      if (collectionRecord.getSqlContent() == null || collectionRecord.getSqlContent().trim().isEmpty()) {
        throw new IllegalArgumentException("sqlContent 不能为空");
      }
      if (collectionRecord.getDbConnectionId() == null) {
        throw new IllegalArgumentException("dbConnectionId 不能为空");
      }
      if (collectionRecord.getLlmConfigId() == null) {
        throw new IllegalArgumentException("llmConfigId 不能为空");
      }

      System.out.println("准备保存收藏记录: queryId=" + collectionRecord.getQueryId() 
          + ", userId=" + collectionRecord.getUserId() 
          + ", dbConnectionId=" + collectionRecord.getDbConnectionId());

      CollectionRecord saved = collectionRecordService.save(collectionRecord);
      System.out.println("收藏保存成功，ID: " + saved.getId());
      return Result.success(saved);
    } catch (Exception e) {
      System.err.println("保存收藏记录失败: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable String id) {
    collectionRecordService.deleteById(id);
    return Result.success();
  }

  /**
   * 查找与给定SQL相似的收藏记录（用于自动分类）
   *
   * @param userId 用户ID
   * @param sqlContent SQL内容
   * @param threshold 相似度阈值（可选，默认0.8）
   * @return 相似的收藏记录列表
   */
  @GetMapping("/similar/{userId}")
  public Result<List<CollectionRecord>> findSimilarRecords(
      @PathVariable Long userId,
      @RequestParam String sqlContent,
      @RequestParam(required = false, defaultValue = "0.8") double threshold) {
    return Result.success(collectionRecordService.findSimilarRecords(userId, sqlContent, threshold));
  }

  /**
   * 获取相同查询的不同时间快照（用于对比）
   *
   * @param userId 用户ID
   * @param content SQL内容或userPrompt
   * @param usePrompt 是否使用userPrompt进行匹配（true：使用userPrompt，false：使用SQL）
   * @return 相同查询的不同时间快照列表，按时间倒序
   */
  @GetMapping("/snapshots/{userId}")
  public Result<List<CollectionRecord>> getQuerySnapshots(
      @PathVariable Long userId,
      @RequestParam String content,
      @RequestParam(required = false, defaultValue = "false") boolean usePrompt) {
    return Result.success(collectionRecordService.getQuerySnapshots(userId, content, usePrompt));
  }

  /**
   * 自动分类用户的收藏记录（根据SQL相似度）
   *
   * @param userId 用户ID
   * @return 分类结果，key为分类ID（SQL的标准化版本），value为该分类下的记录列表
   */
  @GetMapping("/auto-classify/{userId}")
  public Result<Map<String, List<CollectionRecord>>> autoClassifyRecords(@PathVariable Long userId) {
    return Result.success(collectionRecordService.autoClassifyRecords(userId));
  }
}
