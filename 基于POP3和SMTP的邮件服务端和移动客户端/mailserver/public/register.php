<?php
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/storage/Database.php';
require_once __DIR__ . '/../src/storage/UserRepository.php';
require_once __DIR__ . '/../src/utils/Validator.php';
require_once __DIR__ . '/../src/utils/Security.php';

session_start();

$error = '';
$success = '';

// 处理注册请求
if (isset($_POST['register'])) {
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';
    $confirmPassword = $_POST['confirm_password'] ?? '';
    
    // 验证CSRF令牌
    if (!Security::verifyCSRFToken($_POST['csrf_token'] ?? '')) {
        $error = "安全验证失败，请重试";
    } else {
        // 验证输入
        $usernameValidation = Validator::validateUsername($username);
        if (!$usernameValidation['valid']) {
            $error = implode('<br>', $usernameValidation['errors']);
        } else {
            // 验证邮箱域名（默认test.com）
            $domain = 'test.com';
            if (!Validator::validateEmailDomain($username, $domain)) {
                $error = "邮箱域名必须是 @{$domain}";
            } else {
                // 验证密码
                $passwordValidation = Validator::validatePassword($password, 6);
                if (!$passwordValidation['valid']) {
                    $error = implode('<br>', $passwordValidation['errors']);
                } else {
                    // 验证密码确认
                    $matchValidation = Validator::validatePasswordMatch($password, $confirmPassword);
                    if (!$matchValidation['valid']) {
                        $error = implode('<br>', $matchValidation['errors']);
                    } else {
                        // 尝试创建用户
                        try {
                            $userRepo = new UserRepository();
                            
                            // 检查用户名是否已存在
                            if ($userRepo->usernameExists($username)) {
                                $error = "该邮箱已被注册";
                            } else {
                                // 创建新用户（默认非管理员，激活状态）
                                $user = $userRepo->create($username, $password, false, true);
                                
                                if ($user) {
                                    $success = "注册成功！请使用您的账号登录。";
                                    // 3秒后跳转到登录页面
                                    header("Refresh: 3; url=index.php");
                                } else {
                                    $error = "注册失败，请稍后重试";
                                }
                            }
                        } catch (Exception $e) {
                            $error = "注册失败: " . $e->getMessage();
                        }
                    }
                }
            }
        }
    }
}

// 生成CSRF令牌
$csrfToken = Security::generateCSRFToken();
?>
<!DOCTYPE html>
<html>
<head>
    <title>用户注册 - 邮件服务器</title>
    <meta charset="UTF-8">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        
        .register-container {
            background: white;
            border-radius: 10px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
            padding: 40px;
            width: 100%;
            max-width: 450px;
        }
        
        h1 {
            color: #333;
            margin-bottom: 10px;
            text-align: center;
        }
        
        .subtitle {
            color: #666;
            text-align: center;
            margin-bottom: 30px;
            font-size: 14px;
        }
        
        .form-group {
            margin-bottom: 20px;
        }
        
        label {
            display: block;
            margin-bottom: 8px;
            color: #333;
            font-weight: 500;
            font-size: 14px;
        }
        
        input[type="text"],
        input[type="email"],
        input[type="password"] {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 14px;
            transition: border-color 0.3s;
        }
        
        input[type="text"]:focus,
        input[type="email"]:focus,
        input[type="password"]:focus {
            outline: none;
            border-color: #667eea;
        }
        
        .help-text {
            font-size: 12px;
            color: #999;
            margin-top: 5px;
        }
        
        .error {
            background: #fee;
            color: #c33;
            padding: 12px;
            border-radius: 5px;
            margin-bottom: 20px;
            font-size: 14px;
            border-left: 4px solid #c33;
        }
        
        .success {
            background: #efe;
            color: #3c3;
            padding: 12px;
            border-radius: 5px;
            margin-bottom: 20px;
            font-size: 14px;
            border-left: 4px solid #3c3;
        }
        
        button[type="submit"] {
            width: 100%;
            padding: 12px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 5px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        
        button[type="submit"]:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }
        
        button[type="submit"]:active {
            transform: translateY(0);
        }
        
        .login-link {
            text-align: center;
            margin-top: 20px;
            font-size: 14px;
            color: #666;
        }
        
        .login-link a {
            color: #667eea;
            text-decoration: none;
            font-weight: 500;
        }
        
        .login-link a:hover {
            text-decoration: underline;
        }
        
        .domain-hint {
            display: inline-block;
            background: #f0f0f0;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: monospace;
            font-size: 12px;
        }
    </style>
</head>
<body>
    <div class="register-container">
        <h1>用户注册</h1>
        <p class="subtitle">创建您的邮件服务器账号</p>
        
        <?php if ($error): ?>
            <div class="error"><?php echo $error; ?></div>
        <?php endif; ?>
        
        <?php if ($success): ?>
            <div class="success"><?php echo $success; ?></div>
        <?php else: ?>
            <form method="POST" action="">
                <input type="hidden" name="csrf_token" value="<?php echo htmlspecialchars($csrfToken); ?>">
                
                <div class="form-group">
                    <label for="username">邮箱地址</label>
                    <input 
                        type="email" 
                        id="username" 
                        name="username" 
                        value="<?php echo htmlspecialchars($_POST['username'] ?? ''); ?>"
                        placeholder="example@test.com" 
                        required
                        autofocus
                    >
                    <div class="help-text">请输入您的邮箱地址（域名必须是 <span class="domain-hint">@test.com</span>）</div>
                </div>
                
                <div class="form-group">
                    <label for="password">密码</label>
                    <input 
                        type="password" 
                        id="password" 
                        name="password" 
                        placeholder="至少6个字符" 
                        required
                        minlength="6"
                    >
                    <div class="help-text">密码长度至少需要6个字符</div>
                </div>
                
                <div class="form-group">
                    <label for="confirm_password">确认密码</label>
                    <input 
                        type="password" 
                        id="confirm_password" 
                        name="confirm_password" 
                        placeholder="请再次输入密码" 
                        required
                        minlength="6"
                    >
                </div>
                
                <button type="submit" name="register">注册</button>
            </form>
        <?php endif; ?>
        
        <div class="login-link">
            已有账号？<a href="index.php">立即登录</a>
        </div>
    </div>
</body>
</html>

