# 邮件服务器项目

基于POP3和SMTP协议的邮件服务端实现

## 环境要求

- Docker & Docker Compose
- PHP 7.4+ (需要扩展: php-mysql, php-sockets)
- WSL2 (Windows环境)
- netstat

## 快速开始

### 1. 安装PHP扩展（如果未安装）

```bash
sudo apt update
sudo apt install php php-cli php-mysql php-sockets -y
```

### 2. 启动数据库

```bash
cd /mnt/d/mailserver/mailserver

# 首次启动或重置数据库
docker-compose down -v
docker-compose up -d

# 等待10-15秒让数据库初始化完成
sleep 15
```

### 3. 初始化管理功能数据库表（首次使用）

```bash
# 执行管理功能相关的数据库表创建
docker-compose exec mysql mysql -umail_user -puser123 mail_server < scripts/create_admin_tables.sql
```

### 4. 查看数据库（phpMyAdmin）

- 访问：http://localhost:8088
- 登录信息：
  - 服务器：`mysql`（或留空）
  - 用户名：`root`
  - 密码：`root123`

### 5. 测试账号

- 管理员：`admin@test.com` / `123456`
- 普通用户：`user1@test.com` / `123456`

## 端口说明

- **25** - SMTP服务器（发送邮件）
- **110** - POP3服务器（接收邮件）
- **3308** - MySQL数据库
- **8080** - Web管理后台
- **8088** - phpMyAdmin管理界面



## Web管理后台

### 启动Web服务器

**方式1（推荐）：从public目录启动**
```bash
cd /mnt/d/mailserver/mailserver/public
php -S localhost:8080
#如果打不开可以尝试换端口为8888或者别的
```

**方式2：从项目根目录启动**
```bash
cd /mnt/d/mailserver/mailserver
php -S localhost:8080 -t public
```

### 访问管理后台

- 访问：http://localhost:8080
- 登录账号：
  - 管理员：`admin@test.com` / `123456`
  - 普通用户：`user1@test.com` / `123456`



## 测试服务器方法

### 测试SMTP（发送邮件）

**终端1：启动SMTP服务器**
```bash
sudo php scripts/start_smtp.php
```

**终端2：连接测试**
```bash
telnet localhost 25
```

**输入命令：**
```
HELO test
MAIL FROM: <user1@test.com>
RCPT TO: <admin@test.com>
DATA
Subject: 测试邮件
From: user1@test.com
To: admin@test.com

这是一封测试邮件！
.
QUIT
```

### 测试POP3（接收邮件）

**终端1：启动POP3服务器**
```bash
sudo php scripts/start_pop3.php
```

**终端2：连接测试**
```bash
telnet localhost 110
```

**输入命令：**
```
USER admin@test.com
PASS 123456
STAT
LIST
RETR 1
QUIT
```

## 查看数据

### 方法1：phpMyAdmin（推荐）
访问 http://localhost:8088，选择 `mail_server` 数据库

### 方法2：命令行
```bash
# 查看用户
docker-compose exec mysql mysql -umail_user -puser123 mailserver -e "SELECT * FROM users;"

# 查看邮件
docker-compose exec mysql mysql -umail_user -puser123 mailserver -e "SELECT id, sender, recipient, subject, created_at FROM emails ORDER BY id DESC;"
```

## 重置数据库

```bash
docker-compose down -v
docker-compose up -d
sleep 15
# 重新执行初始化脚本
docker-compose exec mysql mysql -umail_user -puser123 mail_server < scripts/create_admin_tables.sql
```

## 常见问题
### web页面启停服务器显示端口未成功监听
- 确保web用户有进入目录执行开始脚本的能力，可用下面命令测试，观察输出。

```bash
#模拟www-data用户执行start_smtp.php脚本
sudo -u www-data php /home/clumxc/projects/mailserver/scripts/start_smtp.php
```
- 需要sudo权限的低端口（25、110）无法通过Web页面开启，需要先改为25252、1100等其它端口；  
或给 /usr/bin/php 这个可执行文件贴一张“特许证”，以后不管谁运行 php，都能绑低端口，不需要 root。
一步一步做：
```bash
#给 PHP 贴特许证,打开终端，执行：
sudo setcap 'cap_net_bind_service=+ep' /usr/bin/php
```
```bash
#确认贴上了
getcap /usr/bin/php
#看到输出
/usr/bin/php = cap_net_bind_service+ep
```
```bash
#重启你的 Web 服务（让新能力生效）
#如果你用 Apache：
sudo systemctl restart apache2
#如果你用 Nginx + PHP-FPM：
sudo systemctl restart php-fpm
```
### 端口被占用

**SMTP服务器启动失败（25端口）**
```bash
# 检查端口占用
sudo netstat -tlnp | grep 25

# 或使用
sudo lsof -i :25
```

**POP3服务器启动失败（110端口）**
```bash
sudo netstat -tlnp | grep 110
```

### 数据库连接失败

```bash
# 检查Docker容器状态
docker-compose ps

# 查看数据库日志
docker-compose logs mysql

# 重启数据库
docker-compose restart mysql
```

### telnet连接失败

```bash
# 安装telnet（如果未安装）
sudo apt install telnet

# 或使用nc替代
nc localhost 25
```

### 密码验证失败

- 确认使用正确的测试账号：`admin@test.com` / `123456`
- 如果重置了数据库，密码会恢复为 `123456`

## 项目结构

```
mailserver/
├── scripts/                    # 启动脚本和SQL
│   ├── start_smtp.php          # 启动SMTP服务器
│   ├── start_pop3.php          # 启动POP3服务器
│   ├── create_tables.sql       # 数据库初始化脚本
│   └── create_admin_tables.sql # 管理功能数据库表
├── src/                        # 源代码
│   ├── protocol/               # SMTP/POP3协议实现
│   │   ├── SmtpServer.php
│   │   └── Pop3Server.php
│   ├── storage/                # 数据存储层
│   │   ├── Database.php
│   │   ├── UserRepository.php
│   │   ├── EmailRepository.php
│   │   ├── SystemSettingsRepository.php
│   │   ├── FilterRepository.php
│   │   ├── ServiceRepository.php
│   │   └── MailboxRepository.php
│   ├── admin/                  # 管理后台逻辑
│   │   └── BroadcastService.php
│   └── utils/                  # 工具类
│       ├── Security.php
│       └── Validator.php
├── public/                     # Web管理界面
│   ├── index.php               # 主页面（登录+仪表盘）
│   ├── register.php            # 用户注册
│   ├── logout.php              # 退出登录
│   ├── users.php               # 用户管理
│   ├── emails.php               # 邮件管理
│   ├── broadcast.php           # 群发邮件
│   ├── filters.php             # 过滤规则
│   ├── settings.php            # 系统设置
│   ├── services.php            # 服务管理
│   ├── logs.php                # 日志管理
│   └── help.php                # 帮助
├── config/                     # 配置文件
│   ├── database.php            # 数据库配置
│   └── constants.php           # 常量定义
└── docker-compose.yml          # Docker配置
```

## 功能完成情况

### ✅ 服务器端功能（已完成）

根据课程设计说明书要求，服务器端功能已全部实现：

1. **✅ 邮箱管理**
   - 设置用户邮箱大小限制
   - 查看用户邮箱使用情况

2. **✅ 客户管理**
   - 创建新客户账号和密码
   - 设置用户权限（管理员/普通用户）
   - 启用/禁用用户
   - 删除客户账号
   - 编辑用户信息

3. **✅ 服务起停**
   - SMTP服务状态管理
   - POP3服务状态管理
   - 服务启动/停止控制

4. **✅ 系统设置**
   - SMTP端口设置（默认25）
   - POP3端口设置（默认110）
   - 服务器域名设置（默认test.com）
   - 管理员密码修改
   - 邮件过滤（账号过滤）
   - IP地址过滤

5. **✅ 日志管理**
   - SMTP日志查看
   - POP3日志查看
   - 日志清除功能
   - 日志存储位置设置
   - 日志文件大小管理

6. **✅ 日常管理**
   - 群发邮件功能（发送给所有用户或指定用户）

7. **✅ 帮助**
   - 系统使用帮助文档
