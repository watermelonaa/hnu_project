### 一、完整表/集合清单（按MySQL + MongoDB分类）
#### （一）MySQL 8.4 数据表（结构化数据）
##### 1. 基础信息表（用户、角色、字典）
1.1 users（用户表）  
1.2 roles（角色表）  
1.3 db_types（数据库类型表）  
1.4 notification_targets（通知目标表）  
1.5 priorities（通知优先级表）  
1.6 error_types（错误类型表）  
1.7 llm_status（大模型状态表）  

##### 2. 数据资源表（数据库、表、字段元数据）
2.1 db_connections（数据库连接表）  
2.2 table_metadata（表元数据表）  
2.3 column_metadata（字段元数据表）  

##### 3. 权限与关系表（用户权限、好友关系）
3.1 user_db_permissions（用户数据权限表）  
3.2 friend_relations（好友关系表）  
3.3 friend_requests（好友请求表）  
3.4 query_shares（查询分享记录表）  

##### 4. 系统日志与监控表
4.1 system_health（系统健康表）  
4.2 operation_logs（系统操作日志表）  
4.3 llm_configs（大模型配置表）  
4.4 notifications（通知表）  
4.5 token_consume（token消耗表）  
4.6 error_logs（错误分析表）  
4.7 performance_metrics（性能趋势表）  
4.8 db_connection_logs（数据库连接日志表）  
4.9 query_logs（查询日志表）  
4.10 user_searches（用户搜索表）  

#### （二）MongoDB 8.2 集合（非结构化数据）
1. query_collections（收藏查询表-组）  
2. collection_records（收藏记录表-具体记录）  
3. dialog_records（多轮对话列表）  
4. dialog_details（多轮对话具体内容表）  
5. sql_cache（SQL缓存集合）  
6. ai_interaction_logs（AI交互日志集合）  
7. friend_chats（好友聊天记录表）  

---

### 二、表/集合详细字段（严格遵循文档）
#### （一）MySQL 数据表字段
##### 1. 基础信息表
###### 1.1 users（用户表）
| 字段代码 | 字段名       | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                  |
|----------|--------------|-------------|------------|--------------------------|--------------------------------------------|-----------------------------------------------------------|
| id       | 用户ID       | bigint(20)  | 是（自增） | -                        | 唯一标识用户                               | PRIMARY KEY (id)                                           |
| username | 用户名       | varchar(50) | -          | -                        | 登录账号，唯一                               | UNIQUE (username), NOT NULL                               |
| password | 密码         | varchar(100)| -          | -                        | BCrypt加密存储                              | NOT NULL                                                   |
| email    | 邮箱         | varchar(100)| -          | -                        | 用于好友添加、通知                           | UNIQUE (email), NOT NULL                                   |
| phonenumber | 手机号   | varchar(50) | -          | -                        | 用户绑定的手机号                             | UNIQUE (phonenumber), NOT NULL                              |
| role_id  | 角色ID       | tinyint(2)  | -          | 是（关联roles.id）        | 关联角色类型                                 | NOT NULL, FOREIGN KEY (role_id) REFERENCES roles(id)        |
| avatar_url | 头像URL  | varchar(255)| -          | -                        | 头像路径，默认"/default-avatar.png"          | DEFAULT '/default-avatar.png'                               |
| status   | 账号状态     | tinyint(1)  | -          | -                        | 0-禁用，1-正常                              | DEFAULT 1, NOT NULL                                       |
| online_status | 在线状态 | tinyint(1)  | -          | -                        | 0-离线，1-在线                              | DEFAULT 0, NOT NULL                                       |
| create_time | 创建时间 | datetime    | -          | -                        | 账号创建时间（系统管理员创建）               | DEFAULT CURRENT_TIMESTAMP, NOT NULL                        |
| update_time | 更新时间 | datetime    | -          | -                        | 信息更新时间                                 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, NOT NULL |

###### 1.2 roles（角色表）
| 字段代码 | 字段名       | 数据类型    | 主键       | 外键 | 字段描述                                   | 约束条件                  |
|----------|--------------|-------------|------------|------|--------------------------------------------|---------------------------|
| id       | 角色ID       | tinyint(2)  | 是（自增） | -    | 唯一标识角色                               | PRIMARY KEY (id)          |
| role_name | 角色名称    | varchar(30) | -          | -    | 如"系统管理员"、"数据管理员"、"普通用户"     | UNIQUE (role_name), NOT NULL |
| role_code | 角色编码    | varchar(20) | -          | -    | 如"sys_admin"、"data_admin"、"normal_user" | UNIQUE (role_code), NOT NULL |
| description | 角色描述 | varchar(500)| -          | -    | 角色权限范围说明                           | -                        |

###### 1.3 db_types（数据库类型表）
| 字段代码 | 字段名       | 数据类型    | 主键       | 外键 | 字段描述                                   | 约束条件                  |
|----------|--------------|-------------|------------|------|--------------------------------------------|---------------------------|
| id       | 类型ID       | tinyint(2)  | 是（自增） | -    | 唯一标识数据库类型                         | PRIMARY KEY (id)          |
| type_name | 类型名称    | varchar(50) | -          | -    | 如"MySQL"、"MongoDB"、"SQL Server"          | UNIQUE (type_name), NOT NULL |
| type_code | 类型编码    | varchar(20) | -          | -    | 如"mysql"、"mongodb"、"mssql"                | UNIQUE (type_code), NOT NULL |
| description | 类型描述 | varchar(500)| -          | -    | 数据库特性说明                             | -                        |

###### 1.4 notification_targets（通知目标表）
| 字段代码 | 字段名       | 数据类型    | 主键       | 外键 | 字段描述                                   | 约束条件                  |
|----------|--------------|-------------|------------|------|--------------------------------------------|---------------------------|
| id       | 目标ID       | tinyint(2)  | 是（自增） | -    | 唯一标识通知目标                           | PRIMARY KEY (id)          |
| target_name | 目标名称  | varchar(30) | -          | -    | 如"所有用户"、"普通用户"                   | UNIQUE (target_name), NOT NULL |
| target_code | 目标编码  | varchar(20) | -          | -    | 如"all"、"normal_user"                     | UNIQUE (target_code), NOT NULL |
| description | 目标描述 | varchar(200)| -          | -    | 接收范围说明                               | -                        |

###### 1.5 priorities（通知优先级表）
| 字段代码 | 字段名       | 数据类型    | 主键       | 外键 | 字段描述                                   | 约束条件                  |
|----------|--------------|-------------|------------|------|--------------------------------------------|---------------------------|
| id       | 优先级ID     | tinyint(2)  | 是（自增） | -    | 唯一标识优先级                             | PRIMARY KEY (id)          |
| priority_name | 优先级名称 | varchar(20) | -          | -    | 如"紧急"、"普通"、"低"                     | UNIQUE (priority_name), NOT NULL |
| priority_code | 优先级编码 | varchar(20) | -          | -    | 如"urgent"、"normal"、"low"                | UNIQUE (priority_code), NOT NULL |
| sort     | 排序权重     | int(11)     | -          | -    | 数值越小越优先展示（1-紧急，2-普通）       | NOT NULL                  |

###### 1.6 error_types（错误类型表）
| 字段代码 | 字段名       | 数据类型    | 主键       | 外键 | 字段描述                                   | 约束条件                  |
|----------|--------------|-------------|------------|------|--------------------------------------------|---------------------------|
| id       | 错误ID       | tinyint(2)  | 是（自增） | -    | 唯一标识错误类型                           | PRIMARY KEY (id)          |
| error_name | 错误名称  | varchar(50) | -          | -    | 如"模型调用超时"、"数据库连接错误"         | UNIQUE (error_name), NOT NULL |
| error_code | 错误编码  | varchar(50) | -          | -    | 如"llm_timeout"、"db_connection_error"      | UNIQUE (error_code), NOT NULL |
| description | 错误描述 | varchar(500)| -          | -    | 错误原因说明                               | -                        |

###### 1.7 llm_status（大模型状态表）
| 字段代码 | 字段名       | 数据类型    | 主键       | 外键 | 字段描述                                   | 约束条件                  |
|----------|--------------|-------------|------------|------|--------------------------------------------|---------------------------|
| id       | 状态ID       | tinyint(2)  | 是（自增） | -    | 唯一标识大模型状态                         | PRIMARY KEY (id)          |
| status_name | 状态名称  | varchar(20) | -          | -    | 如"可用"、"不可用"、"不稳定"               | UNIQUE (status_name), NOT NULL |
| status_code | 状态编码  | varchar(20) | -          | -    | 如"available"、"unavailable"、"unstable"    | UNIQUE (status_code), NOT NULL |
| description | 状态描述 | varchar(200)| -          | -    | 状态含义说明（如"可用：API成功率≥95%"）     | -                        |

##### 2. 数据资源表
###### 2.1 db_connections（数据库连接表）
| 字段代码 | 字段名       | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                  |
|----------|--------------|-------------|------------|--------------------------|--------------------------------------------|-----------------------------------------------------------|
| id       | 连接ID       | bigint(20)  | 是（自增） | -                        | 唯一标识数据库连接                           | PRIMARY KEY (id)                                           |
| name     | 连接名称     | varchar(100)| -          | -                        | 用户自定义名称（如"订单主库"）               | UNIQUE (name), NOT NULL                                   |
| db_type_id | 数据库类型ID | tinyint(2)  | -          | 是（关联db_types.id）     | 关联数据库类型                               | NOT NULL, FOREIGN KEY (db_type_id) REFERENCES db_types(id)  |
| url      | 连接地址     | varchar(255)| -          | -                        | 如"192.168.1.101:3306/orders_db"           | NOT NULL                                                   |
| username | 数据库账号   | varchar(50) | -          | -                        | 访问数据库的账号（加密存储）                 | NOT NULL                                                   |
| password | 数据库密码   | varchar(100)| -          | -                        | 访问数据库的密码（加密存储）                 | NOT NULL                                                   |
| status   | 连接状态     | varchar(20) | -          | -                        | "connected"-已连接、"error"-连接错误、"disabled"-已禁用 | DEFAULT 'disconnected', NOT NULL                          |
| create_user_id | 创建者ID | bigint(20)  | -          | 是（关联users.id）        | 仅数据管理员可创建                           | NOT NULL, FOREIGN KEY (create_user_id) REFERENCES users(id) |
| create_time | 创建时间 | datetime    | -          | -                        | 连接创建时间                                 | DEFAULT CURRENT_TIMESTAMP, NOT NULL                        |
| update_time | 更新时间 | datetime    | -          | -                        | 连接信息更新时间                             | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, NOT NULL |

###### 2.2 table_metadata（表元数据表）
| 字段代码 | 字段名           | 数据类型    | 主键       | 外键                         | 字段描述                                   | 约束条件                                                      |
|----------|------------------|-------------|------------|------------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 表ID             | bigint(20)  | 是（自增） | -                            | 唯一标识表元数据                           | PRIMARY KEY (id)                                               |
| db_connection_id | 数据库连接ID   | bigint(20)  | -          | 是（关联db_connections.id）   | 所属数据库连接                             | NOT NULL, FOREIGN KEY (db_connection_id) REFERENCES db_connections(id) ON DELETE CASCADE |
| table_name | 表名           | varchar(100)| -          | -                            | 数据库中实际表名（如"orders_2023"）        | NOT NULL                                                       |
| description | 表描述        | varchar(500)| -          | -                            | 用于AI理解表含义（如"存储2023年订单数据"） | -                                                            |
| create_time | 创建时间     | datetime    | -          | -                            | 元数据录入时间                             | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |
| update_time | 更新时间     | datetime    | -          | -                            | 元数据更新时间                             | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, NOT NULL |

###### 2.3 column_metadata（字段元数据表）
| 字段代码 | 字段名           | 数据类型    | 主键       | 外键                       | 字段描述                                   | 约束条件                                                      |
|----------|------------------|-------------|------------|----------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 字段ID           | bigint(20)  | 是（自增） | -                          | 唯一标识字段元数据                           | PRIMARY KEY (id)                                               |
| table_id | 表ID             | bigint(20)  | -          | 是（关联table_metadata.id） | 所属表                                     | NOT NULL, FOREIGN KEY (table_id) REFERENCES table_metadata(id) ON DELETE CASCADE |
| column_name | 字段名       | varchar(100)| -          | -                          | 表中实际字段名（如"order_amount"）          | NOT NULL                                                       |
| data_type | 数据类型         | varchar(50) | -          | -                          | 字段数据类型（如"int"、"varchar(50)"）       | NOT NULL                                                       |
| description | 字段描述    | varchar(500)| -          | -                          | 用于AI理解字段含义（如"订单金额，单位：元"） | -                                                            |
| is_primary | 是否主键     | tinyint(1)  | -          | -                          | 0-否，1-是                                | DEFAULT 0, NOT NULL                                           |
| create_time | 创建时间     | datetime    | -          | -                          | 元数据录入时间                             | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |

##### 3. 权限与关系表
###### 3.1 user_db_permissions（用户数据权限表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|--------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 权限ID               | bigint(20)  | 是（自增） | -                        | 唯一标识权限记录                           | PRIMARY KEY (id)                                               |
| user_id  | 用户ID               | bigint(20)  | -          | 是（关联users.id）        | 被授权用户（普通用户）                       | NOT NULL, FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE |
| permission_details | 权限详情       | json        | -          | -                        | 可访问的表列表，格式：[{"db_connection_id":1, "table_ids":[1,2]},...] | NOT NULL |
| last_grant_user_id | 最后授权管理员ID | bigint(20)  | -          | 是（关联users.id）        | 最后修改权限的数据管理员/系统管理员         | NOT NULL, FOREIGN KEY (last_grant_user_id) REFERENCES users(id) |
| is_assigned | 是否已分配     | tinyint(1)  | -          | -                        | 0-未分配，1-已分配（默认0）                 | DEFAULT 0, NOT NULL                                           |
| last_grant_time | 最后授权时间   | datetime    | -          | -                        | 最后一次权限修改时间                       | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |
| update_time | 更新时间         | datetime    | -          | -                        | 记录更新时间                               | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, NOT NULL |

###### 3.2 friend_relations（好友关系表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|--------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 关系ID               | bigint(20)  | 是（自增） | -                        | 唯一标识好友关系                           | PRIMARY KEY (id)                                               |
| user_id  | 用户ID               | bigint(20)  | -          | 是（关联users.id）        | 主动添加方                                 | NOT NULL, FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE |
| friend_id | 好友ID               | bigint(20)  | -          | 是（关联users.id）        | 被动添加方                                 | NOT NULL, FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE |
| friend_username | 好友用户名     | varchar(50) | -          | -                        | 冗余存储，便于列表展示                     | NOT NULL                                                       |
| online_status | 好友在线状态   | tinyint(1)  | -          | -                        | 0-离线，1-在线                             | DEFAULT 0, NOT NULL                                           |
| remark_name | 备注名             | varchar(50) | -          | -                        | 用户对好友的自定义备注                     | -                                                            |
| create_time | 添加时间           | datetime    | -          | -                        | 好友关系建立时间                           | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |

###### 3.3 friend_requests（好友请求表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|--------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 请求ID               | bigint(20)  | 是（自增） | -                        | 唯一标识好友请求                           | PRIMARY KEY (id)                                               |
| applicant_id | 申请人ID         | bigint(20)  | -          | 是（关联users.id）        | 发起请求的用户                             | NOT NULL, FOREIGN KEY (applicant_id) REFERENCES users(id) ON DELETE CASCADE |
| recipient_id | 接收人ID         | bigint(20)  | -          | 是（关联users.id）        | 收到请求的用户                             | NOT NULL, FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE |
| apply_msg | 申请留言             | varchar(200)| -          | -                        | 申请人留言（如"我是张三，加个好友"）         | -                                                            |
| status   | 请求状态             | tinyint(1)  | -          | -                        | 0-待处理，1-已同意，2-已拒绝                 | DEFAULT 0, NOT NULL                                           |
| create_time | 申请时间           | datetime    | -          | -                        | 请求发起时间                               | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |
| handle_time | 处理时间           | datetime    | -          | -                        | 接收人处理时间（未处理为NULL）               | -                                                            |

###### 3.4 query_shares（查询分享记录表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|--------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 分享ID               | bigint(20)  | 是（自增） | -                        | 唯一标识分享记录                           | PRIMARY KEY (id)                                               |
| share_user_id | 分享人ID         | bigint(20)  | -          | 是（关联users.id）        | 发起分享的用户                             | NOT NULL, FOREIGN KEY (share_user_id) REFERENCES users(id) ON DELETE CASCADE |
| receive_user_id | 接收人ID         | bigint(20)  | -          | 是（关联users.id）        | 接收分享的好友                             | NOT NULL, FOREIGN KEY (receive_user_id) REFERENCES users(id) ON DELETE CASCADE |
| dialog_id | 会话ID               | varchar(50) | -          | -                        | 关联MongoDB的dialog_details集合，定位具体对话文档 | NOT NULL                                                       |
| target_rounds | 会话轮次数组     | json        | -          | -                        | 筛选指定轮次序号(roundNum)的元素            | NOT NULL                                                       |
| query_title | 查询标题           | varchar(200)| -          | -                        | 冗余存储，便于接收人查看                     | NOT NULL                                                       |
| share_time | 分享时间           | datetime    | -          | -                        | 分享发起时间                               | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |
| receive_status | 接收状态         | tinyint(1)  | -          | -                        | 0-未处理，1-已保存，2-已删除                 | DEFAULT 0, NOT NULL                                           |

##### 4. 系统日志与监控表
###### 4.1 system_health（系统健康表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键 | 字段描述                                   | 约束条件                  |
|----------|----------------------|-------------|------------|------|--------------------------------------------|---------------------------|
| id       | 记录ID               | bigint(20)  | 是（自增） | -    | 唯一标识健康记录                           | PRIMARY KEY (id)          |
| db_delay | 数据库延迟           | int(11)     | -          | -    | 数据库服务响应延迟（ms）                     | NOT NULL                  |
| cache_delay | 缓存延迟         | int(11)     | -          | -    | 缓存服务响应延迟（ms）                     | NOT NULL                  |
| llm_delay | 大模型延迟           | int(11)     | -          | -    | 大模型API响应延迟（ms）                     | NOT NULL                  |
| storage_usage | 存储使用率     | decimal(5,2)| -          | -    | 存储服务使用率（%，如95.50）               | NOT NULL                  |
| collect_time | 采集时间         | datetime    | -          | -    | 数据采集时间                               | DEFAULT CURRENT_TIMESTAMP, NOT NULL |

###### 4.2 operation_logs（系统操作日志表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|--------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 日志ID               | bigint(20)  | 是（自增） | -                        | 唯一标识操作日志                           | PRIMARY KEY (id)                                               |
| user_id  | 用户ID               | bigint(20)  | -          | 是（关联users.id）        | 操作人ID（匿名操作为0）                     | NOT NULL, FOREIGN KEY (user_id) REFERENCES users(id)           |
| username | 用户名               | varchar(50) | -          | -                        | 操作人用户名（冗余，便于查询）             | NOT NULL                                                       |
| operation | 操作名称             | varchar(100)| -          | -                        | 如"创建数据库连接"、"分配用户权限"         | NOT NULL                                                       |
| module   | 操作模块             | varchar(50) | -          | -                        | 如"数据源管理"、"用户管理"                 | NOT NULL                                                       |
| related_llm | 涉及模型         | varchar(50) | -          | -                        | 关联大模型名称（无则为NULL）               | -                                                            |
| ip_address | IP地址           | varchar(50) | -          | -                        | 操作人IP地址                               | NOT NULL                                                       |
| operate_time | 操作时间         | datetime    | -          | -                        | 操作执行时间                               | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |
| result   | 操作结果             | tinyint(1)  | -          | -                        | 0-失败，1-成功                             | NOT NULL                                                       |
| error_msg | 错误信息             | text        | -          | -                        | 失败原因（成功为NULL）                     | -                                                            |

###### 4.3 llm_configs（大模型配置表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|--------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 配置ID               | bigint(20)  | 是（自增） | -                        | 唯一标识大模型配置                           | PRIMARY KEY (id)                                               |
| name     | 模型名称             | varchar(50) | -          | -                        | 如"智谱AI"、"通义千问"                       | UNIQUE (name), NOT NULL                                       |
| version  | 模型版本             | varchar(20) | -          | -                        | 如"4.0"、"3.0"                               | NOT NULL                                                       |
| api_key  | API密钥              | varchar(200)| -          | -                        | 大模型API Key（AES加密）                     | NOT NULL                                                       |
| api_url  | API地址              | varchar(255)| -          | -                        | 调用地址（如"https://api.zhipuai.com/v4/chat/completions"） | NOT NULL |
| status_id | 状态ID               | tinyint(2)  | -          | 是（关联llm_status.id）  | 模型状态（可用/不可用/不稳定）               | NOT NULL, FOREIGN KEY (status_id) REFERENCES llm_status(id)    |
| is_disabled | 是否禁用         | tinyint(1)  | -          | -                        | 0-启用，1-禁用（强制下线）                   | DEFAULT 0, NOT NULL                                           |
| timeout  | 超时时间             | int(11)     | -          | -                        | API调用超时阈值（ms，如5000）               | NOT NULL                                                       |
| create_user_id | 创建人ID         | bigint(20)  | -          | 是（关联users.id）        | 仅系统管理员可创建                           | NOT NULL, FOREIGN KEY (create_user_id) REFERENCES users(id)    |
| create_time | 创建时间         | datetime    | -          | -                        | 配置创建时间                               | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |
| update_time | 更新时间         | datetime    | -          | -                        | 配置更新时间                               | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, NOT NULL |

###### 4.4 notifications（通知表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|--------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 通知ID               | bigint(20)  | 是（自增） | -                        | 唯一标识通知                               | PRIMARY KEY (id)                                               |
| title    | 通知标题             | varchar(100)| -          | -                        | 通知标题（如"系统维护通知"）                 | NOT NULL                                                       |
| content  | 通知内容             | text        | -          | -                        | 通知详细内容（支持简单HTML）                 | NOT NULL                                                       |
| target_id | 目标ID               | tinyint(2)  | -          | 是（关联notification_targets.id） | 接收目标（所有用户/指定角色）               | NOT NULL, FOREIGN KEY (target_id) REFERENCES notification_targets(id) |
| priority_id | 优先级ID         | tinyint(2)  | -          | 是（关联priorities.id）   | 通知优先级（紧急/普通/低）                   | NOT NULL, FOREIGN KEY (priority_id) REFERENCES priorities(id)  |
| publisher_id | 发布者ID         | bigint(20)  | -          | 是（关联users.id）        | 发布通知的管理员                           | NOT NULL, FOREIGN KEY (publisher_id) REFERENCES users(id)      |
| is_top   | 是否置顶             | tinyint(1)  | -          | -                        | 0-否，1-是（置顶优先展示）                 | DEFAULT 0, NOT NULL                                           |
| publish_time | 发布时间         | datetime    | -          | -                        | 发布时间（草稿为NULL）                     | -                                                            |
| create_time | 创建时间         | datetime    | -          | -                        | 草稿创建时间                               | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |
| Latest_updateTime | 上次更新时间 | datetime    | -          | -                        | 最近一次编辑并保存的时间                   | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |

###### 4.5 token_consume（token消耗表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键 | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|------|--------------------------------------------|---------------------------------------------------------------|
| id       | 记录ID               | bigint(20)  | 是（自增） | -    | 唯一标识消耗记录                           | PRIMARY KEY (id)                                               |
| llm_name | 模型名称             | varchar(50) | -          | -    | 消耗token的大模型                           | NOT NULL                                                       |
| total_tokens | 总消耗         | int(11)     | -          | -    | 当日总消耗（输入+输出）                     | NOT NULL                                                       |
| prompt_tokens | 输入消耗         | int(11)     | -          | -    | 当日输入token消耗                           | NOT NULL                                                       |
| completion_tokens | 输出消耗     | int(11)     | -          | -    | 当日输出token消耗                           | NOT NULL                                                       |
| consume_date | 消耗日期         | date        | -          | -    | 消耗日期（如"2025-11-05"）                 | NOT NULL                                                       |
| growth_rate | 增长率         | decimal(5,2)| -          | -    | 较前一日增长率（%，如+12.50）               | -                                                            |

###### 4.6 error_logs（错误分析表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|--------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 记录ID               | bigint(20)  | 是（自增） | -                        | 唯一标识错误记录                           | PRIMARY KEY (id)                                               |
| error_type_id | 错误类型ID         | tinyint(2)  | -          | 是（关联error_types.id）  | 错误类型（模型超时/数据库错误等）           | NOT NULL, FOREIGN KEY (error_type_id) REFERENCES error_types(id) |
| error_count | 错误次数         | int(11)     | -          | -                        | 统计周期内错误次数                           | NOT NULL                                                       |
| error_rate | 错误率             | decimal(5,2)| -          | -                        | 错误次数占总请求比例（%）                   | -                                                            |
| period   | 统计周期             | varchar(20) | -          | -                        | "today"-今日、"7days"-近7日                 | NOT NULL                                                       |
| stat_time | 统计时间             | datetime    | -          | -                        | 统计生成时间                               | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |

###### 4.7 performance_metrics（性能趋势表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键 | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|------|--------------------------------------------|---------------------------------------------------------------|
| id       | 指标ID               | bigint(20)  | 是（自增） | -    | 唯一标识性能指标                           | PRIMARY KEY (id)                                               |
| metric_type | 指标类型         | varchar(20) | -          | -    | "query_count"-查询量、"response_time"-响应时间 | NOT NULL                                                       |
| metric_value | 指标值         | decimal(10,2)| -          | -    | 指标数值（查询量为整数，响应时间单位ms）     | NOT NULL                                                       |
| metric_time | 指标时间         | datetime    | -          | -    | 指标采集时间（精确到小时）                   | NOT NULL                                                       |
| trend    | 趋势标识             | tinyint(1)  | -          | -    | 0-下降，1-上升（较上一周期）                 | -                                                            |

###### 4.8 db_connection_logs（数据库连接日志表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|--------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 日志ID               | bigint(20)  | 是（自增） | -                        | 唯一标识连接日志                           | PRIMARY KEY (id)                                               |
| db_connection_id | 数据库连接ID     | bigint(20)  | -          | 是（关联db_connections.id） | 连接的数据源                               | NOT NULL, FOREIGN KEY (db_connection_id) REFERENCES db_connections(id) |
| db_name  | 数据库名称           | varchar(100)| -          | -                        | 冗余存储，便于查看                           | NOT NULL                                                       |
| connect_time | 连接时间         | datetime    | -          | -                        | 尝试连接时间                               | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |
| status   | 连接状态             | varchar(20) | -          | -                        | "success"-成功、"timeout"-超时、"auth_failed"-认证失败 | NOT NULL |
| remark   | 备注信息             | text        | -          | -                        | 错误代码                                   | -                                                            |
| handler_id | 处理人ID         | bigint(20)  | -          | 是（关联users.id）        | 触发连接的用户（NULL为系统检测）             | FOREIGN KEY (handler_id) REFERENCES users(id)                  |

###### 4.9 query_logs（查询日志表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|--------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 日志ID               | bigint(20)  | 是（自增） | -                        | 唯一标识查询日志                           | PRIMARY KEY (id)                                               |
| dialog_id | 对话ID               | varchar(50) | -          | -                        | 关联多轮对话ID                             | NOT NULL                                                       |
| data_source_id | 数据源ID         | bigint(20)  | -          | 是（关联db_connections.id） | 查询的数据源                               | NOT NULL, FOREIGN KEY (data_source_id) REFERENCES db_connections(id) |
| user_id  | 用户ID               | bigint(20)  | -          | 是（关联users.id）        | 执行查询的用户                             | NOT NULL, FOREIGN KEY (user_id) REFERENCES users(id)          |
| query_date | 查询日期         | date        | -          | -                        | 查询执行日期                               | NOT NULL                                                       |
| query_time | 查询时间         | datetime    | -          | -                        | 查询执行时间                               | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |
| execute_result | 执行结果         | tinyint(1)  | -          | -                        | 0-失败，1-成功                             | NOT NULL                                                       |

###### 4.10 user_searches（用户搜索表）
| 字段代码 | 字段名               | 数据类型    | 主键       | 外键                     | 字段描述                                   | 约束条件                                                      |
|----------|----------------------|-------------|------------|--------------------------|--------------------------------------------|---------------------------------------------------------------|
| id       | 搜索ID               | bigint(20)  | 是（自增） | -                        | 唯一标识搜索记录                           | PRIMARY KEY (id)                                               |
| user_id  | 用户ID               | bigint(20)  | -          | 是（关联users.id）        | 执行搜索的用户                             | NOT NULL, FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE |
| sql_content | SQL语句         | text        | -          | -                        | 用户执行的SQL语句                           | NOT NULL                                                       |
| query_title | 查询标题         | varchar(200)| -          | -                        | 大模型生成的标题                           | NOT NULL                                                       |
| search_count | 搜索次数         | int(11)     | -          | -                        | 该搜索被执行的次数                         | DEFAULT 1, NOT NULL                                           |
| last_search_time | 最后搜索时间   | datetime    | -          | -                        | 最后一次执行该搜索的时间                   | DEFAULT CURRENT_TIMESTAMP, NOT NULL                            |

#### （二）MongoDB 8.2 集合字段
###### 1. query_collections（收藏查询表-组）
| 字段代码 | 字段名       | 数据类型 | 主键               | 外键关联   | 字段描述                                   | 约束条件                  |
|----------|--------------|----------|--------------------|------------|--------------------------------------------|---------------------------|
| _id      | 组ID         | ObjectId | 是（自动生成）     | -          | 唯一标识收藏组                             | PRIMARY KEY (_id)          |
| userId   | 用户ID       | Long     | -                  | users.id   | 所属用户                                   | NOT NULL                  |
| groupName | 组名称      | String   | -                  | -          | 收藏组名称（由大模型生成，如"销售报表查询"） | NOT NULL, 同用户内唯一     |
| createTime | 创建时间   | Date     | -                  | -          | 组创建时间                                 | DEFAULT new Date()         |

###### 2. collection_records（收藏记录表-具体记录）
| 字段代码 | 字段名       | 数据类型 | 主键               | 外键关联               | 字段描述                                   | 约束条件                  |
|----------|--------------|----------|--------------------|------------------------|--------------------------------------------|---------------------------|
| _id      | 记录ID       | ObjectId | 是（自动生成）     | -                      | 唯一标识收藏记录                           | PRIMARY KEY (_id)          |
| queryId  | 组ID         | ObjectId | -                  | query_collections._id   | 所属收藏组                                 | NOT NULL                  |
| userId   | 用户ID       | Long     | -                  | users.id               | 所属用户                                   | NOT NULL                  |
| sqlContent | SQL语句   | String   | -                  | -                      | 收藏的SQL语句                               | NOT NULL                  |
| queryResult | 查询结果 | Document | -                  | -                      | 结果数据（如{ "columns": [], "data": [] }） | -                        |
| dbConnectionId | 数据库来源 | Long     | -                  | db_connections.id       | 收藏记录来源的数据库连接                     | NOT NULL                  |
| llmConfigId | 大模型来源   | Long     | -                  | llm_configs.id          | 收藏记录来源的大模型配置                     | NOT NULL                  |
| createTime | 创建时间 | Date     | -                  | -                      | 收藏时间                                   | DEFAULT new Date()         |

###### 3. dialog_records（多轮对话列表）
| 字段代码 | 字段名       | 数据类型 | 主键               | 外键关联   | 字段描述                                   | 约束条件                  |
|----------|--------------|----------|--------------------|------------|--------------------------------------------|---------------------------|
| _id      | 文档ID       | ObjectId | 是（自动生成）     | -          | 唯一标识对话列表                           | PRIMARY KEY (_id)          |
| dialogId | 对话ID       | String   | -                  | -          | 自定义唯一标识（如"USER123_20251105"）     | NOT NULL, UNIQUE           |
| userId   | 用户ID       | Long     | -                  | users.id   | 所属用户                                   | NOT NULL                  |
| topic    | 对话主题     | String   | -                  | -          | 大模型生成的主题（如"2023年订单分析"）     | NOT NULL                  |
| totalRounds | 总轮数   | Int      | -                  | -          | 对话总轮次                                 | DEFAULT 0                 |
| startTime | 开始时间   | Date     | -                  | -          | 对话开始时间                               | DEFAULT new Date()         |
| lastTime | 最后对话时间 | Date     | -                  | -          | 最后一轮对话时间                           | DEFAULT new Date()         |

###### 4. dialog_details（多轮对话具体内容表）
| 字段代码 | 字段名       | 数据类型 | 主键               | 外键关联               | 字段描述                                   | 约束条件                  |
|----------|--------------|----------|--------------------|------------------------|--------------------------------------------|---------------------------|
| _id      | 文档ID       | ObjectId | 是（自动生成）     | -                      | 唯一标识对话内容                           | PRIMARY KEY (_id)          |
| dialogId | 对话ID       | String   | -                  | dialog_records.dialogId | 关联的对话列表                             | NOT NULL                  |
| rounds   | 对话轮次     | Array    | -                  | -                      | 每轮对话详情：{ "roundNum": Int, "userInput": String, "aiResponse": String, "generatedSql": String, "roundTime": Date } | NOT NULL |

###### 5. sql_cache（SQL缓存集合）
| 字段代码 | 字段名       | 数据类型 | 主键               | 外键关联               | 字段描述                                   | 约束条件                  |
|----------|--------------|----------|--------------------|------------------------|--------------------------------------------|---------------------------|
| _id      | 文档ID       | ObjectId | 是（自动生成）     | -                      | 唯一标识缓存记录                           | PRIMARY KEY (_id)          |
| nlHash   | 自然语言哈希 | String   | -                  | -                      | 自然语言查询的MD5哈希（快速匹配）           | NOT NULL                  |
| userId   | 用户ID       | Long     | -                  | users.id               | 关联用户（NULL为全局缓存）                   | -                        |
| connectionId | 数据源ID | Long     | -                  | db_connections.id       | 关联数据源                                 | NOT NULL                  |
| tableIds | 表ID列表     | Array    | -                  | table_metadata.id       | 涉及的表ID                                 | NOT NULL                  |
| dbType   | 数据库类型   | String   | -                  | db_types.type_code      | 如"mysql"、"mongodb"                       | NOT NULL                  |
| generatedSql | 生成的SQL | String   | -                  | -                      | 缓存的SQL语句                               | NOT NULL                  |
| hitCount | 命中次数     | Int      | -                  | -                      | 缓存被复用次数                             | DEFAULT 0                 |
| expireTime | 过期时间 | Date     | -                  | -                      | 缓存过期时间（默认7天）                     | NOT NULL                  |

###### 6. ai_interaction_logs（AI交互日志集合）
| 字段代码 | 字段名       | 数据类型 | 主键               | 外键关联               | 字段描述                                   | 约束条件                  |
|----------|--------------|----------|--------------------|------------------------|--------------------------------------------|---------------------------|
| _id      | 文档ID       | ObjectId | 是（自动生成）     | -                      | 唯一标识交互记录                           | PRIMARY KEY (_id)          |
| userId   | 用户ID       | Long     | -                  | users.id               | 发起请求的用户                             | NOT NULL                  |
| requestType | 请求类型 | String   | -                  | -                      | "nl2sql"-自然语言转SQL、"sql_optimize"-SQL优化 | NOT NULL                  |
| llmName  | 模型名称     | String   | -                  | llm_configs.name        | 调用的大模型                               | NOT NULL                  |
| requestParams | 请求参数 | Document | -                  | -                      | 请求参数：{ "naturalLanguage": String, "metadata": Object, "temperature": Double } | NOT NULL |
| responseResult | 响应结果 | Document | -                  | -                      | 响应结果：{ "sql": String, "confidence": Double, "suggestion": String } | - |
| tokenUsage | Token消耗 | Document | -                  | -                      | { "promptTokens": Int, "completionTokens": Int, "totalTokens": Int } | NOT NULL |
| responseTime | 响应时间 | Int      | -                  | -                      | 响应耗时（ms）                             | NOT NULL                  |
| status   | 交互状态     | String   | -                  | -                      | "success"-成功、"fail"-失败                 | NOT NULL                  |
| errorMsg | 错误信息     | String   | -                  | -                      | 失败原因（成功为NULL）                     | -                        |
| createTime | 创建时间 | Date     | -                  | -                      | 请求发起时间                               | DEFAULT new Date()         |

###### 7. friend_chats（好友聊天记录表）
| 字段代码 | 字段名       | 数据类型 | 主键               | 外键关联   | 字段描述                                   | 约束条件                  |
|----------|--------------|----------|--------------------|------------|--------------------------------------------|---------------------------|
| _id      | 记录ID       | ObjectId | 是（自动生成）     | -          | 唯一标识聊天记录                           | PRIMARY KEY (_id)          |
| user_id  | 发送人ID     | Long     | -                  | users.id   | 发送消息的用户ID（关联MySQL的users表）       | NOT NULL                  |
| friend_id | 接收人ID   | Long     | -                  | users.id   | 接收消息的好友ID（关联MySQL的users表）       | NOT NULL                  |
| content_type | 内容类型 | String   | -                  | -          | 消息类型：text（文本）、query_share（查询分享）、image（图片）等 | NOT NULL |
| content  | 消息内容     | Document | -                  | -          | 动态内容：文本：{text: "Hello"}；分享：{query_id: "xxx", title: "订单查询"}；图片：{url: "xxx.png", size: 1024} | NOT NULL |
| send_time | 发送时间   | Date     | -                  | -          | 消息发送时间（精确到毫秒）                   | NOT NULL                  |
| is_read  | 是否已读     | Boolean  | -                  | -          | true（已读）/false（未读），默认false        | DEFAULT false             |
| extra    | 额外信息     | Document | -                  | -          | 扩展字段（如quote_msg_id引用消息ID、is_recalled是否撤回等） | - |

---

### 二、表/集合之间的关系（一对多/多对一/一对一）
#### （一）MySQL 表关系
| 关联表1                  | 关联表2                  | 关系类型 | 说明                                   |
|---------------------------|---------------------------|----------|----------------------------------------|
| users（用户表）           | roles（角色表）           | 多对一   | 多个用户属于同一角色（如多个普通用户）   |
| db_connections（数据库连接表） | db_types（数据库类型表）  | 多对一   | 多个数据库连接属于同一类型（如多个MySQL连接） |
| table_metadata（表元数据表） | db_connections（数据库连接表） | 多对一 | 多个表属于同一数据库连接               |
| column_metadata（字段元数据表） | table_metadata（表元数据表） | 多对一 | 多个字段属于同一表                     |
| user_db_permissions（用户数据权限表） | users（被授权用户） | 一对一 | 一个用户对应一条权限记录（每个用户一行） |
| user_db_permissions（用户数据权限表） | users（授权管理员） | 多对一 | 多个权限记录可由同一管理员修改         |
| friend_relations（好友关系表） | users（用户）           | 多对一   | 一个用户可添加多个好友                 |
| friend_relations（好友关系表） | users（好友）           | 多对一   | 一个用户可被多个用户添加为好友           |
| friend_requests（好友请求表） | users（申请人）         | 多对一   | 一个用户可发起多个好友请求             |
| friend_requests（好友请求表） | users（接收人）         | 多对一   | 一个用户可接收多个好友请求             |
| query_shares（查询分享记录表） | users（分享人）         | 多对一   | 一个用户可分享多个查询                 |
| query_shares（查询分享记录表） | users（接收人）         | 多对一   | 一个用户可接收多个分享                 |
| llm_configs（大模型配置表） | llm_status（大模型状态表） | 多对一 | 多个大模型配置属于同一状态             |
| llm_configs（大模型配置表） | users（创建人）         | 多对一   | 一个管理员可创建多个大模型配置         |
| notifications（通知表）   | notification_targets（通知目标表） | 多对一 | 多个通知属于同一接收目标               |
| notifications（通知表）   | priorities（通知优先级表） | 多对一 | 多个通知属于同一优先级                 |
| notifications（通知表）   | users（发布者）         | 多对一   | 一个管理员可发布多个通知               |
| error_logs（错误分析表）   | error_types（错误类型表） | 多对一 | 多个错误记录属于同一错误类型           |
| db_connection_logs（数据库连接日志表） | db_connections（数据库连接表） | 多对一 | 一个数据库连接有多个连接日志           |
| query_logs（查询日志表）   | db_connections（数据库连接表） | 多对一 | 一个数据源有多个查询日志               |
| query_logs（查询日志表）   | users（用户）           | 多对一   | 一个用户有多个查询日志                 |
| user_searches（用户搜索表） | users（用户）           | 多对一   | 一个用户有多个搜索记录                 |

#### （二）MongoDB 集合关系
| 关联集合1                  | 关联集合2                  | 关系类型 | 说明                                   |
|-----------------------------|-----------------------------|----------|----------------------------------------|
| collection_records（收藏记录表） | query_collections（收藏查询表） | 多对一 | 一个收藏组包含多个收藏记录             |
| collection_records（收藏记录表） | db_connections（数据库连接表） | 多对一 | 一个数据库连接有多个收藏记录           |
| collection_records（收藏记录表） | llm_configs（大模型配置表） | 多对一 | 一个大模型配置有多个收藏记录           |
| dialog_details（多轮对话具体内容表） | dialog_records（多轮对话列表） | 一对一 | 一个对话列表对应一个具体内容集合       |
| sql_cache（SQL缓存集合）     | db_connections（数据库连接表） | 多对一 | 一个数据源有多个SQL缓存                 |
| sql_cache（SQL缓存集合）     | users（用户表）             | 多对一   | 一个用户有多个个人SQL缓存               |
| ai_interaction_logs（AI交互日志集合） | users（用户表） | 多对一 | 一个用户有多个AI交互记录               |
| ai_interaction_logs（AI交互日志集合） | llm_configs（大模型配置表） | 多对一 | 一个大模型有多个交互记录               |
| friend_chats（好友聊天记录表） | users（用户表）             | 多对一   | 一个用户可发送多条聊天消息             |
| friend_chats（好友聊天记录表） | users（用户表）             | 多对一   | 一个用户可接收多条聊天消息             |

---

### 三、完整索引设计（按表/集合分类）
#### （一）MySQL 数据表索引
| 表名                  | 索引类型       | 索引字段                                  | 索引说明                                   |
|-----------------------|----------------|-------------------------------------------|--------------------------------------------|
| users                 | 普通索引       | role_id                                   | 按角色查询用户                             |
| users                 | 普通索引       | status                                    | 按账号状态筛选用户                         |
| users                 | 唯一索引       | username                                  | 用户名唯一，加速登录查询                   |
| users                 | 唯一索引       | email                                     | 邮箱唯一，加速好友添加查询                 |
| users                 | 唯一索引       | phonenumber                               | 手机号唯一，加速账号验证                   |
| roles                 | 主键索引       | id                                        | 主键默认索引，加速角色关联查询             |
| db_types              | 主键索引       | id                                        | 主键默认索引，加速数据库类型关联查询       |
| notification_targets  | 主键索引       | id                                        | 主键默认索引，加速通知目标关联查询         |
| priorities            | 普通索引       | sort                                      | 按排序权重查询优先级                       |
| priorities            | 主键索引       | id                                        | 主键默认索引，加速通知优先级关联查询       |
| error_types           | 主键索引       | id                                        | 主键默认索引，加速错误类型关联查询         |
| llm_status            | 主键索引       | id                                        | 主键默认索引，加速大模型状态关联查询       |
| db_connections        | 普通索引       | db_type_id                                 | 按数据库类型查询连接                       |
| db_connections        | 普通索引       | status                                    | 按连接状态筛选连接                         |
| db_connections        | 普通索引       | create_user_id                             | 按创建人查询连接                           |
| db_connections        | 唯一索引       | name                                      | 连接名称唯一，加速连接查询                 |
| table_metadata        | 唯一复合索引   | db_connection_id, table_name               | 同一连接下表名唯一，加速表元数据查询       |
| table_metadata        | 普通索引       | db_connection_id                           | 按数据库连接查询表                         |
| column_metadata       | 唯一复合索引   | table_id, column_name                      | 同一表下字段名唯一，加速字段元数据查询     |
| column_metadata       | 普通索引       | table_id                                   | 按表查询字段                               |
| user_db_permissions   | 唯一索引       | user_id                                    | 每个用户一行权限，加速权限查询             |
| user_db_permissions   | 普通索引       | last_grant_user_id                         | 按授权管理员查询权限记录                   |
| user_db_permissions   | 普通索引       | is_assigned                                | 按是否分配筛选权限                         |
| friend_relations      | 唯一复合索引   | user_id, friend_id                         | 避免重复添加好友                           |
| friend_relations      | 普通索引       | user_id                                    | 按用户查询好友列表                         |
| friend_relations      | 普通索引       | friend_id                                  | 按好友ID查询关联关系                       |
| friend_requests       | 唯一复合索引   | applicant_id, recipient_id                 | 避免重复发送好友请求                       |
| friend_requests       | 普通复合索引   | recipient_id, status                       | 接收人查询待处理请求                       |
| query_shares          | 普通复合索引   | receive_user_id, receive_status             | 接收人查询未处理分享                       |
| query_shares          | 普通索引       | share_user_id                              | 按分享人查询分享记录                       |
| system_health         | 普通索引       | collect_time                               | 按时间查询系统健康趋势                     |
| operation_logs        | 普通索引       | operate_time                               | 按时间查询操作日志                         |
| operation_logs        | 普通索引       | user_id                                    | 按用户查询操作日志                         |
| operation_logs        | 普通索引       | module                                     | 按模块筛选操作日志                         |
| llm_configs           | 普通索引       | status_id                                  | 按状态查询大模型配置                       |
| llm_configs           | 普通索引       | is_disabled                                | 按是否禁用筛选配置                         |
| llm_configs           | 唯一索引       | name                                      | 模型名称唯一，加速模型查询                 |
| notifications         | 普通索引       | target_id                                  | 按目标筛选通知                             |
| notifications         | 普通索引       | priority_id                                | 按优先级筛选通知                           |
| notifications         | 普通复合索引   | is_top DESC, publish_time DESC             | 置顶通知优先，按时间排序                   |
| token_consume         | 唯一复合索引   | llm_name, consume_date                     | 同一模型每日一条记录，加速消耗查询         |
| token_consume         | 普通索引       | consume_date                               | 按日期查询消耗趋势                         |
| error_logs            | 普通复合索引   | error_type_id, period                      | 按错误类型+周期查询错误记录                 |
| error_logs            | 普通索引       | stat_time                                  | 按统计时间查询错误记录                     |
| performance_metrics   | 普通复合索引   | metric_type, metric_time                   | 按类型+时间查询性能趋势                     |
| db_connection_logs    | 普通索引       | db_connection_id                           | 按连接查询日志                             |
| db_connection_logs    | 普通索引       | connect_time                               | 按时间查询连接日志                         |
| db_connection_logs    | 普通索引       | status                                    | 按状态筛选连接日志                         |
| query_logs            | 普通复合索引   | data_source_id, query_date                 | 统计数据源每日查询量                       |
| query_logs            | 普通索引       | user_id                                    | 按用户查询查询日志                         |
| query_logs            | 普通索引       | dialog_id                                  | 按对话ID查询查询日志                       |
| user_searches         | 普通复合索引   | user_id, last_search_time                  | 清理30天前的搜索记录                       |
| user_searches         | 普通复合索引   | user_id, search_count DESC                 | 查询用户常用搜索（按次数排序）             |

#### （二）MongoDB 集合索引
| 集合名                  | 索引类型       | 索引字段                                  | 索引说明                                   |
|-------------------------|----------------|-------------------------------------------|--------------------------------------------|
| query_collections       | 复合索引       | { "userId": 1, "groupName": 1 }           | 同用户内组名唯一，加速组查询               |
| collection_records      | 复合索引       | { "queryId": 1, "userId": 1 }              | 关联收藏组和用户，加速记录查询             |
| collection_records      | 普通索引       | { "dbConnectionId": 1 }                    | 按数据库来源查询收藏记录                   |
| collection_records      | 普通索引       | { "llmConfigId": 1 }                       | 按大模型来源查询收藏记录                   |
| dialog_records          | 复合索引       | { "userId": 1, "lastTime": -1 }            | 用户按时间查询对话列表（降序）             |
| dialog_records          | 唯一索引       | { "dialogId": 1 }                          | 对话ID唯一，加速对话定位                   |
| dialog_details          | 单字段索引     | { "dialogId": 1 }                          | 关联对话列表，加速内容查询                 |
| sql_cache               | 复合索引       | { "nlHash": 1, "connectionId": 1, "tableIds": 1 } | 精确匹配缓存，加速SQL复用                 |
| sql_cache               | TTL索引        | { "expireTime": 1 }                        | 自动删除过期缓存（默认7天）                 |
| ai_interaction_logs      | 复合索引       | { "userId": 1, "createTime": -1 }          | 用户按时间查询交互记录（降序）             |
| ai_interaction_logs      | 复合索引       | { "llmName": 1, "status": 1 }              | 按模型+状态查询交互记录                     |
| friend_chats            | 复合索引       | { "user_id": 1, "friend_id": 1, "send_time": -1 } | 按用户+好友+时间查询聊天记录（最新在前）   |
| friend_chats            | 复合索引       | { "friend_id": 1, "is_read": 1 }           | 查询好友未读消息                           |