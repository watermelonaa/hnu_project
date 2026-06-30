package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.FriendRelation;
import com.baoma.natural_language_query.entity.mysql.FriendRequest;
import com.baoma.natural_language_query.mapper.FriendRelationMapper;
import com.baoma.natural_language_query.mapper.FriendRequestMapper;
import com.baoma.natural_language_query.service.FriendRelationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FriendRelationServiceImpl implements FriendRelationService {

  @Autowired private FriendRelationMapper friendRelationMapper;

  @Autowired private FriendRequestMapper friendRequestMapper;

  @Override
  public List<FriendRelation> listByUserId(Long userId) {
    QueryWrapper<FriendRelation> wrapper = new QueryWrapper<>();
    wrapper.eq("user_id", userId);
    return friendRelationMapper.selectList(wrapper);
  }

  @Override
  public FriendRelation getById(Long id) {
    return friendRelationMapper.selectById(id);
  }

  @Override
  public FriendRelation getByUserIdAndFriendId(Long userId, Long friendId) {
    QueryWrapper<FriendRelation> wrapper = new QueryWrapper<>();
    wrapper.eq("user_id", userId).eq("friend_id", friendId);
    return friendRelationMapper.selectOne(wrapper);
  }

  @Override
  public boolean save(FriendRelation friendRelation) {
    return friendRelationMapper.insert(friendRelation) > 0;
  }

  @Override
  public boolean updateById(FriendRelation friendRelation) {
    return friendRelationMapper.updateById(friendRelation) > 0;
  }

  @Override
  public boolean removeById(Long id) {
    return friendRelationMapper.deleteById(id) > 0;
  }

  @Override
  public boolean removeByUserIdAndFriendId(Long userId, Long friendId) {
    // 删除双向好友关系
    // 1. 删除 userId -> friendId
    QueryWrapper<FriendRelation> wrapper1 = new QueryWrapper<>();
    wrapper1.eq("user_id", userId).eq("friend_id", friendId);
    friendRelationMapper.delete(wrapper1);

    // 2. 删除 friendId -> userId
    QueryWrapper<FriendRelation> wrapper2 = new QueryWrapper<>();
    wrapper2.eq("user_id", friendId).eq("friend_id", userId);
    friendRelationMapper.delete(wrapper2);

    // 3. 删除所有相关的好友请求记录（双向）
    // 删除 userId -> friendId 的请求
    QueryWrapper<FriendRequest> requestWrapper1 = new QueryWrapper<>();
    requestWrapper1.eq("applicant_id", userId).eq("recipient_id", friendId);
    friendRequestMapper.delete(requestWrapper1);

    // 删除 friendId -> userId 的请求
    QueryWrapper<FriendRequest> requestWrapper2 = new QueryWrapper<>();
    requestWrapper2.eq("applicant_id", friendId).eq("recipient_id", userId);
    friendRequestMapper.delete(requestWrapper2);

    return true;
  }
}
