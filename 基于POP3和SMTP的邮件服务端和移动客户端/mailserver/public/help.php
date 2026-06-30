<?php
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/storage/Database.php';

session_start();

// 身份验证
if (!isset($_SESSION['user_id'])) {
    header('Location: index.php');
    exit;
}
?>
<!DOCTYPE html>
<html>
<head>
    <title>帮助 - 邮件服务器</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
        .header { background: #007bff; color: white; padding: 15px; margin: -20px -20px 20px -20px; }
        .menu { background: white; padding: 10px; margin-bottom: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .menu a { margin-right: 15px; text-decoration: none; color: #007bff; }
        .container { background: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); max-width: 900px; }
        .section { margin-bottom: 30px; }
        .section h3 { color: #007bff; border-bottom: 2px solid #007bff; padding-bottom: 10px; }
        .section h4 { color: #333; margin-top: 20px; }
        .code-block { background: #f8f9fa; padding: 15px; border-radius: 5px; border-left: 4px solid #007bff; margin: 10px 0; font-family: monospace; }
        ul, ol { line-height: 1.8; }
        .highlight { background: #fff3cd; padding: 2px 4px; border-radius: 3px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>邮件服务器管理后台</h1>
        <div>欢迎, <?php echo htmlspecialchars($_SESSION['username']); ?> 
            (<a href="logout.php" style="color: white;">退出</a>)
        </div>
    </div>
    
    <div class="menu">
        <a href="index.php">仪表盘</a>
        <a href="users.php">用户管理</a>
        <a href="broadcast.php">群发邮件</a>
        <a href="filters.php">过滤规则</a>
        <a href="logs.php">系统日志</a>
        <a href="services.php">服务管理</a>
        <a href="settings.php">系统设置</a>
        <a href="help.php">帮助</a>
    </div>
    
    <div class="container">
        <h2>使用帮助</h2>
        
        <div class="section">
            <h3>系统概述</h3>
            <p>这是一个基于POP3和SMTP协议的邮件服务器管理系统，支持用户注册、邮件收发、系统管理等功能。</p>
        </div>
        
        <div class="section">
            <h3>功能模块</h3>
            
            <h4>1. 用户管理</h4>
            <ul>
                <li><strong>创建用户：</strong>可以创建新的用户账号，设置密码、管理员权限和激活状态</li>
                <li><strong>编辑用户：</strong>可以修改用户密码、权限和状态</li>
                <li><strong>删除用户：</strong>可以删除用户账号（不能删除自己）</li>
                <li><strong>用户列表：</strong>查看所有注册用户及其状态</li>
            </ul>
            
            
            <h4>2. 群发邮件</h4>
            <ul>
                <li><strong>发送给所有用户：</strong>可以一次性向所有激活用户发送通知邮件</li>
                <li><strong>发送给指定用户：</strong>可以选择特定用户进行群发</li>
                <li><strong>邮件内容：</strong>支持自定义主题和内容</li>
            </ul>
            
            <h4>3. 过滤规则</h4>
            <ul>
                <li><strong>邮箱过滤：</strong>可以阻止或允许特定邮箱地址</li>
                <li><strong>IP过滤：</strong>可以阻止或允许特定IP地址</li>
                <li><strong>规则管理：</strong>可以启用、禁用或删除过滤规则</li>
            </ul>
            
            <h4>4. 系统设置</h4>
            <ul>
                <li><strong>端口设置：</strong>配置SMTP端口（默认25）和POP3端口（默认110）</li>
                <li><strong>域名设置：</strong>设置邮件服务器域名（默认test.com）</li>
                <li><strong>邮箱管理：</strong>设置用户邮箱大小限制</li>
                <li><strong>日志设置：</strong>配置日志存储路径和最大大小</li>
                <li><strong>密码修改：</strong>管理员可以修改自己的密码</li>
            </ul>
            
            <h4>5. 服务管理</h4>
            <ul>
                <li><strong>SMTP服务：</strong>查看和管理SMTP服务状态</li>
                <li><strong>POP3服务：</strong>查看和管理POP3服务状态</li>
                <li><strong>服务起停：</strong>启动或停止邮件服务</li>
            </ul>
            
            <h4>6. 日志管理</h4>
            <ul>
                <li><strong>查看日志：</strong>查看SMTP和POP3服务器日志</li>
                <li><strong>日志过滤：</strong>按类型过滤日志（全部/SMTP/POP3）</li>
                <li><strong>清除日志：</strong>管理员可以清除日志记录</li>
            </ul>
        </div>
        
        <div class="section">
            <h3>启动服务器</h3>
            <p>要启动邮件服务器，需要在命令行执行以下命令：</p>
            <div class="code-block">
# 启动SMTP服务器（需要sudo权限）<br>
sudo php scripts/start_smtp.php<br><br>
# 启动POP3服务器（需要sudo权限）<br>
sudo php scripts/start_pop3.php
            </div>
            <p><span class="highlight">注意：</span>两个服务器需要分别在两个终端运行。</p>
        </div>
        
        <div class="section">
            <h3>测试邮件服务器</h3>
            <h4>测试SMTP（发送邮件）</h4>
            <div class="code-block">
telnet localhost 25<br>
HELO test<br>
MAIL FROM: &lt;user1@test.com&gt;<br>
RCPT TO: &lt;admin@test.com&gt;<br>
DATA<br>
Subject: 测试邮件<br>
From: user1@test.com<br>
To: admin@test.com<br><br>
这是一封测试邮件！<br>
.<br>
QUIT
            </div>
            
            <h4>测试POP3（接收邮件）</h4>
            <div class="code-block">
telnet localhost 110<br>
USER admin@test.com<br>
PASS 123456<br>
STAT<br>
LIST<br>
RETR 1<br>
QUIT
            </div>
        </div>
        
        <div class="section">
            <h3>常见问题</h3>
            
            <h4>Q: 端口被占用怎么办？</h4>
            <p>A: 检查端口占用情况：</p>
            <div class="code-block">
sudo netstat -tlnp | grep 25  # 检查SMTP端口<br>
sudo netstat -tlnp | grep 110 # 检查POP3端口
            </div>
            
            <h4>Q: 数据库连接失败？</h4>
            <p>A: 确保Docker容器正在运行：</p>
            <div class="code-block">
docker-compose ps<br>
docker-compose up -d mysql
            </div>
            
            <h4>Q: 如何重置数据库？</h4>
            <p>A: 执行以下命令：</p>
            <div class="code-block">
docker-compose down -v<br>
docker-compose up -d<br>
sleep 15
            </div>
        </div>
        
        <div class="section">
            <h3>默认账号</h3>
            <ul>
                <li><strong>管理员：</strong>admin@test.com / 123456</li>
                <li><strong>普通用户：</strong>user1@test.com / 123456</li>
            </ul>
        </div>
    </div>
</body>
</html>

