package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.entity.mysql.UserSearch;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;

public interface UserService {

  User getById(Long id);

  List<User> list();

  Page<User> page(int current, int size);

  boolean save(User user);

  boolean updateById(User user);

  boolean removeById(Long id);

  User getByUsername(String username);

  List<User> searchByEmail(String email);

  User getByEmail(String email);

  User searchByPhoneNumber(String phoneNumber);

  boolean changePassword(Long userId, String oldPassword, String newPassword);

  // UserSearch相关方法
  List<UserSearch> listUserSearchesByUserId(Long userId);

  List<UserSearch> listTopUserSearchesByUserId(Long userId, int limit);

  UserSearch getUserSearchByUserIdAndSqlContent(Long userId, String sqlContent);

  boolean saveUserSearch(UserSearch userSearch);

  boolean updateUserSearchById(UserSearch userSearch);

  boolean removeUserSearchById(Long id);
  /**
   * 重置密码（不校验旧密码）
   * @param userId 用户ID
   * @param newPassword 新密码
   * @return 是否重置成功
   */
  boolean resetPassword(Long userId, String newPassword);
}
