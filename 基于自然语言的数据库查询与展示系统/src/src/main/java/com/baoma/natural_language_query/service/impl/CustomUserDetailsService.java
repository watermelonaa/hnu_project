package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  @Autowired private UserMapper userMapper;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getUsername, username);
    User user = userMapper.selectOne(wrapper);

    if (user == null) {
      throw new UsernameNotFoundException("用户不存在: " + username);
    }

    if (user.getStatus() == 0) {
      throw new DisabledException("账号已被禁用或锁定，请联系管理员");
    }

    return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
        .password(user.getPassword())
        .authorities(
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRoleId())))
        .accountExpired(false)
        .accountLocked(false)
        .credentialsExpired(false)
        .disabled(false)
        .build();
  }
}
