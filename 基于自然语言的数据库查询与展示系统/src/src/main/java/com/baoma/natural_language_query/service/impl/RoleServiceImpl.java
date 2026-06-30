package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.Role;
import com.baoma.natural_language_query.mapper.RoleMapper;
import com.baoma.natural_language_query.service.RoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {}
