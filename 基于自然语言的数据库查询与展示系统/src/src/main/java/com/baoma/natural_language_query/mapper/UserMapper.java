package com.baoma.natural_language_query.mapper;

import com.baoma.natural_language_query.entity.mysql.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {}
