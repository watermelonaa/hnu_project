package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.UserDbPermission;
import com.baoma.natural_language_query.mapper.UserDbPermissionMapper;
import com.baoma.natural_language_query.service.UserDbPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserDbPermissionServiceImpl
    extends ServiceImpl<UserDbPermissionMapper, UserDbPermission>
    implements UserDbPermissionService {

  @Override
  public UserDbPermission getByUserId(Long userId) {
    LambdaQueryWrapper<UserDbPermission> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(UserDbPermission::getUserId, userId);
    return getOne(wrapper);
  }

  @Override
  public List<UserDbPermission> listAssigned() {
    LambdaQueryWrapper<UserDbPermission> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(UserDbPermission::getIsAssigned, 1);
    wrapper.orderByDesc(UserDbPermission::getLastGrantTime);
    return list(wrapper);
  }

  @Override
  public List<UserDbPermission> listUnassigned() {
    LambdaQueryWrapper<UserDbPermission> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(UserDbPermission::getIsAssigned, 0);
    return list(wrapper);
  }

  @Override
  public boolean saveOrUpdate(UserDbPermission entity) {
    // 根据user_id判断是否存在记录（不依赖entity.getId()）
    if (entity.getUserId() != null) {
      UserDbPermission existing = getByUserId(entity.getUserId());
      if (existing != null) {
        // 已存在，更新现有记录
        entity.setId(existing.getId());
        return updateById(entity);
      }
    }

    // 新增记录
    // 强制设置ID为null，确保执行INSERT
    entity.setId(null);
    return save(entity);
  }
}
