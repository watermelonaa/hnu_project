package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.QueryShare;
import com.baoma.natural_language_query.mapper.QueryShareMapper;
import com.baoma.natural_language_query.service.QueryShareService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QueryShareServiceImpl implements QueryShareService {

  @Autowired private QueryShareMapper queryShareMapper;

  @Override
  public List<QueryShare> listByReceiveUserId(Long receiveUserId) {
    QueryWrapper<QueryShare> wrapper = new QueryWrapper<>();
    wrapper.eq("receive_user_id", receiveUserId);
    return queryShareMapper.selectList(wrapper);
  }

  @Override
  public List<QueryShare> listByReceiveUserIdAndStatus(Long receiveUserId, Integer receiveStatus) {
    QueryWrapper<QueryShare> wrapper = new QueryWrapper<>();
    wrapper.eq("receive_user_id", receiveUserId).eq("receive_status", receiveStatus);
    return queryShareMapper.selectList(wrapper);
  }

  @Override
  public List<QueryShare> listByShareUserId(Long shareUserId) {
    QueryWrapper<QueryShare> wrapper = new QueryWrapper<>();
    wrapper.eq("share_user_id", shareUserId);
    return queryShareMapper.selectList(wrapper);
  }

  @Override
  public QueryShare getById(Long id) {
    return queryShareMapper.selectById(id);
  }

  @Override
  public boolean save(QueryShare queryShare) {
    return queryShareMapper.insert(queryShare) > 0;
  }

  @Override
  public boolean updateById(QueryShare queryShare) {
    return queryShareMapper.updateById(queryShare) > 0;
  }

  @Override
  public boolean removeById(Long id) {
    return queryShareMapper.deleteById(id) > 0;
  }
}
