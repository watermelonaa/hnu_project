package com.baoma.natural_language_query.repository;

import com.baoma.natural_language_query.entity.mongodb.CollectionRecord;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionRecordRepository extends MongoRepository<CollectionRecord, String> {

  List<CollectionRecord> findByQueryId(ObjectId queryId);

  List<CollectionRecord> findByUserId(Long userId);

  List<CollectionRecord> findByDbConnectionId(Long dbConnectionId);

  // 根据用户ID和SQL内容查找（用于查找相似查询）
  List<CollectionRecord> findByUserIdAndSqlContent(Long userId, String sqlContent);

  // 根据用户ID查找所有记录（用于自动分类）
  List<CollectionRecord> findAllByUserIdOrderByCreateTimeDesc(Long userId);
}
