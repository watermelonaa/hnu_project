package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户搜索记录实体类
 *
 * <p>对应MySQL数据库中的user_searches表，存储用户的SQL查询历史记录。 系统会记录用户常用的SQL查询，支持查询历史、热门查询等功能。
 *
 * <p>功能说明：
 *
 * <ul>
 *   <li>记录用户执行过的SQL查询语句
 *   <li>统计查询次数（searchCount）
 *   <li>记录最后查询时间（lastSearchTime）
 *   <li>支持查询标题（queryTitle）用于快速识别
 * </ul>
 *
 * @author 项目组
 * @version 1.0
 */
@Data
@TableName("user_searches")
public class UserSearch {

  /** 搜索记录ID（主键，自增） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /**
   * 用户ID
   *
   * <p>关联User表
   */
  private Long userId;

  /**
   * SQL查询内容
   *
   * <p>存储用户执行过的SQL语句
   */
  private String sqlContent;

  /**
   * 查询标题
   *
   * <p>用户友好的查询描述，用于快速识别查询内容
   */
  private String queryTitle;

  /**
   * 查询次数
   *
   * <p>该SQL查询被执行的次数，用于统计热门查询
   */
  private Integer searchCount;

  /**
   * 最后查询时间
   *
   * <p>该SQL查询最后一次执行的时间
   */
  private LocalDateTime lastSearchTime;
}
