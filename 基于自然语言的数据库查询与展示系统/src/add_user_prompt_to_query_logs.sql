-- 为 query_logs 表添加 user_prompt 字段
-- 执行此脚本前请备份数据库

-- 添加 user_prompt 字段
ALTER TABLE `query_logs` 
ADD COLUMN `user_prompt` TEXT COMMENT '用户原始提问（自然语言）' AFTER `user_id`;

-- 验证字段是否添加成功
-- SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = DATABASE() 
--   AND TABLE_NAME = 'query_logs' 
--   AND COLUMN_NAME = 'user_prompt';

