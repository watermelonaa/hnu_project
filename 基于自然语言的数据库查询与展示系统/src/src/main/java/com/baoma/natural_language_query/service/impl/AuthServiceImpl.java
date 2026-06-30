package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.dto.LoginDTO;
import com.baoma.natural_language_query.entity.mysql.Role;
import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.mapper.RoleMapper;
import com.baoma.natural_language_query.mapper.UserMapper;
import com.baoma.natural_language_query.service.AuthService;
import com.baoma.natural_language_query.service.TokenBlacklistService;
import com.baoma.natural_language_query.utils.JwtUtil;
import com.baoma.natural_language_query.vo.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

  @Autowired private UserMapper userMapper;

  @Autowired private RoleMapper roleMapper;

  @Autowired private JwtUtil jwtUtil;

  @Autowired private AuthenticationManager authenticationManager;

  @Autowired private TokenBlacklistService tokenBlacklistService;

  @Override
  public LoginVO login(LoginDTO loginDTO) {
    // 使用 Spring Security 的 AuthenticationManager 进行认证
    // 这会调用我们自定义的 CustomUserDetailsService 来加载用户并验证密码
    // 注意：不再捕获 AuthenticationException，让它向上抛出到 Controller 层处理
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginDTO.getUsername(), loginDTO.getPassword()));

    // 认证成功后，从数据库查询完整的用户信息
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getUsername, loginDTO.getUsername());
    User user = userMapper.selectOne(wrapper);

    if (user == null) {
      throw new RuntimeException("用户不存在");
    }

    // 检查用户是否已经登录，如果已登录则使旧token失效（实现单点登录）
    tokenBlacklistService.invalidateOldToken(user.getId());

    // 查询角色信息
    Role role = roleMapper.selectById(user.getRoleId());

    // 生成新的JWT token
    String token = jwtUtil.generateToken(user.getId(), user.getUsername());

    // 保存用户当前活跃的token到Redis
    tokenBlacklistService.saveUserToken(user.getId(), token);

    // 构造返回数据
    LoginVO loginVO = new LoginVO();
    loginVO.setToken(token);
    loginVO.setUserId(user.getId());
    loginVO.setUsername(user.getUsername());
    loginVO.setEmail(user.getEmail());
    loginVO.setRoleId(user.getRoleId());
    loginVO.setRoleName(role != null ? role.getRoleName() : null);
    loginVO.setAvatarUrl(user.getAvatarUrl());

    return loginVO;
  }

  @Override
  public void logout(String token, Long userId) {
    // 将token加入黑名单，使其失效
    if (token != null && !token.isEmpty()) {
      tokenBlacklistService.addToBlacklist(token);
    }
    // 删除用户当前活跃的token记录
    if (userId != null) {
      tokenBlacklistService.removeUserToken(userId);
    }
  }
}
