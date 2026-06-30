#!/usr/bin/env php
<?php

// 打印当前目录
error_log('start_smtp.php CWD: ' . getcwd());
error_log('start_smtp.php __DIR__: ' . __DIR__);


require_once __DIR__.'/../src/storage/SystemSettingsRepository.php';
require_once __DIR__ . '/../src/protocol/SmtpServer.php';

$settingsRepo = new SystemSettingsRepository();
// 获取端口设置（放在函数定义之前）
$smtpPort = $settingsRepo->get('smtp_port', 25);

echo "启动最简SMTP邮件服务器\n";
echo "==============================\n\n";

// 检查端口权限
/**if (posix_getuid() != 0) {
    echo "错误：需要管理员权限监听".$smtpPort."端口\n";
    echo "请使用：sudo php " . __FILE__ . "\n";
    echo "或者使用其他端口\n";
    exit(1);
}**/

$server = new SimpleSmtpServer('0.0.0.0',$smtpPort);
$server->start();
?>