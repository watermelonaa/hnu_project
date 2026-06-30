package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.entity.mysql.UserSearch;
import com.baoma.natural_language_query.mapper.UserMapper;
import com.baoma.natural_language_query.mapper.UserSearchMapper;
import com.baoma.natural_language_query.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

  @Autowired private UserMapper userMapper;

  @Autowired private UserSearchMapper userSearchMapper;

  @Autowired private PasswordEncoder passwordEncoder;

  @Override
  public User getById(Long id) {
    return userMapper.selectById(id);
  }

  @Override
  public List<User> list() {
    return userMapper.selectList(null);
  }

  @Override
  public Page<User> page(int current, int size) {
    Page<User> page = new Page<>(current, size);
    return userMapper.selectPage(page, null);
  }

  @Override
  public boolean save(User user) {
    if (StringUtils.hasText(user.getPassword())) {
      user.setPassword(passwordEncoder.encode(user.getPassword()));
    }
    return userMapper.insert(user) > 0;
  }

  @Override
  public boolean updateById(User user) {
    if (StringUtils.hasText(user.getPassword())) {
      user.setPassword(passwordEncoder.encode(user.getPassword()));
    }
    return userMapper.updateById(user) > 0;
  }

  @Override
  public boolean removeById(Long id) {
    return userMapper.deleteById(id) > 0;
  }

  @Override
  public User getByUsername(String username) {
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getUsername, username);
    return userMapper.selectOne(wrapper);
  }

  @Override
  public List<User> searchByEmail(String email) {
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.like(User::getEmail, email);
    return userMapper.selectList(wrapper);
  }

  @Override
  public User getByEmail(String email) {
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getEmail, email);
    return userMapper.selectOne(wrapper);
  }

  @Override
  public User searchByPhoneNumber(String phoneNumber) {
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getPhonenumber, phoneNumber);
    return userMapper.selectOne(wrapper);
  }

  @Override
  public boolean changePassword(Long userId, String oldPassword, String newPassword) {
    // 1. 查询用户
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new RuntimeException("用户不存在");
    }

    // 2. 验证旧密码（如果提供了旧密码）
    if (oldPassword != null && !passwordEncoder.matches(oldPassword, user.getPassword())) {
      throw new RuntimeException("当前密码错误");
    }

    // 3. 加密新密码
    String encodedNewPassword = passwordEncoder.encode(newPassword);

    // 4. 更新密码
    user.setPassword(encodedNewPassword);
    return userMapper.updateById(user) > 0;
  }

  // UserSearch相关方法实现
  @Override
  public List<UserSearch> listUserSearchesByUserId(Long userId) {
    QueryWrapper<UserSearch> wrapper = new QueryWrapper<>();
    wrapper.eq("user_id", userId);
    return userSearchMapper.selectList(wrapper);
  }

  @Override
  public List<UserSearch> listTopUserSearchesByUserId(Long userId, int limit) {
    QueryWrapper<UserSearch> wrapper = new QueryWrapper<>();
    wrapper.eq("user_id", userId).orderByDesc("search_count").last("LIMIT " + limit);
    return userSearchMapper.selectList(wrapper);
  }

  @Override
  public UserSearch getUserSearchByUserIdAndSqlContent(Long userId, String sqlContent) {
    QueryWrapper<UserSearch> wrapper = new QueryWrapper<>();
    wrapper.eq("user_id", userId).eq("sql_content", sqlContent);
    return userSearchMapper.selectOne(wrapper);
  }

  @Override
  public boolean saveUserSearch(UserSearch userSearch) {
    return userSearchMapper.insert(userSearch) > 0;
  }

  @Override
  public boolean updateUserSearchById(UserSearch userSearch) {
    return userSearchMapper.updateById(userSearch) > 0;
  }

  @Override
  public boolean removeUserSearchById(Long id) {
    return userSearchMapper.deleteById(id) > 0;
  }

  @Override
  public boolean resetPassword(Long userId, String newPassword) {
    return changePassword(userId, null, newPassword);
  }
}
