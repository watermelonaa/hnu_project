package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.dto.LoginDTO;
import com.baoma.natural_language_query.vo.LoginVO;

public interface AuthService {
  LoginVO login(LoginDTO loginDTO);

  void logout(String token, Long userId);
}
