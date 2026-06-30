<?php
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/storage/Database.php';
require_once __DIR__ . '/../src/storage/UserRepository.php';
require_once __DIR__ . '/../src/storage/EmailRepository.php';
require_once __DIR__ . '/../src/admin/BroadcastService.php';
require_once __DIR__ . '/../src/utils/Security.php';

session_start();

// 身份验证
if (!isset($_SESSION['user_id'])) {
    header('Location: index.php');
    exit;
}

// 检查管理员权限
if (!$_SESSION['is_admin']) {
    die('权限不足：只有管理员可以访问此页面');
}

$broadcastService = new BroadcastService();
$userRepo = new UserRepository();
$message = '';
$error = '';

// 处理群发邮件
if (isset($_POST['send_broadcast'])) {
    $subject = trim($_POST['subject'] ?? '');
    $body = trim($_POST['body'] ?? '');
    $broadcastType = $_POST['broadcast_type'] ?? 'all';
    $recipients = trim($_POST['recipients'] ?? '');
    
    if (empty($subject)) {
        $error = "主题不能为空";
    } elseif (empty($body)) {
        $error = "邮件内容不能为空";
    } else {
        $senderEmail = $_SESSION['username'];
        
        try {
            if ($broadcastType === 'all') {
                // 群发给所有用户
                $result = $broadcastService->broadcastToAll($senderEmail, $subject, $body);
            } else {
                // 群发给指定用户
                $recipientList = array_filter(array_map('trim', explode(',', $recipients)));
                if (empty($recipientList)) {
                    $error = "请指定收件人";
                } else {
                    $result = $broadcastService->broadcastToUsers($senderEmail, $recipientList, $subject, $body);
                }
            }
            
            if (isset($result)) {
                if ($result['success'] > 0) {
                    $message = "群发成功！成功发送 {$result['success']} 封邮件";
                    if ($result['failed'] > 0) {
                        $message .= "，失败 {$result['failed']} 封";
                    }
                    if (!empty($result['errors'])) {
                        $error = "部分失败：" . implode('<br>', array_slice($result['errors'], 0, 5));
                        if (count($result['errors']) > 5) {
                            $error .= "<br>... 还有 " . (count($result['errors']) - 5) . " 个错误";
                        }
                    }
                } else {
                    $error = "群发失败：" . implode('<br>', $result['errors']);
                }
            }
        } catch (Exception $e) {
            $error = "群发失败: " . $e->getMessage();
        }
    }
}

// 获取所有用户列表（用于选择收件人）
$allUsers = $userRepo->getAll();
?>
<!DOCTYPE html>
<html>
<head>
    <title>群发邮件 - 邮件服务器</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
        .header { background: #007bff; color: white; padding: 15px; margin: -20px -20px 20px -20px; }
        .menu { background: white; padding: 10px; margin-bottom: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .menu a { margin-right: 15px; text-decoration: none; color: #007bff; }
        .container { background: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); max-width: 800px; }
        .message { background: #d4edda; color: #155724; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        .error { background: #f8d7da; color: #721c24; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        .form-group { margin-bottom: 20px; }
        .form-group label { display: block; margin-bottom: 8px; font-weight: 500; }
        .form-group input, .form-group textarea, .form-group select { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }
        .form-group textarea { min-height: 200px; font-family: monospace; }
        .form-group small { color: #666; font-size: 12px; }
        .btn { padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
        .btn-primary { background: #007bff; color: white; }
        .btn-primary:hover { background: #0056b3; }
        .radio-group { display: flex; gap: 20px; margin-bottom: 15px; }
        .radio-group label { display: flex; align-items: center; cursor: pointer; }
        .radio-group input[type="radio"] { width: auto; margin-right: 5px; }
        .user-list { max-height: 200px; overflow-y: auto; border: 1px solid #ddd; padding: 10px; border-radius: 4px; background: #f8f9fa; }
        .user-list-item { padding: 5px; }
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
        <h2>群发邮件</h2>
        
        <?php if ($message): ?>
            <div class="message"><?php echo $message; ?></div>
        <?php endif; ?>
        
        <?php if ($error): ?>
            <div class="error"><?php echo $error; ?></div>
        <?php endif; ?>
        
        <form method="POST">
            <div class="form-group">
                <label>发送方式</label>
                <div class="radio-group">
                    <label>
                        <input type="radio" name="broadcast_type" value="all" checked onchange="toggleRecipients()">
                        发送给所有用户
                    </label>
                    <label>
                        <input type="radio" name="broadcast_type" value="selected" onchange="toggleRecipients()">
                        发送给指定用户
                    </label>
                </div>
            </div>
            
            <div class="form-group" id="recipients-group" style="display: none;">
                <label>收件人（多个邮箱用逗号分隔）</label>
                <input type="text" name="recipients" placeholder="user1@test.com, user2@test.com">
                <small>请输入邮箱地址，多个邮箱用逗号分隔</small>
                <div class="user-list">
                    <strong>可用用户列表：</strong>
                    <?php foreach ($allUsers as $user): ?>
                        <?php if ($user['is_active'] && $user['username'] !== $_SESSION['username']): ?>
                            <div class="user-list-item"><?php echo htmlspecialchars($user['username']); ?></div>
                        <?php endif; ?>
                    <?php endforeach; ?>
                </div>
            </div>
            
            <div class="form-group">
                <label>邮件主题 *</label>
                <input type="text" name="subject" required placeholder="请输入邮件主题" value="<?php echo htmlspecialchars($_POST['subject'] ?? ''); ?>">
            </div>
            
            <div class="form-group">
                <label>邮件内容 *</label>
                <textarea name="body" required placeholder="请输入邮件内容"><?php echo htmlspecialchars($_POST['body'] ?? ''); ?></textarea>
            </div>
            
            <button type="submit" name="send_broadcast" class="btn btn-primary">发送群发邮件</button>
        </form>
    </div>
    
    <script>
        function toggleRecipients() {
            const broadcastType = document.querySelector('input[name="broadcast_type"]:checked').value;
            const recipientsGroup = document.getElementById('recipients-group');
            if (broadcastType === 'selected') {
                recipientsGroup.style.display = 'block';
            } else {
                recipientsGroup.style.display = 'none';
            }
        }
    </script>
</body>
</html>

