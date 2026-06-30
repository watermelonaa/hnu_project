<?php
/**
 * 系统设置页面
 * 
 * @uses SystemSettingsRepository
 * @uses UserRepository
 * @uses MailboxRepository
 */

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/storage/Database.php';
require_once __DIR__ . '/../src/storage/SystemSettingsRepository.php';
require_once __DIR__ . '/../src/storage/UserRepository.php';
require_once __DIR__ . '/../src/storage/MailboxRepository.php';
require_once __DIR__ . '/../src/utils/Validator.php';
require_once __DIR__ . '/../src/utils/Security.php';

//开启报错提示
ini_set('log_errors', 1);
ini_set('error_log', '/dev/stdout');

session_start();

// 是否登录验证
if (!isset($_SESSION['user_id'])) {
    header('Location: index.php');
    exit;
}


/** @var SystemSettingsRepository */
$settingsRepo = new SystemSettingsRepository();
/** @var UserRepository */
$userRepo = new UserRepository();
/** @var MailboxRepository */
$mailboxRepo = new MailboxRepository();
$message = '';
$error = '';
$domain = $settingsRepo->get('domain', 'test.com');

// 处理系统设置更新
if (isset($_POST['update_settings'])) {
    // SMTP端口
    if (isset($_POST['smtp_port'])) {
        $port = (int)$_POST['smtp_port'];
        if (Validator::validatePort($port)) {
            $settingsRepo->set('smtp_port', $port);
        } else {
            $error = "SMTP端口无效（1-65535）";
        }
    }
    
    // POP3端口
    if (isset($_POST['pop3_port'])) {
        $port = (int)$_POST['pop3_port'];
        if (Validator::validatePort($port)) {
            $settingsRepo->set('pop3_port', $port);
        } else {
            $error = "POP3端口无效（1-65535）";
        }
    }
    
    // 域名
    if (isset($_POST['domain'])) {
        $newDomain = trim($_POST['domain']);
        if (!empty($newDomain)) {
            /* **** 调试开始 **** */
            $oldDomain = $settingsRepo->get('domain', 'test.com');   // 实时旧值
            error_log('【调试】实时旧域名：' . $oldDomain);
            error_log('【调试】提交新域名：' . $newDomain);
            /* **** 调试结束 **** */

            if ($newDomain !== $oldDomain) {
                $settingsRepo->set('domain', $newDomain);

                require_once __DIR__ . '/../src/admin/SyncDomainService.php';
                $sync  = new SyncDomainService();
                $count = $sync->run($oldDomain, $newDomain);
                $message .= " 已同步 $count 个用户邮箱后缀。";

                /* **** 调试开始 **** */
                error_log('【调试】REPLACE 影响行数：' . $count);
                /* **** 调试结束 **** */
                
            }
        }
    }
    
    // 默认邮箱大小限制
    if (isset($_POST['mailbox_size_limit'])) {
        $size = (int)$_POST['mailbox_size_limit'];
        if ($size > 0) {
            $settingsRepo->set('mailbox_size_limit', $size);
        }
    }
    
    // 日志路径
    if (isset($_POST['log_path'])) {
        $settingsRepo->set('log_path', trim($_POST['log_path']));
    }
    
    // 日志最大大小
    if (isset($_POST['log_max_size'])) {
        $size = (int)$_POST['log_max_size'];
        if ($size > 0) {
            $settingsRepo->set('log_max_size', $size);
        }
    }
    
    if (empty($error)) {
        $message = "系统设置已更新";
    }
}

// 处理管理员密码修改
if (isset($_POST['change_admin_password'])) {
    $oldPassword = $_POST['old_password'] ?? '';
    $newPassword = $_POST['new_password'] ?? '';
    $confirmPassword = $_POST['confirm_password'] ?? '';
    
    $user = $userRepo->findById($_SESSION['user_id']);
    
    if (!Security::verifyPassword($oldPassword, $user['password_hash'])) {
        $error = "原密码错误";
    } else {
        $passwordValidation = Validator::validatePassword($newPassword, 6);
        if (!$passwordValidation['valid']) {
            $error = implode('<br>', $passwordValidation['errors']);
        } elseif ($newPassword !== $confirmPassword) {
            $error = "两次输入的密码不一致";
        } else {
            if ($userRepo->update($_SESSION['user_id'], ['password' => $newPassword])) {
                $message = "管理员密码已更新";
            } else {
                $error = "密码更新失败";
            }
        }
    }
}

// 处理用户邮箱大小设置
if (isset($_POST['set_mailbox_size'])) {
    $userId = (int)$_POST['user_id'];
    $sizeBytes = (int)$_POST['mailbox_size'];
    
    if ($sizeBytes > 0) {
        if ($mailboxRepo->setSizeLimit($userId, $sizeBytes)) {
            $message = "邮箱大小限制已更新";
        } else {
            $error = "更新失败";
        }
    } else {
        $error = "邮箱大小必须大于0";
    }
}

// 获取当前设置
$settings = $settingsRepo->getAll();
$users = $userRepo->getAll();
?>
<!DOCTYPE html>
<html>
<head>
    <title>系统设置 - 邮件服务器</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
        .header { background: #007bff; color: white; padding: 15px; margin: -20px -20px 20px -20px; }
        .menu { background: white; padding: 10px; margin-bottom: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .menu a { margin-right: 15px; text-decoration: none; color: #007bff; }
        .container { background: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .section { margin-bottom: 30px; padding-bottom: 20px; border-bottom: 1px solid #ddd; }
        .section:last-child { border-bottom: none; }
        .section h3 { margin-top: 0; color: #333; }
        .message { background: #d4edda; color: #155724; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        .error { background: #f8d7da; color: #721c24; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        .form-group { margin-bottom: 15px; }
        .form-group label { display: block; margin-bottom: 5px; font-weight: 500; }
        .form-group input, .form-group select { width: 100%; max-width: 400px; padding: 8px; border: 1px solid #ddd; border-radius: 4px; }
        .form-group small { color: #666; font-size: 12px; }
        .btn { padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; }
        .btn-primary { background: #007bff; color: white; }
        .btn-success { background: #28a745; color: white; }
        table { width: 100%; border-collapse: collapse; margin-top: 15px; }
        th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
        th { background: #f8f9fa; }
        .size-input { width: 150px; }
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
        <h2>系统设置</h2>
        
        <?php if ($message): ?>
            <div class="message"><?php echo $message; ?></div>
        <?php endif; ?>
        
        <?php if ($error): ?>
            <div class="error"><?php echo $error; ?></div>
        <?php endif; ?>
        
        <!-- 服务器端口设置 -->
        <div class="section">
            <h3>服务器端口设置</h3>
            <form method="POST">
                <div class="form-group">
                    <label>SMTP端口（默认25）</label>
                    <input type="number" name="smtp_port" value="<?php echo htmlspecialchars($settings['smtp_port'] ?? '25'); ?>" min="1" max="65535" required>
                    <small>SMTP服务器监听端口</small>
                </div>
                <div class="form-group">
                    <label>POP3端口（默认110）</label>
                    <input type="number" name="pop3_port" value="<?php echo htmlspecialchars($settings['pop3_port'] ?? '110'); ?>" min="1" max="65535" required>
                    <small>POP3服务器监听端口</small>
                </div>
                <button type="submit" name="update_settings" class="btn btn-primary">保存端口设置</button>
            </form>
        </div>
        
        <!-- 域名设置 -->
        <div class="section">
            <h3>域名设置</h3>
            <form method="POST">
                <div class="form-group">
                    <label>服务器域名（默认test.com）</label>
                    <input type="text" name="domain" value="<?php echo htmlspecialchars($settings['domain'] ?? 'test.com'); ?>" >
                    <small>邮件服务器域名，用户邮箱必须使用此域名</small>
                </div>

                <button type="submit" name="update_settings" class="btn btn-primary">保存域名设置</button>
            </form>
        </div>
        
        <!-- 邮箱管理 -->
        <div class="section">
            <h3>邮箱大小管理</h3>
            <form method="POST">
                <div class="form-group">
                    <label>默认邮箱大小限制（字节）</label>
                    <input type="number" name="mailbox_size_limit" value="<?php echo htmlspecialchars($settings['mailbox_size_limit'] ?? '104857600'); ?>" min="1" required>
                    <small>默认值：104857600 (100MB)</small>
                </div>
                <button type="submit" name="update_settings" class="btn btn-primary">保存默认大小</button>
            </form>
            
            <h4>用户邮箱大小设置</h4>
            <table>
                <thead>
                    <tr>
                        <th>用户</th>
                        <th>当前使用</th>
                        <th>限制大小</th>
                        <th>使用率</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <?php foreach ($users as $user): ?>
                        <?php 
                        $usage = $mailboxRepo->getUsage($user['id']);
                        $usedMB = round($usage['used'] / 1048576, 2);
                        $limitMB = round($usage['limit'] / 1048576, 2);
                        ?>
                        <tr>
                            <td><?php echo htmlspecialchars($user['username']); ?></td>
                            <td><?php echo $usedMB; ?> MB</td>
                            <td><?php echo $limitMB; ?> MB</td>
                            <td><?php echo $usage['percentage']; ?>%</td>
                            <td>
                                <form method="POST" style="display: inline;">
                                    <input type="hidden" name="user_id" value="<?php echo $user['id']; ?>">
                                    <input type="number" name="mailbox_size" value="<?php echo $usage['limit']; ?>" class="size-input" min="1" required>
                                    <button type="submit" name="set_mailbox_size" class="btn btn-success">设置</button>
                                </form>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        </div>
        
        <!-- 日志设置 -->
        <div class="section">
            <h3>日志设置</h3>
            <form method="POST">
                <div class="form-group">
                    <label>日志文件存储路径</label>
                    <input type="text" name="log_path" value="<?php echo htmlspecialchars($settings['log_path'] ?? '/var/log/mailserver'); ?>" required>
                    <small>日志文件存储的目录路径</small>
                </div>
                <div class="form-group">
                    <label>日志文件最大大小（字节）</label>
                    <input type="number" name="log_max_size" value="<?php echo htmlspecialchars($settings['log_max_size'] ?? '10485760'); ?>" min="1" required>
                    <small>默认值：10485760 (10MB)</small>
                </div>
                <button type="submit" name="update_settings" class="btn btn-primary">保存日志设置</button>
            </form>
        </div>
        
        <!-- 管理员密码修改 -->
        <div class="section">
            <h3>修改管理员密码</h3>
            <form method="POST">
                <div class="form-group">
                    <label>原密码</label>
                    <input type="password" name="old_password" required>
                </div>
                <div class="form-group">
                    <label>新密码</label>
                    <input type="password" name="new_password" required minlength="6">
                    <small>密码长度至少6个字符</small>
                </div>
                <div class="form-group">
                    <label>确认新密码</label>
                    <input type="password" name="confirm_password" required minlength="6">
                </div>
                <button type="submit" name="change_admin_password" class="btn btn-primary">修改密码</button>
            </form>
        </div>
    </div>
</body>
</html>

