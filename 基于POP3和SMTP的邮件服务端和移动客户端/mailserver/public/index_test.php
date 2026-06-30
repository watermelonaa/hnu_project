<?php
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/storage/Database.php';
require_once __DIR__ . '/../src/storage/UserRepository.php';
require_once __DIR__ . '/../src/utils/Security.php';

session_start();

// 简单身份验证，检查用户是否已登录。如果未登录（$_SESSION['user_id']不存在），则重定向到登录页面。
function requireAuth() {
    if (!isset($_SESSION['user_id'])) {
        header('Location: index.php');
        exit;
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
            } else {
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
    <html lang="zh-CN">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>邮件服务器管理后台 - 登录</title>
        <style>
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
                background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 20px;
            }
            
            .login-container {
                width: 100%;
                max-width: 420px;
                animation: fadeIn 0.5s ease;
            }
            
            .login-header {
                text-align: center;
                margin-bottom: 30px;
            }
            
            .login-header h1 {
                color: #007bff;
                font-size: 28px;
                font-weight: 600;
                margin-bottom: 8px;
            }
            
            .login-header p {
                color: #6c757d;
                font-size: 14px;
            }
            
            .login-card {
                background: white;
                border-radius: 12px;
                box-shadow: 0 8px 30px rgba(0, 123, 255, 0.15);
                padding: 40px;
                border: 1px solid rgba(0, 123, 255, 0.1);
            }
            
            .error-message {
                background-color: #f8d7da;
                color: #721c24;
                padding: 12px;
                border-radius: 6px;
                margin-bottom: 20px;
                border-left: 4px solid #dc3545;
                font-size: 14px;
                display: flex;
                align-items: center;
                gap: 8px;
            }
            
            .error-message:before {
                content: "⚠";
                font-size: 16px;
            }
            
            .form-group {
                margin-bottom: 24px;
            }
            
            .form-group label {
                display: block;
                color: #495057;
                font-weight: 500;
                margin-bottom: 8px;
                font-size: 14px;
            }
            
            .form-control {
                width: 100%;
                padding: 12px 16px;
                border: 2px solid #e9ecef;
                border-radius: 8px;
                font-size: 15px;
                transition: all 0.3s ease;
                background-color: #f8f9fa;
            }
            
            .form-control:focus {
                outline: none;
                border-color: #007bff;
                background-color: white;
                box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
            }
            
            .form-control:hover {
                border-color: #ced4da;
            }
            
            .login-btn {
                width: 100%;
                padding: 14px;
                background: linear-gradient(to right, #007bff, #0056b3);
                color: white;
                border: none;
                border-radius: 8px;
                font-size: 16px;
                font-weight: 600;
                cursor: pointer;
                transition: all 0.3s ease;
                margin-top: 10px;
            }
            
            .login-btn:hover {
                background: linear-gradient(to right, #0069d9, #004085);
                transform: translateY(-1px);
                box-shadow: 0 4px 12px rgba(0, 123, 255, 0.2);
            }
            
            .login-btn:active {
                transform: translateY(0);
            }
            
            .test-accounts {
                margin-top: 25px;
                padding-top: 20px;
                border-top: 1px solid #e9ecef;
                text-align: center;
            }
            
            .test-accounts p {
                color: #6c757d;
                font-size: 13px;
                line-height: 1.5;
                margin-bottom: 5px;
            }
            
            .test-accounts strong {
                color: #495057;
                font-weight: 600;
            }
            
            @keyframes fadeIn {
                from {
                    opacity: 0;
                    transform: translateY(-20px);
                }
                to {
                    opacity: 1;
                    transform: translateY(0);
                }
            }
            
            @media (max-width: 480px) {
                .login-card {
                    padding: 30px 25px;
                }
                
                .login-header h1 {
                    font-size: 24px;
                }
                
                body {
                    padding: 15px;
                }
            }
        </style>
    </head>
    <body>
        <div class="login-container">
            <div class="login-header">
                <h1>邮件服务器管理</h1>
                <p>安全登录到管理后台</p>
            </div>
            
            <div class="login-card">
                <?php if (isset($error)): ?>
                <div class="error-message">
                    <?php echo htmlspecialchars($error); ?>
                </div>
                <?php endif; ?>
                
                <form method="POST" action="" id="loginForm">
                    <div class="form-group">
                        <label for="username">用户名</label>
                        <input type="text" 
                               id="username" 
                               name="username" 
                               class="form-control" 
                               value="admin@test.com" 
                               required 
                               placeholder="请输入用户名或邮箱">
                    </div>
                    
                    <div class="form-group">
                        <label for="password">密码</label>
                        <input type="password" 
                               id="password" 
                               name="password" 
                               class="form-control" 
                               value="123456" 
                               required 
                               placeholder="请输入密码">
                    </div>
                    
                    <button type="submit" name="login" class="login-btn">
                        登录系统
                    </button>
                </form>
                
                <div class="test-accounts">
                    <p><strong>测试账号</strong></p>
                    <p>管理员: admin@test.com / 123456</p>
                    <p>普通用户: user1@test.com / 123456</p>
                </div>
            </div>
        </div>
        
        <script>
            // 简单的表单验证增强
            document.getElementById('loginForm').addEventListener('submit', function(e) {
                const username = document.getElementById('username').value.trim();
                const password = document.getElementById('password').value;
                
                if (!username) {
                    e.preventDefault();
                    alert('请输入用户名');
                    return false;
                }
                
                if (!password) {
                    e.preventDefault();
                    alert('请输入密码');
                    return false;
                }
                
                // 登录按钮状态变化
                const submitBtn = e.target.querySelector('button[type="submit"]');
                submitBtn.disabled = true;
                submitBtn.innerHTML = '登录中...';
                submitBtn.style.opacity = '0.8';
            });
        </script>
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
        <?php if ($_SESSION['is_admin']) { ?>
            <a href="users.php">用户管理</a>
        <?php } ?>
        <a href="emails.php">邮件管理</a>
        <?php if ($_SESSION['is_admin']) { ?>
            <a href="broadcast.php">群发邮件</a>
        <?php } ?>
        <a href="filters.php">过滤规则</a>
        <a href="logs.php">系统日志</a>
        <?php if ($_SESSION['is_admin']) { ?>
            <a href="services.php">服务管理</a>
            <a href="settings.php">系统设置</a>
        <?php } ?>
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