package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mongodb.CollectionRecord;
import com.baoma.natural_language_query.repository.CollectionRecordRepository;
import com.baoma.natural_language_query.service.CollectionRecordService;
import com.baoma.natural_language_query.utils.SqlSimilarityUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CollectionRecordServiceImpl implements CollectionRecordService {

  @Autowired private CollectionRecordRepository collectionRecordRepository;

  @Override
  public List<CollectionRecord> listByQueryId(ObjectId queryId) {
    return collectionRecordRepository.findByQueryId(queryId);
  }

  @Override
  public List<CollectionRecord> listByUserId(Long userId) {
    return collectionRecordRepository.findByUserId(userId);
  }

  @Override
  public List<CollectionRecord> listByDbConnectionId(Long dbConnectionId) {
    return collectionRecordRepository.findByDbConnectionId(dbConnectionId);
  }

  @Override
  public CollectionRecord getById(String id) {
    return collectionRecordRepository.findById(id).orElse(null);
  }

  @Override
  public CollectionRecord save(CollectionRecord collectionRecord) {
    return collectionRecordRepository.save(collectionRecord);
  }

  @Override
  public void deleteById(String id) {
    collectionRecordRepository.deleteById(id);
  }

  @Override
  public List<CollectionRecord> findSimilarRecords(Long userId, String sqlContent, double similarityThreshold) {
    List<CollectionRecord> allRecords = collectionRecordRepository.findByUserId(userId);
    List<CollectionRecord> similarRecords = new ArrayList<>();

    for (CollectionRecord record : allRecords) {
      if (record.getSqlContent() != null) {
        double similarity = SqlSimilarityUtil.calculateSimilarity(sqlContent, record.getSqlContent());
        if (similarity >= similarityThreshold) {
          similarRecords.add(record);
        }
      }
    }

    // 按创建时间倒序排序
    similarRecords.sort(Comparator.comparing(CollectionRecord::getCreateTime).reversed());
    return similarRecords;
  }

  @Override
  public List<CollectionRecord> findSimilarRecords(Long userId, String sqlContent) {
    return findSimilarRecords(userId, sqlContent, 0.8);
  }

  @Override
  public List<CollectionRecord> getQuerySnapshots(Long userId, String sqlContent, boolean usePrompt) {
    List<CollectionRecord> allRecords = collectionRecordRepository.findByUserId(userId);
    List<CollectionRecord> snapshots = new ArrayList<>();

    for (CollectionRecord record : allRecords) {
      boolean isMatch = false;
      if (usePrompt) {
        // 使用userPrompt进行匹配
        if (record.getUserPrompt() != null && sqlContent != null) {
          isMatch = SqlSimilarityUtil.isSamePrompt(record.getUserPrompt(), sqlContent);
        }
      } else {
        // 使用SQL进行匹配
        if (record.getSqlContent() != null) {
          isMatch = SqlSimilarityUtil.isSameQuery(record.getSqlContent(), sqlContent);
        }
      }

      if (isMatch) {
        snapshots.add(record);
      }
    }

    // 按创建时间倒序排序（最新的在前）
    snapshots.sort(Comparator.comparing(CollectionRecord::getCreateTime).reversed());
    return snapshots;
  }

  @Override
  public Map<String, List<CollectionRecord>> autoClassifyRecords(Long userId) {
    List<CollectionRecord> allRecords = collectionRecordRepository.findByUserId(userId);
    Map<String, List<CollectionRecord>> classified = new HashMap<>();

    for (CollectionRecord record : allRecords) {
      if (record.getSqlContent() == null) {
        continue;
      }

      // 使用标准化的SQL作为分类键
      String normalizedSql = SqlSimilarityUtil.normalizeSql(record.getSqlContent());
      String categoryKey = normalizedSql;

      // 检查是否已有相似的分组
      boolean foundGroup = false;
      for (String key : classified.keySet()) {
        if (SqlSimilarityUtil.isSameQuery(key, normalizedSql)) {
          categoryKey = key;
          foundGroup = true;
          break;
        }
      }

      // 添加到对应分组
      classified.computeIfAbsent(categoryKey, k -> new ArrayList<>()).add(record);
    }

    // 对每个分组内的记录按时间倒序排序
    for (List<CollectionRecord> group : classified.values()) {
      group.sort(Comparator.comparing(CollectionRecord::getCreateTime).reversed());
    }

    return classified;
  }
}
