#!/usr/bin/env php
<?php
// 打印当前目录
error_log('start_smtp.php CWD: ' . getcwd());
error_log('start_smtp.php __DIR__: ' . __DIR__);

require_once __DIR__.'/../src/storage/SystemSettingsRepository.php';
require_once __DIR__ . '/../src/protocol/Pop3Server.php';

$settingsRepo = new SystemSettingsRepository();
// 获取端口设置（放在函数定义之前）
$pop3Port = $settingsRepo->get('pop3_port', 110);

echo "启动最简POP3邮件服务器\n";
echo "==============================\n\n";

$server = new SimplePop3Server('0.0.0.0',$pop3Port);
$server->start();
?>