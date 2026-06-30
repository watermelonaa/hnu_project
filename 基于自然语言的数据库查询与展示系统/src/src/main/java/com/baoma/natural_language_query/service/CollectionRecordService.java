package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mongodb.CollectionRecord;
import java.util.List;
import java.util.Map;
import org.bson.types.ObjectId;

public interface CollectionRecordService {

  List<CollectionRecord> listByQueryId(ObjectId queryId);

  List<CollectionRecord> listByUserId(Long userId);

  List<CollectionRecord> listByDbConnectionId(Long dbConnectionId);

  CollectionRecord getById(String id);

  CollectionRecord save(CollectionRecord collectionRecord);

  void deleteById(String id);

  /**
   * 查找与给定SQL相似的收藏记录（用于自动分类）
   *
   * @param userId 用户ID
   * @param sqlContent SQL内容
   * @param similarityThreshold 相似度阈值（0.0-1.0），默认0.8
   * @return 相似的收藏记录列表
   */
  List<CollectionRecord> findSimilarRecords(Long userId, String sqlContent, double similarityThreshold);

  /**
   * 查找与给定SQL相似的收藏记录（使用默认阈值0.8）
   *
   * @param userId 用户ID
   * @param sqlContent SQL内容
   * @return 相似的收藏记录列表
   */
  List<CollectionRecord> findSimilarRecords(Long userId, String sqlContent);

  /**
   * 获取相同查询的不同时间快照（用于对比）
   *
   * @param userId 用户ID
   * @param sqlContent SQL内容（或userPrompt）
   * @param usePrompt 是否使用userPrompt进行匹配（true：使用userPrompt，false：使用SQL）
   * @return 相同查询的不同时间快照列表，按时间倒序
   */
  List<CollectionRecord> getQuerySnapshots(Long userId, String sqlContent, boolean usePrompt);

  /**
   * 自动分类用户的收藏记录（根据SQL相似度）
   *
   * @param userId 用户ID
   * @return 分类结果，key为分类ID（SQL的标准化版本），value为该分类下的记录列表
   */
  Map<String, List<CollectionRecord>> autoClassifyRecords(Long userId);
}
