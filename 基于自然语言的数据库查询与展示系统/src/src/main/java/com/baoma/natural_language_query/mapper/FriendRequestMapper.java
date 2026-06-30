package com.baoma.natural_language_query.mapper;

import com.baoma.natural_language_query.entity.mysql.FriendRequest;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {
  /**
   * 根据申请者ID和接收者ID查询好友请求记录
   *
   * @param applicantId 申请者ID
   * @param recipientId 接收者ID
   * @return 好友请求记录（无则返回null）
   */
  @Select(
      "SELECT * FROM friend_requests WHERE applicant_id = #{applicantId} AND recipient_id = #{recipientId}")
  FriendRequest selectByApplicantAndRecipient(Long applicantId, Long recipientId);
}
