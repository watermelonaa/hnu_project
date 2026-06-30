#!/bin/bash
# 导入团队共享的测试数据到 Docker 容器
# 使用前确保已经运行 docker compose up -d

echo "开始导入数据..."

# 检查备份文件是否存在
if [ ! -f "./data-backup/mysql-data.sql" ]; then
    echo "错误：找不到 MySQL 备份文件 ./data-backup/mysql-data.sql"
    exit 1
fi

if [ ! -d "./data-backup/mongodb-data" ]; then
    echo "错误：找不到 MongoDB 备份目录 ./data-backup/mongodb-data"
    exit 1
fi

# 导入 MySQL 数据
echo "导入 MySQL 数据..."
docker exec -i nlq_mysql mysql \
  -uroot -proot123456 \
  natural_language_query_system \
  < ./data-backup/mysql-data.sql

# 导入 MongoDB 数据
echo "导入 MongoDB 数据..."
docker cp ./data-backup/mongodb-data nlq_mongodb:/tmp/mongodb-restore
docker exec nlq_mongodb mongorestore \
  --username=admin \
  --password=admin123456 \
  --authenticationDatabase=admin \
  --db=natural_language_query_system \
  /tmp/mongodb-restore

echo "数据导入完成！"



