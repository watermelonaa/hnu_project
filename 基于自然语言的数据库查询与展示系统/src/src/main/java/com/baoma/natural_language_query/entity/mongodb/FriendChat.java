package com.baoma.natural_language_query.entity.mongodb;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "friend_chats")
public class FriendChat {

  @Id private String id;

  @Field("user_id")
  private Long userId;

  @Field("friend_id")
  private Long friendId;

  @Field("content_type")
  private String contentType;

  private Map<String, Object> content;

  @Field("send_time")
  private LocalDateTime sendTime;

  @Field("is_read")
  private Boolean isRead;

  private Map<String, Object> extra;
}
