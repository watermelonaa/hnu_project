<?php
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/storage/Database.php';
require_once __DIR__ . '/../src/storage/UserRepository.php';
require_once __DIR__ . '/../src/utils/Security.php';

session_start();

// 简单身份验证，检查用户是否已登录。如果未登录（$_SESSION['user_id']不存在），则重定向到登录页面。
function requireAuth() {
    if (!isset($_SESSION['user_id'])) {
         if (basename($_SERVER['PHP_SELF']) !== 'index.php') {
            header('Location: index.php');
            exit;
        }
    }
}

// 登录检查
if (isset($_POST['login'])) {
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';
    
    try {
        // 检查登录尝试次数（防止暴力破解）
        if (!Security::checkLoginAttempts($username)) {
            $error = "登录失败次数过多，请5分钟后再试";
        } else {
            $userRepo = new UserRepository();
            $user = $userRepo->verifyPassword($username, $password);
            
            if ($user && $user['is_active'] && $user['is_admin']) {
                // 登录成功，清除尝试记录
                Security::clearLoginAttempts($username);
                
                $_SESSION['user_id'] = $user['id'];
                $_SESSION['username'] = $user['username'];
                $_SESSION['is_admin'] = $user['is_admin'];
                header('Location: index.php');
                exit;
            }else if($user && !$user['is_active']){
                $error = "用户被禁用";
            }else if($user && $user['is_active'] && !$user['is_admin']){
                $error = "没有权限";
            }else{
                // 登录失败，记录尝试
                Security::recordLoginAttempt($username);
                $error = "用户名或密码错误";
            }
        }
    } catch (Exception $e) {
        $error = "登录失败: " . $e->getMessage();
    }
}

// 如果是登录页面
if (basename($_SERVER['PHP_SELF']) === 'index.php' && !isset($_SESSION['user_id'])) {
    ?>
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>邮件服务器管理后台 - 登录</title>
        <style>
            body { font-family: Arial, sans-serif; max-width: 400px; margin: 50px auto; padding: 20px; }
            .login-box { border: 1px solid #ddd; padding: 20px; border-radius: 5px; }
            input { width: 100%; padding: 8px; margin: 5px 0 15px 0; }
            button { background: #007bff; color: white; padding: 10px; border: none; width: 100%; }
            .error { color: red; margin-bottom: 15px; }
        </style>
    </head>
    <body>
        <div class="login-box">
            <h2>邮件服务器管理后台</h2>
            <?php if (isset($error)) echo "<div class='error'>$error</div>"; ?>
            <form method="POST">
                <div>
                    <label>用户名:</label>
                    <input type="text" name="username" value="admin@test.com" required>
                </div>
                <div>
                    <label>密码:</label>
                    <input type="password" name="password" value="123456" required>
                </div>
                <button type="submit" name="login">登录</button>
            </form>
            <p style="margin-top: 10px; font-size: 12px; color: #666;">
                测试账号: admin@test.com / 123456<br>
                普通账号: user1@test.com / 123456
            </p>
        </div>
    </body>
    </html>
    <?php
    exit;
}

requireAuth();
?>
<!DOCTYPE html>
<html>
<head>
    <title>邮件服务器管理后台</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
        .header { background: #007bff; color: white; padding: 15px; margin: -20px -20px 20px -20px; }
        .menu { background: #f8f9fa; padding: 10px; margin-bottom: 20px; }
        .menu a { margin-right: 15px; text-decoration: none; color: #007bff; }
        .stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 20px; }
        .stat-box { border: 1px solid #ddd; padding: 15px; text-align: center; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background: #f8f9fa; }
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
        <?php?>
            <a href="users.php">用户管理</a>
        <?php ?>
        <a href="broadcast.php">群发邮件</a>
        <?php ?>
        <a href="filters.php">过滤规则</a>
        <a href="logs.php">系统日志</a>
        <?php ?>
        <a href="services.php">服务管理</a>
        <a href="settings.php">系统设置</a>
        <?php ?>
        <a href="help.php">帮助</a>
    </div>
    
    <div class="stats">
        <?php
        $db = Database::getInstance();
        
        // 统计用户数
        $stmt = $db->query("SELECT COUNT(*) as count FROM users");
        $userCount = $stmt->fetch()['count'];
        
        // 统计邮件数
        $stmt = $db->query("SELECT COUNT(*) as count FROM emails WHERE is_deleted = 0");
        $emailCount = $stmt->fetch()['count'];
        
        // 统计今日日志
        $stmt = $db->query("SELECT COUNT(*) as count FROM server_logs WHERE DATE(created_at) = CURDATE()");
        $logCount = $stmt->fetch()['count'];
        
        // 统计活跃会话（简化版）
        $activeConnections = 0;
        ?>
        
        <div class="stat-box">
            <h3><?php echo $userCount; ?></h3>
            <p>注册用户</p>
        </div>
        <div class="stat-box">
            <h3><?php echo $emailCount; ?></h3>
            <p>总邮件数</p>
        </div>
        <div class="stat-box">
            <h3><?php echo $logCount; ?></h3>
            <p>今日日志</p>
        </div>
        <div class="stat-box">
            <h3><?php echo $activeConnections; ?></h3>
            <p>活跃连接</p>
        </div>
    </div>
    
    <h2>最近邮件</h2>
    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>发件人</th>
                <th>收件人</th>
                <th>主题</th>
                <th>时间</th>
            </tr>
        </thead>
        <tbody>
            <?php
            $stmt = $db->query("
                SELECT e.*, 
                       COALESCE(u1.username, e.sender) as sender_name,
                       COALESCE(u2.username, e.recipient) as recipient_name
                FROM emails e
                LEFT JOIN users u1 ON e.sender_id = u1.id
                LEFT JOIN users u2 ON e.recipient_id = u2.id
                WHERE e.is_deleted = 0
                ORDER BY e.created_at DESC
                LIMIT 10
            ");
            
            while ($email = $stmt->fetch()) {
                echo "<tr>";
                echo "<td>{$email['id']}</td>";
                echo "<td>" . htmlspecialchars($email['sender_name'] ?? '未知') . "</td>";
                echo "<td>" . htmlspecialchars($email['recipient_name'] ?? '未知') . "</td>";
                echo "<td>" . htmlspecialchars($email['subject'] ?? '(无主题)') . "</td>";
                echo "<td>{$email['created_at']}</td>";
                echo "</tr>";
            }
            ?>
        </tbody>
    </table>
</body>
</html>