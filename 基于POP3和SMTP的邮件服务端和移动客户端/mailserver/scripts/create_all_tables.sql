-- ============================================
-- 邮件服务器完整数据库初始化脚本
-- ============================================
-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS mail_server 
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mail_server;

-- 创建用户并授权（关键步骤！）
CREATE USER IF NOT EXISTS 'mail_user'@'%' IDENTIFIED BY 'user123';
GRANT ALL PRIVILEGES ON mail_server.* TO 'mail_user'@'%';
FLUSH PRIVILEGES;

-- ============================================
-- 核心功能表
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_admin TINYINT(1) DEFAULT 0,
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 邮件表（兼容两种存储方式：SimpleSmtpServer用sender/recipient，SmtpHandler用sender_id/recipient_id）
CREATE TABLE IF NOT EXISTS emails (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender VARCHAR(100),
    recipient VARCHAR(100),
    sender_id INT,
    recipient_id INT,
    subject VARCHAR(200),
    body TEXT,
    headers TEXT,
    size_bytes INT DEFAULT 0,
    is_deleted TINYINT(1) DEFAULT 0,
    is_read TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE SET NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE=InnoDB;

-- 系统日志表
CREATE TABLE IF NOT EXISTS server_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    log_type VARCHAR(50),
    message TEXT,
    client_ip VARCHAR(45),
    user_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE=InnoDB;

-- ============================================
-- 管理功能表
-- ============================================

-- 系统设置表
CREATE TABLE IF NOT EXISTS system_settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE=InnoDB;

-- 过滤规则表
CREATE TABLE IF NOT EXISTS filter_rules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rule_type ENUM('email', 'ip') NOT NULL,
    rule_value VARCHAR(255) NOT NULL,
    action ENUM('block', 'allow') DEFAULT 'block',
    description TEXT,
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_rule (rule_type, rule_value)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE=InnoDB;

-- 服务状态表
CREATE TABLE IF NOT EXISTS service_status (
    id INT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(50) UNIQUE NOT NULL,
    is_running TINYINT(1) DEFAULT 0,
    pid INT,
    last_started_at TIMESTAMP NULL,
    last_stopped_at TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE=InnoDB;

-- 用户邮箱大小限制表
CREATE TABLE IF NOT EXISTS user_mailbox_limits (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    size_limit_bytes BIGINT DEFAULT 104857600, -- 100MB
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user (user_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ENGINE=InnoDB;

-- ============================================
-- 插入默认数据
-- ============================================

-- 插入测试用户（密码都是：123456）
INSERT INTO users (username, password_hash, is_admin, is_active) VALUES
('admin@test.com', '$2y$10$jB21V61k9aLAyp5.5qBpV.L70Aq6.XrtJrvlNI28bOXeJboLBJwoq', 1, 1),
('user1@test.com', '$2y$10$jB21V61k9aLAyp5.5qBpV.L70Aq6.XrtJrvlNI28bOXeJboLBJwoq', 0, 1)
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- 插入测试邮件
INSERT INTO emails (sender, recipient, subject, body) VALUES
('admin@test.com', 'user1@test.com', '欢迎使用邮件系统', '这是第一封测试邮件'),
('user1@test.com', 'admin@test.com', '回复测试', '我收到了，谢谢！')
ON DUPLICATE KEY UPDATE id = id;

-- 插入系统默认设置
INSERT INTO system_settings (setting_key, setting_value) VALUES
('smtp_port', '25'),
('pop3_port', '110'),
('domain', 'test.com'),
('mailbox_size_limit', '104857600'), -- 100MB
('smtp_enabled', '1'),
('pop3_enabled', '1'),
('log_path', '/var/log/mailserver'),
('log_max_size', '10485760') -- 10MB
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

-- 插入服务状态初始记录
INSERT INTO service_status (service_name, is_running) VALUES
('smtp', 0),
('pop3', 0)
ON DUPLICATE KEY UPDATE service_name = VALUES(service_name);

