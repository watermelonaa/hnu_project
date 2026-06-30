@echo off
REM Windows 版本的数据导入脚本
echo 开始导入数据...

REM 检查备份文件
if not exist "data-backup\mysql-data.sql" (
    echo 错误：找不到 MySQL 备份文件 data-backup\mysql-data.sql
    pause
    exit /b 1
)

if not exist "data-backup\mongodb-data" (
    echo 错误：找不到 MongoDB 备份目录 data-backup\mongodb-data
    pause
    exit /b 1
)

REM 导入 MySQL 数据
echo 导入 MySQL 数据...
docker exec -i nlq_mysql mysql -uroot -proot123456 natural_language_query_system < data-backup\mysql-data.sql

REM 导入 MongoDB 数据
echo 导入 MongoDB 数据...
docker cp data-backup\mongodb-data nlq_mongodb:/tmp/mongodb-restore
docker exec nlq_mongodb mongorestore --username=admin --password=admin123456 --authenticationDatabase=admin --db=natural_language_query_system /tmp/mongodb-restore

echo.
echo 数据导入完成！
pause



