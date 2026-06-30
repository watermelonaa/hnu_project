<?php
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/storage/Database.php';
require_once __DIR__ . '/../src/storage/SystemSettingsRepository.php';
//
error_reporting(E_ALL);
ini_set('display_errors', 1);

session_start();

// 身份验证
if (!isset($_SESSION['user_id'])) {
    header('Location: index.php');
    exit;
}

$db = Database::getInstance();
$settingsRepo = new SystemSettingsRepository();
$message = '';
$error = '';

// 处理清除日志
if (isset($_GET['clear_logs'])) {
    $logType = $_GET['clear_logs'] ?? '';
    
    if ($logType === 'all') {
        $stmt = $db->prepare("DELETE FROM server_logs");
        $stmt->execute();
        $message = "所有日志已清除";
    } elseif ($logType === 'smtp') {
        $stmt = $db->prepare("DELETE FROM server_logs WHERE log_type = 'SMTP'");
        $stmt->execute();
        $message = "SMTP日志已清除";
    } elseif ($logType === 'pop3') {
        $stmt = $db->prepare("DELETE FROM server_logs WHERE log_type = 'POP3'");
        $stmt->execute();
        $message = "POP3日志已清除";
    }
}

// 获取日志统计
$stmt = $db->query("SELECT COUNT(*) as count FROM server_logs");
$totalLogs = $stmt->fetch()['count'];

$stmt = $db->query("SELECT COUNT(*) as count FROM server_logs WHERE log_type = 'SMTP'");
$smtpLogs = $stmt->fetch()['count'];

$stmt = $db->query("SELECT COUNT(*) as count FROM server_logs WHERE log_type = 'POP3'");
$pop3Logs = $stmt->fetch()['count'];

// 分页参数
$page = isset($_GET['page']) ? max(1, (int)$_GET['page']) : 1;
$perPage = 50;
$offset = ($page - 1) * $perPage;

// 过滤参数
$filterType = $_GET['type'] ?? 'all';

// 获取日志列表
if ($filterType === 'smtp') {
    $stmt = $db->prepare("
        SELECT l.*, u.username 
        FROM server_logs l
        LEFT JOIN users u ON l.user_id = u.id
        WHERE l.log_type = 'SMTP'
        ORDER BY l.created_at DESC
        LIMIT ? OFFSET ?
    ");
    $stmt->execute([$perPage, $offset]);
    
    $countStmt = $db->prepare("SELECT COUNT(*) as count FROM server_logs WHERE log_type = 'SMTP'");
    $countStmt->execute();
    $totalLogs = $countStmt->fetch()['count'];
} elseif ($filterType === 'pop3') {
    $stmt = $db->prepare("
        SELECT l.*, u.username 
        FROM server_logs l
        LEFT JOIN users u ON l.user_id = u.id
        WHERE l.log_type = 'POP3'
        ORDER BY l.created_at DESC
        LIMIT ? OFFSET ?
    ");
    $stmt->execute([$perPage, $offset]);
    
    $countStmt = $db->prepare("SELECT COUNT(*) as count FROM server_logs WHERE log_type = 'POP3'");
    $countStmt->execute();
    $totalLogs = $countStmt->fetch()['count'];
} else {
    $stmt = $db->prepare("
        SELECT l.*, u.username 
        FROM server_logs l
        LEFT JOIN users u ON l.user_id = u.id
        ORDER BY l.created_at DESC
        LIMIT ? OFFSET ?
    ");
    $stmt->execute([$perPage, $offset]);
}

$logs = $stmt->fetchAll();
$totalPages = ceil($totalLogs / $perPage);

// 获取日志设置
$logPath = $settingsRepo->get('log_path', '/var/log/mailserver');
$logMaxSize = $settingsRepo->get('log_max_size', 10485760);
?>
<!DOCTYPE html>
<html>
<head>
    <title>系统日志 - 邮件服务器</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
        .header { background: #007bff; color: white; padding: 15px; margin: -20px -20px 20px -20px; }
        .menu { background: white; padding: 10px; margin-bottom: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .menu a { margin-right: 15px; text-decoration: none; color: #007bff; }
        .container { background: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .message { background: #d4edda; color: #155724; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        .error { background: #f8d7da; color: #721c24; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        .stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 20px; }
        .stat-box { border: 1px solid #ddd; padding: 15px; text-align: center; border-radius: 5px; }
        .stat-box h3 { margin: 0; font-size: 24px; color: #007bff; }
        .stat-box p { margin: 5px 0 0 0; color: #666; }
        .filters { margin-bottom: 20px; }
        .filters a { display: inline-block; padding: 8px 16px; margin-right: 10px; text-decoration: none; border: 1px solid #ddd; border-radius: 4px; }
        .filters a.active { background: #007bff; color: white; border-color: #007bff; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background: #f8f9fa; }
        .btn { padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; text-decoration: none; display: inline-block; }
        .btn-danger { background: #dc3545; color: white; }
        .pagination { margin-top: 20px; text-align: center; }
        .pagination a { display: inline-block; padding: 8px 12px; margin: 0 4px; text-decoration: none; border: 1px solid #ddd; border-radius: 4px; }
        .pagination a:hover { background: #f8f9fa; }
        .pagination .current { background: #007bff; color: white; border-color: #007bff; }
        .log-info { background: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
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
        <h2>系统日志管理</h2>
        
        <?php if ($message): ?>
            <div class="message"><?php echo $message; ?></div>
        <?php endif; ?>
        
        <?php if ($error): ?>
            <div class="error"><?php echo $error; ?></div>
        <?php endif; ?>
        
        <!-- 日志统计 -->
        <div class="stats">
            <div class="stat-box">
                <h3><?php echo $totalLogs; ?></h3>
                <p>总日志数</p>
            </div>
            <div class="stat-box">
                <h3><?php echo $smtpLogs; ?></h3>
                <p>SMTP日志</p>
            </div>
            <div class="stat-box">
                <h3><?php echo $pop3Logs; ?></h3>
                <p>POP3日志</p>
            </div>
        </div>
        
        <!-- 日志信息 -->
        <div class="log-info">
            <strong>日志存储路径：</strong><?php echo htmlspecialchars($logPath); ?><br>
            <strong>日志文件最大大小：</strong><?php echo round($logMaxSize / 1048576, 2); ?> MB
        </div>
        
        <!-- 过滤和操作 -->
        <div class="filters">
            <a href="?type=all" class="<?php echo $filterType === 'all' ? 'active' : ''; ?>">全部</a>
            <a href="?type=smtp" class="<?php echo $filterType === 'smtp' ? 'active' : ''; ?>">SMTP日志</a>
            <a href="?type=pop3" class="<?php echo $filterType === 'pop3' ? 'active' : ''; ?>">POP3日志</a>
            <?php if ($_SESSION['is_admin'] ?? false): ?>
                <a href="?clear_logs=smtp" class="btn btn-danger" onclick="return confirm('确定要清除SMTP日志吗？');">清除SMTP日志</a>
                <a href="?clear_logs=pop3" class="btn btn-danger" onclick="return confirm('确定要清除POP3日志吗？');">清除POP3日志</a>
                <a href="?clear_logs=all" class="btn btn-danger" onclick="return confirm('确定要清除所有日志吗？');">清除所有日志</a>
            <?php endif; ?>
        </div>
        
        <!-- 日志列表 -->
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>类型</th>
                    <th>消息</th>
                    <th>用户</th>
                    <th>IP地址</th>
                    <th>时间</th>
                </tr>
            </thead>
            <tbody>
                <?php if (empty($logs)): ?>
                    <tr>
                        <td colspan="6" style="text-align: center; padding: 40px;">暂无日志</td>
                    </tr>
                <?php else: ?>
                    <?php foreach ($logs as $log): ?>
                    <tr>
                        <td><?php echo $log['id']; ?></td>
                        <td><?php echo htmlspecialchars($log['log_type'] ?? '-'); ?></td>
                        <td><?php echo htmlspecialchars($log['message'] ?? '-'); ?></td>
                        <td><?php echo htmlspecialchars($log['username'] ?? '-'); ?></td>
                        <td><?php echo htmlspecialchars($log['client_ip'] ?? '-'); ?></td>
                        <td><?php echo $log['created_at']; ?></td>
                    </tr>
                    <?php endforeach; ?>
                <?php endif; ?>
            </tbody>
        </table>
        
        <!-- 分页 -->
        <?php if ($totalPages > 1): ?>
        <div class="pagination">
            <?php if ($page > 1): ?>
                <a href="?page=<?php echo $page - 1; ?>&type=<?php echo $filterType; ?>">上一页</a>
            <?php endif; ?>
            
            <?php for ($i = 1; $i <= $totalPages; $i++): ?>
                <?php if ($i == $page): ?>
                    <span class="current"><?php echo $i; ?></span>
                <?php else: ?>
                    <a href="?page=<?php echo $i; ?>&type=<?php echo $filterType; ?>"><?php echo $i; ?></a>
                <?php endif; ?>
            <?php endfor; ?>
            
            <?php if ($page < $totalPages): ?>
                <a href="?page=<?php echo $page + 1; ?>&type=<?php echo $filterType; ?>">下一页</a>
            <?php endif; ?>
        </div>
        <?php endif; ?>
    </div>
</body>
</html>

