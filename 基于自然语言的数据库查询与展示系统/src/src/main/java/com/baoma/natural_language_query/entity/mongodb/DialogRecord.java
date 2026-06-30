package com.baoma.natural_language_query.entity.mongodb;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 对话记录实体类（MongoDB）
 *
 * <p>对应MongoDB数据库中的dialog_records集合，存储用户的多轮对话记录。 系统支持用户与系统进行多轮对话来优化查询，每次对话都会记录在DialogRecord中。
 *
 * <p>对话流程：
 *
 * <ol>
 *   <li>用户发起自然语言查询，系统生成对话ID
 *   <li>系统生成SQL并返回结果
 *   <li>用户根据结果调整查询内容
 *   <li>系统根据调整请求修改SQL，重新查询
 *   <li>重复步骤3-4，直到用户满意
 * </ol>
 *
 * <p>对话详情存储在DialogDetail集合中，通过dialogId关联。
 *
 * @author 项目组
 * @version 1.0
 */
@Data
@Document(collection = "dialog_records")
public class DialogRecord {

  /** 记录ID（MongoDB自动生成） */
  @Id private String id;

  /**
   * 对话ID
   *
   * <p>用于标识一次完整的对话会话，关联多个DialogDetail记录
   */
  private String dialogId;

  /**
   * 用户ID
   *
   * <p>关联User表
   */
  private Long userId;

  /**
   * 对话主题
   *
   * <p>对话的简要描述或标题
   */
  private String topic;

  /**
   * 对话轮数
   *
   * <p>该对话包含的查询轮数（用户与系统的交互次数）
   */
  private Integer totalRounds;

  /** 对话开始时间 */
  private LocalDateTime startTime;

  /** 对话最后更新时间 */
  private LocalDateTime lastTime;
}
