package com.baoma.natural_language_query.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private MongoTemplate mongoTemplate;

  @Autowired private StringRedisTemplate redisTemplate;

  @GetMapping("/hello")
  public String hello() {
    return "Hello, Spring Boot!";
  }

  @GetMapping("/mysql")
  public Map<String, Object> testMySQL() {
    Map<String, Object> result = new HashMap<>();
    try {
      // 查询 MySQL 版本
      String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
      // 查询数据库名
      String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
      // 查询表数量
      Integer tableCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ?",
              Integer.class,
              database);

      result.put("status", "success");
      result.put("version", version);
      result.put("database", database);
      result.put("tableCount", tableCount);
    } catch (Exception e) {
      result.put("status", "error");
      result.put("message", e.getMessage());
    }
    return result;
  }

  @GetMapping("/mongodb")
  public Map<String, Object> testMongoDB() {
    Map<String, Object> result = new HashMap<>();
    try {
      // 获取数据库名
      String dbName = mongoTemplate.getDb().getName();
      // 获取集合数量
      int collectionCount =
          mongoTemplate.getDb().listCollectionNames().into(new java.util.ArrayList<>()).size();

      result.put("status", "success");
      result.put("database", dbName);
      result.put("collectionCount", collectionCount);
      
      // 测试保存 DialogRecord
      try {
        com.baoma.natural_language_query.entity.mongodb.DialogRecord testRecord = 
            new com.baoma.natural_language_query.entity.mongodb.DialogRecord();
        testRecord.setDialogId("test_" + System.currentTimeMillis());
        testRecord.setUserId(999L);
        testRecord.setTopic("测试对话");
        testRecord.setTotalRounds(1);
        testRecord.setStartTime(java.time.LocalDateTime.now());
        testRecord.setLastTime(java.time.LocalDateTime.now());
        
        com.baoma.natural_language_query.entity.mongodb.DialogRecord saved = 
            mongoTemplate.save(testRecord, "dialog_records");
        result.put("dialogRecordTest", "success");
        result.put("savedDialogId", saved.getId());
        
        // 删除测试记录
        mongoTemplate.remove(saved, "dialog_records");
        result.put("cleanup", "success");
      } catch (Exception e) {
        result.put("dialogRecordTest", "error");
        result.put("dialogRecordError", e.getMessage());
        e.printStackTrace();
      }
    } catch (Exception e) {
      result.put("status", "error");
      result.put("message", e.getMessage());
      e.printStackTrace();
    }
    return result;
  }

  @GetMapping("/redis")
  public Map<String, Object> testRedis() {
    Map<String, Object> result = new HashMap<>();
    try {
      // 测试写入
      redisTemplate.opsForValue().set("test:key", "test_value");
      // 测试读取
      String value = redisTemplate.opsForValue().get("test:key");
      // 删除测试数据
      redisTemplate.delete("test:key");

      result.put("status", "success");
      result.put("message", "Redis 连接正常");
      result.put("testValue", value);
    } catch (Exception e) {
      result.put("status", "error");
      result.put("message", e.getMessage());
    }
    return result;
  }

  @GetMapping("/all")
  public Map<String, Object> testAll() {
    Map<String, Object> result = new HashMap<>();
    result.put("mysql", testMySQL());
    result.put("mongodb", testMongoDB());
    result.put("redis", testRedis());
    return result;
  }
}
