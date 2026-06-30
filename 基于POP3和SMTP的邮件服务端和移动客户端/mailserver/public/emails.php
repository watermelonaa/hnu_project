<?php
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/storage/Database.php';
require_once __DIR__ . '/../src/storage/EmailRepository.php';
require_once __DIR__ . '/../src/utils/Security.php';

session_start();

// 身份验证
if (!isset($_SESSION['user_id'])) {
    header('Location: index.php');
    exit;
}

$emailRepo = new EmailRepository();
$message = '';
$error = '';

// 处理删除邮件
if (isset($_GET['delete'])) {
    $emailId = (int)$_GET['delete'];
    if ($emailRepo->delete($emailId)) {
        $message = "邮件删除成功";
    } else {
        $error = "删除失败";
    }
}

// 处理标记已读
if (isset($_GET['mark_read'])) {
    $emailId = (int)$_GET['mark_read'];
    if ($emailRepo->markAsRead($emailId)) {
        $message = "邮件已标记为已读";
    }
}

// 获取邮件列表
$isAdmin = $_SESSION['is_admin'] ?? false;
$userId = $_SESSION['user_id'];

// 分页参数
$page = isset($_GET['page']) ? max(1, (int)$_GET['page']) : 1;
$perPage = 20;
$offset = ($page - 1) * $perPage;

// 获取邮件
if ($isAdmin) {
    $emails = $emailRepo->getAll($perPage, $offset);
    $totalEmails = $emailRepo->getCount();
} else {
    $emails = $emailRepo->getInbox($userId, $perPage, $offset);
    $totalEmails = $emailRepo->getCount($userId);
}

$totalPages = ceil($totalEmails / $perPage);
?>
<!DOCTYPE html>
<html>
<head>
    <title>邮件管理 - 邮件服务器</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
        .header { background: #007bff; color: white; padding: 15px; margin: -20px -20px 20px -20px; }
        .menu { background: white; padding: 10px; margin-bottom: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .menu a { margin-right: 15px; text-decoration: none; color: #007bff; }
        .container { background: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .message { background: #d4edda; color: #155724; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        .error { background: #f8d7da; color: #721c24; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background: #f8f9fa; font-weight: 600; }
        tr:hover { background: #f8f9fa; }
        .btn { padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; text-decoration: none; display: inline-block; }
        .btn-primary { background: #007bff; color: white; }
        .btn-danger { background: #dc3545; color: white; }
        .btn-success { background: #28a745; color: white; }
        .btn-small { padding: 4px 8px; font-size: 12px; }
        .badge { padding: 4px 8px; border-radius: 3px; font-size: 12px; font-weight: 500; }
        .badge-read { background: #6c757d; color: white; }
        .badge-unread { background: #007bff; color: white; }
        .email-unread { font-weight: bold; }
        .pagination { margin-top: 20px; text-align: center; }
        .pagination a { display: inline-block; padding: 8px 12px; margin: 0 4px; text-decoration: none; border: 1px solid #ddd; border-radius: 4px; }
        .pagination a:hover { background: #f8f9fa; }
        .pagination .current { background: #007bff; color: white; border-color: #007bff; }
        .email-preview { max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .modal { display: none; position: fixed; z-index: 1000; left: 0; top: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); overflow: auto; }
        .modal-content { background: white; margin: 50px auto; padding: 20px; width: 80%; max-width: 800px; border-radius: 5px; }
        .close { float: right; font-size: 28px; font-weight: bold; cursor: pointer; }
        .email-body { white-space: pre-wrap; background: #f8f9fa; padding: 15px; border-radius: 5px; margin-top: 10px; }
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
        <h2>邮箱管理 <?php if ($isAdmin): ?>(全部邮件)<?php else: ?>(我的收件箱)<?php endif; ?></h2>
        
        <?php if ($message): ?>
            <div class="message"><?php echo $message; ?></div>
        <?php endif; ?>
        
        <?php if ($error): ?>
            <div class="error"><?php echo $error; ?></div>
        <?php endif; ?>
        
        <p>共 <?php echo $totalEmails; ?> 封邮件</p>
        
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>发件人</th>
                    <th>收件人</th>
                    <th>主题</th>
                    <th>状态</th>
                    <th>时间</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <?php if (empty($emails)): ?>
                    <tr>
                        <td colspan="7" style="text-align: center; padding: 40px;">
                            暂无邮件
                        </td>
                    </tr>
                <?php else: ?>
                    <?php foreach ($emails as $email): ?>
                    <tr class="<?php echo $email['is_read'] ? '' : 'email-unread'; ?>">
                        <td><?php echo $email['id']; ?></td>
                        <td><?php echo htmlspecialchars($email['sender_name'] ?? $email['sender'] ?? '未知'); ?></td>
                        <td><?php echo htmlspecialchars($email['recipient_name'] ?? $email['recipient'] ?? '未知'); ?></td>
                        <td class="email-preview">
                            <a href="#" onclick="viewEmail(<?php echo htmlspecialchars(json_encode($email)); ?>); return false;">
                                <?php echo htmlspecialchars($email['subject'] ?? '(无主题)'); ?>
                            </a>
                        </td>
                        <td>
                            <?php if ($email['is_read']): ?>
                                <span class="badge badge-read">已读</span>
                            <?php else: ?>
                                <span class="badge badge-unread">未读</span>
                            <?php endif; ?>
                        </td>
                        <td><?php echo $email['created_at']; ?></td>
                        <td>
                            <a href="#" onclick="viewEmail(<?php echo htmlspecialchars(json_encode($email)); ?>); return false;" class="btn btn-primary btn-small">查看</a>
                            <?php if (!$email['is_read']): ?>
                                <a href="?mark_read=<?php echo $email['id']; ?>" class="btn btn-success btn-small">标记已读</a>
                            <?php endif; ?>
                            <a href="?delete=<?php echo $email['id']; ?>" class="btn btn-danger btn-small" onclick="return confirm('确定要删除此邮件吗？');">删除</a>
                        </td>
                    </tr>
                    <?php endforeach; ?>
                <?php endif; ?>
            </tbody>
        </table>
        
        <!-- 分页 -->
        <?php if ($totalPages > 1): ?>
        <div class="pagination">
            <?php if ($page > 1): ?>
                <a href="?page=<?php echo $page - 1; ?>">上一页</a>
            <?php endif; ?>
            
            <?php for ($i = 1; $i <= $totalPages; $i++): ?>
                <?php if ($i == $page): ?>
                    <span class="current"><?php echo $i; ?></span>
                <?php else: ?>
                    <a href="?page=<?php echo $i; ?>"><?php echo $i; ?></a>
                <?php endif; ?>
            <?php endfor; ?>
            
            <?php if ($page < $totalPages): ?>
                <a href="?page=<?php echo $page + 1; ?>">下一页</a>
            <?php endif; ?>
        </div>
        <?php endif; ?>
    </div>
    
    <!-- 查看邮件模态框 -->
    <div id="emailModal" class="modal">
        <div class="modal-content">
            <span class="close" onclick="closeEmailModal()">&times;</span>
            <h3 id="email-subject">邮件详情</h3>
            <div>
                <strong>发件人：</strong><span id="email-sender"></span><br>
                <strong>收件人：</strong><span id="email-recipient"></span><br>
                <strong>时间：</strong><span id="email-time"></span><br>
                <strong>主题：</strong><span id="email-subject-text"></span>
            </div>
            <div class="email-body" id="email-body"></div>
        </div>
    </div>
    
    <script>
        function viewEmail(email) {
            document.getElementById('email-subject').textContent = email.subject || '(无主题)';
            document.getElementById('email-subject-text').textContent = email.subject || '(无主题)';
            document.getElementById('email-sender').textContent = email.sender_name || email.sender || '未知';
            document.getElementById('email-recipient').textContent = email.recipient_name || email.recipient || '未知';
            document.getElementById('email-time').textContent = email.created_at;
            document.getElementById('email-body').textContent = email.body || '(无内容)';
            document.getElementById('emailModal').style.display = 'block';
        }
        
        function closeEmailModal() {
            document.getElementById('emailModal').style.display = 'none';
        }
        
        window.onclick = function(event) {
            var modal = document.getElementById('emailModal');
            if (event.target == modal) {
                closeEmailModal();
            }
        }
    </script>
</body>
</html>

