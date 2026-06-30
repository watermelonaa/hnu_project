#!/bin/bash
echo "测试SMTP服务器..."

# 启动服务器（后台运行）
sudo php scripts/start_smtp.php &
SERVER_PID=$!
sleep 3  # 等待更长时间确保服务器启动

echo "发送测试邮件..."
# 使用timeout防止telnet无限等待
timeout 5 telnet localhost 25 << 'EOF'
HELO test
MAIL FROM: <user1@test.com>
RCPT TO: <admin@test.com>
DATA
Subject: 自动测试邮件
From: user1@test.com
To: admin@test.com

这是自动发送的测试邮件
.
QUIT
EOF

# 等待服务器处理完成
sleep 2


kill $SERVER_PID 2>/dev/null
echo "测试完成"