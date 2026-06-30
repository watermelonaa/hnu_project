@echo off
REM Windows 版本的数据导出脚本
echo 开始导出数据...

REM 创建导出目录
if not exist "data-backup" mkdir data-backup

REM 导出 MySQL 数据
echo 导出 MySQL 数据...
docker exec nlq_mysql mysqldump -uroot -proot123456 --no-create-info --skip-triggers --complete-insert natural_language_query_system > data-backup\mysql-data.sql

REM 导出 MongoDB 数据
echo 导出 MongoDB 数据...
docker exec nlq_mongodb mongodump --username=admin --password=admin123456 --authenticationDatabase=admin --db=natural_language_query_system --out=/tmp/mongodb-backup
docker cp nlq_mongodb:/tmp/mongodb-backup/natural_language_query_system data-backup\mongodb-data

echo.
echo 数据导出完成！
echo 文件位置：
echo   - MySQL: data-backup\mysql-data.sql
echo   - MongoDB: data-backup\mongodb-data\
echo.
echo 请将 data-backup 文件夹分享给团队成员
pause



