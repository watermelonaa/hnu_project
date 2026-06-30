cat > test_smtp.sh << 'EOF'
#!/bin/bash
echo "📧 手动SMTP测试脚本"
echo "=================="
echo "请在一个终端运行：sudo php scripts/start_smtp.php"
echo "然后在另一个终端运行：telnet localhost 25"
echo ""
echo "手动输入以下命令："
echo "------------------"
echo "HELO test"
echo "MAIL FROM: <user1@test.com>"
echo "RCPT TO: <admin@test.com>"
echo "DATA"
echo "Subject: 中文测试邮件"
echo "From: user1@test.com"
echo "To: admin@test.com"
echo ""
echo "这是一封测试中文的邮件！"
echo "看看中文是否能正常保存和显示。"
echo "谢谢！"
echo "."
echo "QUIT"
echo ""
echo "发送后检查数据库："
echo "docker-compose exec mysql mysql -umail_user -puser123 mail_simple -e \"SELECT subject, body FROM emails ORDER BY id DESC LIMIT 1;\""
EOF

chmod +x test_smtp.sh