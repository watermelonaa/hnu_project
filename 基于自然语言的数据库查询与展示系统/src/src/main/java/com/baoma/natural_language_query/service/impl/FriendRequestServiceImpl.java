package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.FriendRelation;
import com.baoma.natural_language_query.entity.mysql.FriendRequest;
import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.mapper.FriendRequestMapper;
import com.baoma.natural_language_query.service.FriendRelationService;
import com.baoma.natural_language_query.service.FriendRequestService;
import com.baoma.natural_language_query.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendRequestServiceImpl implements FriendRequestService {

  @Autowired private FriendRequestMapper friendRequestMapper;

  @Autowired private FriendRelationService friendRelationService;

  @Autowired private UserService userService;

  @Override
  public List<FriendRequest> listByRecipientId(Long recipientId) {
    QueryWrapper<FriendRequest> wrapper = new QueryWrapper<>();
    wrapper.eq("recipient_id", recipientId);
    return friendRequestMapper.selectList(wrapper);
  }

  @Override
  public List<FriendRequest> listByRecipientIdAndStatus(Long recipientId, Integer status) {
    QueryWrapper<FriendRequest> wrapper = new QueryWrapper<>();
    wrapper.eq("recipient_id", recipientId).eq("status", status);
    return friendRequestMapper.selectList(wrapper);
  }

  @Override
  public FriendRequest getById(Long id) {
    return friendRequestMapper.selectById(id);
  }

  @Override
  public boolean save(FriendRequest friendRequest) {
    // 1. 检查是否给自己发送好友请求
    if (friendRequest.getApplicantId().equals(friendRequest.getRecipientId())) {
      throw new RuntimeException("不能添加自己为好友");
    }

    // 2. 检查是否已经是好友关系
    FriendRelation existingRelation =
        friendRelationService.getByUserIdAndFriendId(
            friendRequest.getApplicantId(), friendRequest.getRecipientId());
    if (existingRelation != null) {
      throw new RuntimeException("已经是好友，无需添加");
    }

    // 3. 核心修改：查询是否存在相同申请者+接收者的请求记录
    FriendRequest existingRequest =
        friendRequestMapper.selectByApplicantAndRecipient(
            friendRequest.getApplicantId(), friendRequest.getRecipientId());

    if (existingRequest != null) {
      // 3.1 存在历史请求：按状态处理
      if (existingRequest.getStatus() == 0) {
        // 状态为待处理：不允许重复发送
        throw new RuntimeException("已向该用户发送好友请求，等待对方处理");
      } else {
        // 状态为已拒绝(2)/已接受(1)：更新原记录为待处理（重置请求）
        existingRequest.setStatus(0); // 重置为待处理
        existingRequest.setApplyMsg(friendRequest.getApplyMsg()); // 更新申请消息
        existingRequest.setCreateTime(LocalDateTime.now()); // 重置创建时间
        existingRequest.setHandleTime(null); // 清空处理时间
        return friendRequestMapper.updateById(existingRequest) > 0;
      }
    } else {
      // 3.2 无历史请求：正常插入新记录
      friendRequest.setCreateTime(LocalDateTime.now()); // 补充创建时间（若前端未传）
      friendRequest.setStatus(0); // 显式设置初始状态为待处理
      return friendRequestMapper.insert(friendRequest) > 0;
    }
  }

  @Override
  public boolean updateById(FriendRequest friendRequest) {
    return friendRequestMapper.updateById(friendRequest) > 0;
  }

  @Override
  public boolean removeById(Long id) {
    return friendRequestMapper.deleteById(id) > 0;
  }

  @Override
  @Transactional
  public boolean acceptRequest(Long requestId) {
    // 1. 查询好友请求
    FriendRequest request = friendRequestMapper.selectById(requestId);
    if (request == null) {
      throw new RuntimeException("好友请求不存在");
    }

    if (request.getStatus() != 0) {
      throw new RuntimeException("该请求已被处理");
    }

    // 检查是否给自己发送好友请求
    if (request.getApplicantId().equals(request.getRecipientId())) {
      throw new RuntimeException("不能添加自己为好友");
    }

    // 2. 更新请求状态为已接受(1)
    request.setStatus(1);
    request.setHandleTime(LocalDateTime.now());
    friendRequestMapper.updateById(request);

    // 3. 获取用户信息
    User applicant = userService.getById(request.getApplicantId());
    User recipient = userService.getById(request.getRecipientId());

    if (applicant == null || recipient == null) {
      throw new RuntimeException("用户不存在");
    }

    // 4. 创建双向好友关系
    // 申请人 -> 接收人
    FriendRelation relation1 = new FriendRelation();
    relation1.setUserId(request.getApplicantId());
    relation1.setFriendId(request.getRecipientId());
    relation1.setFriendUsername(recipient.getUsername());
    relation1.setOnlineStatus(recipient.getOnlineStatus());
    relation1.setCreateTime(LocalDateTime.now());
    friendRelationService.save(relation1);

    // 接收人 -> 申请人
    FriendRelation relation2 = new FriendRelation();
    relation2.setUserId(request.getRecipientId());
    relation2.setFriendId(request.getApplicantId());
    relation2.setFriendUsername(applicant.getUsername());
    relation2.setOnlineStatus(applicant.getOnlineStatus());
    relation2.setCreateTime(LocalDateTime.now());
    friendRelationService.save(relation2);

    return true;
  }

  @Override
  public boolean rejectRequest(Long requestId) {
    // 查询好友请求
    FriendRequest request = friendRequestMapper.selectById(requestId);
    if (request == null) {
      throw new RuntimeException("好友请求不存在");
    }

    if (request.getStatus() != 0) {
      throw new RuntimeException("该请求已被处理");
    }

    // 更新请求状态为已拒绝(2)
    request.setStatus(2);
    request.setHandleTime(LocalDateTime.now());
    return friendRequestMapper.updateById(request) > 0;
  }
}
