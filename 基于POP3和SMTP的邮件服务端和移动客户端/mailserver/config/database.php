<?php
/**
 * 数据库配置文件
 * 使用环境变量或默认配置
 */

return [
    'host' => getenv('DB_HOST') ?: '127.0.0.1', // 使用127.0.0.1连接映射端口
    'port' => getenv('DB_PORT') ?: '3308', // Docker映射端口
    'database' => getenv('DB_DATABASE') ?: 'mail_server',
    'username' => getenv('DB_USERNAME') ?: 'mail_user',
    'password' => getenv('DB_PASSWORD') ?: 'user123',
    'charset' => 'utf8mb4',
    'collation' => 'utf8mb4_unicode_ci',
    'options' => [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
        PDO::MYSQL_ATTR_INIT_COMMAND => "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci"
    ]
];
