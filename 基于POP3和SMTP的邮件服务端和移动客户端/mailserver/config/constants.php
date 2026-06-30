<?php
/**
 * 常量定义文件
 */

// 服务器配置
define('SMTP_PORT', 25);
define('POP3_PORT', 110);
define('SMTP_SSL_PORT', 465);
define('POP3_SSL_PORT', 995);
define('ADMIN_PORT', 8080);
define('SERVER_DOMAIN', 'mail.test');
define('MAX_CONNECTIONS', 50);
define('CONNECTION_TIMEOUT', 300); // 5分钟

// 路径常量
define('ROOT_PATH', dirname(__DIR__));
define('LOG_PATH', ROOT_PATH . '/logs');
define('SCRIPT_PATH', ROOT_PATH . '/scripts');

// 邮件大小限制（字节）
define('MAX_EMAIL_SIZE', 10 * 1024 * 1024); // 10MB
define('MAX_ATTACHMENT_SIZE', 5 * 1024 * 1024); // 5MB

// 响应码
define('SMTP_OK', '250');
define('SMTP_DATA_START', '354');
define('POP3_OK', '+OK');
define('POP3_ERR', '-ERR');

// 日志级别
define('LOG_DEBUG', 1);
define('LOG_INFO', 2);
define('LOG_WARN', 3);
define('LOG_ERROR', 4);