#!/bin/bash
echo "重置数据库..."
docker-compose down -v
echo "启动数据库（SQL会自动执行）..."
docker-compose up -d
echo "等待数据库初始化（10秒）..."
sleep 10
echo "验证数据..."
docker-compose exec mysql mysql -umail_user -puser123 mail_server -e "SELECT id, username, is_admin FROM users; SELECT COUNT(*) as email_count FROM emails;"
echo "完成！"

