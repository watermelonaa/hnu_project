-- MySQL数据库建表脚本
-- 基于last.md文档设计

-- 1. 基础信息表（用户、角色、字典）

-- 1.1 roles（角色表）- 需要先创建，因为users依赖它
CREATE TABLE `roles` (
  `id` TINYINT(2) PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  `role_name` VARCHAR(30) NOT NULL UNIQUE COMMENT '角色名称',
  `role_code` VARCHAR(20) NOT NULL UNIQUE COMMENT '角色编码',
  `description` VARCHAR(500) COMMENT '角色描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 1.2 users（用户表）
CREATE TABLE `users` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密存储）',
  `email` VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
  `phonenumber` VARCHAR(50) NOT NULL UNIQUE COMMENT '手机号',
  `role_id` TINYINT(2) NOT NULL COMMENT '角色ID',
  `avatar_url` MEDIUMTEXT COMMENT '头像URL或Base64编码',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账号状态：0-禁用，1-正常',
  `online_status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '在线状态：0-离线，1-在线',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_role_id` (`role_id`),
  INDEX `idx_status` (`status`),
  UNIQUE INDEX `uk_username` (`username`),
  UNIQUE INDEX `uk_email` (`email`),
  UNIQUE INDEX `uk_phonenumber` (`phonenumber`),
  FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 1.3 db_types（数据库类型表）
CREATE TABLE `db_types` (
  `id` TINYINT(2) PRIMARY KEY AUTO_INCREMENT COMMENT '类型ID',
  `type_name` VARCHAR(50) NOT NULL UNIQUE COMMENT '类型名称',
  `type_code` VARCHAR(20) NOT NULL UNIQUE COMMENT '类型编码',
  `description` VARCHAR(500) COMMENT '类型描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库类型表';

-- 1.4 notification_targets（通知目标表）
CREATE TABLE `notification_targets` (
  `id` TINYINT(2) PRIMARY KEY AUTO_INCREMENT COMMENT '目标ID',
  `target_name` VARCHAR(30) NOT NULL UNIQUE COMMENT '目标名称',
  `target_code` VARCHAR(20) NOT NULL UNIQUE COMMENT '目标编码',
  `description` VARCHAR(200) COMMENT '目标描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知目标表';

-- 1.5 priorities（通知优先级表）
CREATE TABLE `priorities` (
  `id` TINYINT(2) PRIMARY KEY AUTO_INCREMENT COMMENT '优先级ID',
  `priority_name` VARCHAR(20) NOT NULL UNIQUE COMMENT '优先级名称',
  `priority_code` VARCHAR(20) NOT NULL UNIQUE COMMENT '优先级编码',
  `sort` INT(11) NOT NULL COMMENT '排序权重',
  INDEX `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知优先级表';

-- 1.6 error_types（错误类型表）
CREATE TABLE `error_types` (
  `id` TINYINT(2) PRIMARY KEY AUTO_INCREMENT COMMENT '错误ID',
  `error_name` VARCHAR(50) NOT NULL UNIQUE COMMENT '错误名称',
  `error_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '错误编码',
  `description` VARCHAR(500) COMMENT '错误描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错误类型表';

-- 1.7 llm_status（大模型状态表）
CREATE TABLE `llm_status` (
  `id` TINYINT(2) PRIMARY KEY AUTO_INCREMENT COMMENT '状态ID',
  `status_name` VARCHAR(20) NOT NULL UNIQUE COMMENT '状态名称',
  `status_code` VARCHAR(20) NOT NULL UNIQUE COMMENT '状态编码',
  `description` VARCHAR(200) COMMENT '状态描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大模型状态表';

-- 2. 数据资源表

-- 2.1 db_connections（数据库连接表）
CREATE TABLE `db_connections` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '连接ID',
  `name` VARCHAR(100) NOT NULL UNIQUE COMMENT '连接名称',
  `db_type_id` TINYINT(2) NOT NULL COMMENT '数据库类型ID',
  `url` VARCHAR(255) NOT NULL COMMENT '连接地址',
  `username` VARCHAR(50) NOT NULL COMMENT '数据库账号（加密存储）',
  `password` VARCHAR(100) NOT NULL COMMENT '数据库密码（加密存储）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'disconnected' COMMENT '连接状态',
  `create_user_id` BIGINT(20) NOT NULL COMMENT '创建者ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  INDEX `idx_db_type_id` (`db_type_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_create_user_id` (`create_user_id`),
  UNIQUE INDEX `uk_name` (`name`),
  FOREIGN KEY (`db_type_id`) REFERENCES `db_types`(`id`),
  FOREIGN KEY (`create_user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库连接表';

-- 2.2 table_metadata（表元数据表）
CREATE TABLE `table_metadata` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '表ID',
  `db_connection_id` BIGINT(20) NOT NULL COMMENT '数据库连接ID',
  `table_name` VARCHAR(100) NOT NULL COMMENT '表名',
  `description` VARCHAR(500) COMMENT '表描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE INDEX `uk_db_connection_table` (`db_connection_id`, `table_name`),
  INDEX `idx_db_connection_id` (`db_connection_id`),
  FOREIGN KEY (`db_connection_id`) REFERENCES `db_connections`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表元数据表';

-- 2.3 column_metadata（字段元数据表）
CREATE TABLE `column_metadata` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '字段ID',
  `table_id` BIGINT(20) NOT NULL COMMENT '表ID',
  `column_name` VARCHAR(100) NOT NULL COMMENT '字段名',
  `data_type` VARCHAR(50) NOT NULL COMMENT '数据类型',
  `description` VARCHAR(500) COMMENT '字段描述',
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主键：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE INDEX `uk_table_column` (`table_id`, `column_name`),
  INDEX `idx_table_id` (`table_id`),
  FOREIGN KEY (`table_id`) REFERENCES `table_metadata`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段元数据表';

-- 3. 权限与关系表

-- 3.1 user_db_permissions（用户数据权限表）
CREATE TABLE `user_db_permissions` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `permission_details` JSON NOT NULL COMMENT '权限详情',
  `last_grant_user_id` BIGINT(20) NOT NULL COMMENT '最后授权管理员ID',
  `is_assigned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已分配：0-未分配，1-已分配',
  `last_grant_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后授权时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE INDEX `uk_user_id` (`user_id`),
  INDEX `idx_last_grant_user_id` (`last_grant_user_id`),
  INDEX `idx_is_assigned` (`is_assigned`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`last_grant_user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户数据权限表';

-- 3.2 friend_relations（好友关系表）
CREATE TABLE `friend_relations` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `friend_id` BIGINT(20) NOT NULL COMMENT '好友ID',
  `friend_username` VARCHAR(50) NOT NULL COMMENT '好友用户名',
  `online_status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '好友在线状态：0-离线，1-在线',
  `remark_name` VARCHAR(50) COMMENT '备注名',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  UNIQUE INDEX `uk_user_friend` (`user_id`, `friend_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_friend_id` (`friend_id`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`friend_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

-- 3.3 friend_requests（好友请求表）
CREATE TABLE `friend_requests` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '请求ID',
  `applicant_id` BIGINT(20) NOT NULL COMMENT '申请人ID',
  `recipient_id` BIGINT(20) NOT NULL COMMENT '接收人ID',
  `apply_msg` VARCHAR(200) COMMENT '申请留言',
  `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '请求状态：0-待处理，1-已同意，2-已拒绝',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `handle_time` DATETIME COMMENT '处理时间',
  UNIQUE INDEX `uk_applicant_recipient` (`applicant_id`, `recipient_id`),
  INDEX `idx_recipient_status` (`recipient_id`, `status`),
  FOREIGN KEY (`applicant_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`recipient_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友请求表';

-- 3.4 query_shares（查询分享记录表）
CREATE TABLE `query_shares` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '分享ID',
  `share_user_id` BIGINT(20) NOT NULL COMMENT '分享人ID',
  `receive_user_id` BIGINT(20) NOT NULL COMMENT '接收人ID',
  `dialog_id` VARCHAR(50) NOT NULL COMMENT '会话ID',
  `target_rounds` JSON NOT NULL COMMENT '会话轮次数组',
  `query_title` VARCHAR(200) NOT NULL COMMENT '查询标题',
  `share_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分享时间',
  `receive_status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '接收状态：0-未处理，1-已保存，2-已删除',
  INDEX `idx_receive_user_status` (`receive_user_id`, `receive_status`),
  INDEX `idx_share_user_id` (`share_user_id`),
  FOREIGN KEY (`share_user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`receive_user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查询分享记录表';

-- 4. 系统日志与监控表

-- 4.1 system_health（系统健康表）
CREATE TABLE `system_health` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  `db_delay` INT(11) NOT NULL COMMENT '数据库延迟（ms）',
  `cache_delay` INT(11) NOT NULL COMMENT '缓存延迟（ms）',
  `llm_delay` INT(11) NOT NULL COMMENT '大模型延迟（ms）',
  `storage_usage` DECIMAL(5,2) NOT NULL COMMENT '存储使用率（%）',
  `collect_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
  INDEX `idx_collect_time` (`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统健康表';

-- 4.2 operation_logs（系统操作日志表）
CREATE TABLE `operation_logs` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `operation` VARCHAR(100) NOT NULL COMMENT '操作名称',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块',
  `related_llm` VARCHAR(50) COMMENT '涉及模型',
  `ip_address` VARCHAR(50) NOT NULL COMMENT 'IP地址',
  `operate_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `result` TINYINT(1) NOT NULL COMMENT '操作结果：0-失败，1-成功',
  `error_msg` TEXT COMMENT '错误信息',
  INDEX `idx_operate_time` (`operate_time`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_module` (`module`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- 4.3 llm_configs（大模型配置表）
CREATE TABLE `llm_configs` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
  `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '模型名称',
  `version` VARCHAR(20) NOT NULL COMMENT '模型版本',
  `api_key` VARCHAR(200) NOT NULL COMMENT 'API密钥（AES加密）',
  `api_url` VARCHAR(255) NOT NULL COMMENT 'API地址',
  `status_id` TINYINT(2) NOT NULL COMMENT '状态ID',
  `is_disabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否禁用：0-启用，1-禁用',
  `timeout` INT(11) NOT NULL COMMENT '超时时间（ms）',
  `create_user_id` BIGINT(20) NOT NULL COMMENT '创建人ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_status_id` (`status_id`),
  INDEX `idx_is_disabled` (`is_disabled`),
  UNIQUE INDEX `uk_name` (`name`),
  FOREIGN KEY (`status_id`) REFERENCES `llm_status`(`id`),
  FOREIGN KEY (`create_user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大模型配置表';

-- 4.4 notifications（通知表）
CREATE TABLE `notifications` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
  `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
  `content` TEXT NOT NULL COMMENT '通知内容',
  `target_id` TINYINT(2) NOT NULL COMMENT '目标ID',
  `priority_id` TINYINT(2) NOT NULL COMMENT '优先级ID',
  `publisher_id` BIGINT(20) NOT NULL COMMENT '发布者ID',
  `is_top` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
  `publish_time` DATETIME COMMENT '发布时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `latest_update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上次更新时间',
  INDEX `idx_target_id` (`target_id`),
  INDEX `idx_priority_id` (`priority_id`),
  INDEX `idx_is_top_publish_time` (`is_top` DESC, `publish_time` DESC),
  FOREIGN KEY (`target_id`) REFERENCES `notification_targets`(`id`),
  FOREIGN KEY (`priority_id`) REFERENCES `priorities`(`id`),
  FOREIGN KEY (`publisher_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 4.5 token_consume（token消耗表）
CREATE TABLE `token_consume` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  `llm_name` VARCHAR(50) NOT NULL COMMENT '模型名称',
  `total_tokens` BIGINT(11) NOT NULL COMMENT '总消耗',
  `prompt_tokens` BIGINT(11) NOT NULL COMMENT '输入消耗',
  `completion_tokens` BIGINT(11) NOT NULL COMMENT '输出消耗',
  `consume_date` DATE NOT NULL COMMENT '消耗日期',
  `growth_rate` DECIMAL(5,2) COMMENT '增长率（%）',
  UNIQUE INDEX `uk_llm_date` (`llm_name`, `consume_date`),
  INDEX `idx_consume_date` (`consume_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='token消耗表';

-- 4.6 error_logs（错误分析表）
CREATE TABLE `error_logs` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  `error_type_id` TINYINT(2) NOT NULL COMMENT '错误类型ID',
  `error_count` INT(11) NOT NULL COMMENT '错误次数',
  `error_rate` DECIMAL(5,2) COMMENT '错误率（%）',
  `period` VARCHAR(20) NOT NULL COMMENT '统计周期',
  `stat_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计时间',
  INDEX `idx_error_type_period` (`error_type_id`, `period`),
  INDEX `idx_stat_time` (`stat_time`),
  FOREIGN KEY (`error_type_id`) REFERENCES `error_types`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错误分析表';

-- 4.7 performance_metrics（性能趋势表）
CREATE TABLE `performance_metrics` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '指标ID',
  `metric_type` VARCHAR(20) NOT NULL COMMENT '指标类型',
  `metric_value` DECIMAL(10,2) NOT NULL COMMENT '指标值',
  `metric_time` DATETIME NOT NULL COMMENT '指标时间',
  `trend` TINYINT(1) COMMENT '趋势标识：0-下降，1-上升',
  INDEX `idx_metric_type_time` (`metric_type`, `metric_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='性能趋势表';

-- 4.8 db_connection_logs（数据库连接日志表）
CREATE TABLE `db_connection_logs` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  `db_connection_id` BIGINT(20) NOT NULL COMMENT '数据库连接ID',
  `db_name` VARCHAR(100) NOT NULL COMMENT '数据库名称',
  `connect_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '连接时间',
  `status` VARCHAR(20) NOT NULL COMMENT '连接状态',
  `remark` TEXT COMMENT '备注信息',
  `handler_id` BIGINT(20) COMMENT '处理人ID',
  INDEX `idx_db_connection_id` (`db_connection_id`),
  INDEX `idx_connect_time` (`connect_time`),
  INDEX `idx_status` (`status`),
  FOREIGN KEY (`db_connection_id`) REFERENCES `db_connections`(`id`),
  FOREIGN KEY (`handler_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库连接日志表';

-- 4.9 query_logs（查询日志表）
CREATE TABLE `query_logs` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  `dialog_id` VARCHAR(50) NOT NULL COMMENT '对话ID',
  `data_source_id` BIGINT(20) NOT NULL COMMENT '数据源ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `user_prompt` TEXT COMMENT '用户原始提问（自然语言）',
  `query_date` DATE NOT NULL COMMENT '查询日期',
  `query_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '查询时间',
  `execute_result` TINYINT(1) NOT NULL COMMENT '执行结果：0-失败，1-成功',
  INDEX `idx_data_source_date` (`data_source_id`, `query_date`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_dialog_id` (`dialog_id`),
  FOREIGN KEY (`data_source_id`) REFERENCES `db_connections`(`id`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查询日志表';

-- 4.10 user_searches（用户搜索表）
CREATE TABLE `user_searches` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '搜索ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `sql_content` TEXT NOT NULL COMMENT 'SQL语句',
  `query_title` VARCHAR(200) NOT NULL COMMENT '查询标题',
  `search_count` INT(11) NOT NULL DEFAULT 1 COMMENT '搜索次数',
  `last_search_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后搜索时间',
  INDEX `idx_user_last_search` (`user_id`, `last_search_time`),
  INDEX `idx_user_search_count` (`user_id`, `search_count` DESC),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户搜索表';

-- 4.11 user_notification_reads（用户通知已读状态表）
CREATE TABLE `user_notification_reads` (
  `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `notification_id` BIGINT(20) NOT NULL COMMENT '通知ID',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
  `read_time` DATETIME COMMENT '阅读时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE INDEX `uk_user_notification` (`user_id`, `notification_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_notification_id` (`notification_id`),
  INDEX `idx_is_read` (`is_read`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`notification_id`) REFERENCES `notifications`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知已读状态表';

-- ========================================
-- 初始化基础数据（必须数据）
-- ========================================

-- 1. 角色数据
INSERT INTO `roles` (`id`, `role_name`, `role_code`, `description`) VALUES
(1, '系统管理员', 'sys_admin', '拥有系统所有权限，可管理用户、配置大模型'),
(2, '数据管理员', 'data_admin', '可管理数据源连接、分配用户数据权限'),
(3, '普通用户', 'normal_user', '可执行自然语言查询、查看历史记录');

-- 2. 数据库类型
INSERT INTO `db_types` (`id`, `type_name`, `type_code`, `description`) VALUES
(1, 'MySQL', 'mysql', 'MySQL关系型数据库'),
(2, 'MongoDB', 'mongodb', 'MongoDB文档型数据库'),
(3, 'SQL Server', 'mssql', 'Microsoft SQL Server'),
(4, 'PostgreSQL', 'postgresql', 'PostgreSQL关系型数据库');

-- 3. 大模型状态
INSERT INTO `llm_status` (`id`, `status_name`, `status_code`, `description`) VALUES
(1, '可用', 'available', 'API成功率≥95%，响应时间正常'),
(2, '不可用', 'unavailable', 'API无法连接或持续失败'),
(3, '不稳定', 'unstable', 'API成功率在60%-95%之间');

-- 4. 通知目标
INSERT INTO `notification_targets` (`id`, `target_name`, `target_code`, `description`) VALUES
(1, '所有用户', 'all', '系统内所有用户'),
(2, '系统管理员', 'sys_admin', '仅系统管理员可见'),
(3, '数据管理员', 'data_admin', '仅数据管理员可见'),
(4, '普通用户', 'normal_user', '仅普通用户可见');

-- 5. 通知优先级
INSERT INTO `priorities` (`id`, `priority_name`, `priority_code`, `sort`) VALUES
(1, '紧急', 'urgent', 1),
(2, '普通', 'normal', 2),
(3, '低', 'low', 3);

-- 6. 错误类型
INSERT INTO `error_types` (`id`, `error_name`, `error_code`, `description`) VALUES
(1, '模型调用超时', 'llm_timeout', '大模型API响应超时'),
(2, '数据库连接错误', 'db_connection_error', '数据库连接失败或超时'),
(3, 'SQL语法错误', 'sql_syntax_error', '生成的SQL语句语法错误'),
(4, '权限不足', 'permission_denied', '用户无权访问指定数据源');

-- 7. 内置用户（三个角色各一个）
-- 密码统一为 "123456" 的 BCrypt 加密结果：$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
INSERT INTO `users` (`id`, `username`, `password`, `email`, `phonenumber`, `role_id`, `status`) VALUES
(1, 'sys_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'sys_admin@example.com', '13800138001', 1, 1),
(2, 'data_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'data_admin@example.com', '13800138002', 2, 1),
(3, 'normal_user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'normal_user@example.com', '13800138003', 3, 1);

-- 8. 用户数据权限初始化（为所有用户创建权限记录）
INSERT INTO `user_db_permissions` (`user_id`, `permission_details`, `is_assigned`, `last_grant_user_id`) VALUES
(1, '[]', 1, 1),
(2, '[]', 1, 1),
(3, '[]', 0, 1);

