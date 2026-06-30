<?php
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/storage/Database.php';
require_once __DIR__ . '/../src/storage/ServiceRepository.php';
require_once __DIR__ . '/../src/storage/SystemSettingsRepository.php';

//错误提示
error_reporting(E_ALL);
ini_set('display_errors', 1);

session_start();

// 登录验证
if (!isset($_SESSION['user_id'])) {
    header('Location: index.php');
    exit;
}

$serviceRepo = new ServiceRepository();
$settingsRepo = new SystemSettingsRepository();
$message = '';
$error = '';

// 获取端口设置（放在函数定义之前）
$smtpPort = $settingsRepo->get('smtp_port', 25);
$pop3Port = $settingsRepo->get('pop3_port', 110);

function startService($serviceName, $port) {
    $scriptPath = __DIR__ . "/../scripts/start_{$serviceName}.php";
    $logFile    = __DIR__ . "/../logs/{$serviceName}.log";

    error_log("尝试启动服务: $serviceName , 端口: $port");

    if (!file_exists($scriptPath)) {
        return ['success' => false, 'message' => '启动脚本不存在'];
    }
    

    if (isServiceRunning($serviceName, $port)) {
        return ['success' => false, 'message' => '服务已在运行'];
    }

    @file_put_contents($logFile, '');
    //===== 修复：更稳健的命令执行 ===== 
    $raw = shell_exec(sprintf(
    'cd %s && (%s > %s 2>/dev/null </dev/null & echo $!)',
    escapeshellarg(dirname($scriptPath, 2)),
    'nohup php ' . escapeshellarg($scriptPath),
    escapeshellarg($logFile)
    ));
    /*$raw = shell_exec(sprintf(
    'cd %s && (%s > /dev/null 2>&1 </dev/null & echo $!)',
    escapeshellarg(dirname($scriptPath, 2)),
    'nohup php ' . escapeshellarg($scriptPath),
    escapeshellarg($logFile)      // 这一行现在仅用于占位，实际不再写
    ));*/
    
    // 修复trim(null)问题
    $raw = (string)$raw;  // 强制转换为字符串
    $pid = trim($raw);    // 现在可以安全trim
    
    error_log("Shell命令执行结果: " . var_export($raw, true));
    error_log("获取的PID: " . $pid);  // 修复：这里$pid已定义
    
    if (!is_numeric($pid) || $pid < 1) {
        error_log("PID无效: $pid");
        return ['success' => false, 'message' => '未能获取有效进程号'];
    }

    /* ===== 修复：添加进程状态检查 ===== */
    sleep(1);  // 先等待1秒
    
    // 检查进程是否还在运行
    $processExists = file_exists("/proc/$pid");
    error_log("进程是否存在: " . ($processExists ? "是" : "否"));
    
    if (!$processExists) {
        // 查看脚本为什么退出了
        if (file_exists($logFile)) {
            $logContent = file_get_contents($logFile);
            error_log("脚本输出日志:\n" . $logContent);
        }
        return ['success' => false, 'message' => '进程已退出'];
    }

    /* ===== 修复：更长的等待时间 ===== */
    $maxChecks = 6;
    for ($i = 0; $i < $maxChecks; $i++) {
        sleep(1);  // 每次检查等待1秒
        
        if (isServiceRunning($serviceName, $port)) {
            // 再次确认PID仍然有效
            if (file_exists("/proc/$pid")) {
                return ['success' => true, 'pid' => $pid];
            } else {
                // 进程已退出
                return ['success' => false, 'message' => '进程已退出'];
            }
        }
        
        error_log("第 " . ($i+1) . " 次检查：端口未监听");
    }

    // 启动失败 → 清理孤儿
    if (file_exists("/proc/$pid")) {
        @exec("kill -9 {$pid} 2>/dev/null");
    }
    
    // 输出失败原因
    if (file_exists($logFile)) {
        // 只读取最后10行，避免读取大文件
        $logContent = shell_exec("tail -10 " . escapeshellarg($logFile));
        error_log("最终日志内容(最后100行):\n" . $logContent);
    }
        
    return ['success' => false, 'message' => '服务端口未监听，启动失败'];
}
    

function stopService($serviceName, $pid, $port) {
    // ===== 改：优先用 DB 里的 PID，没有再现场嗅探 =====
    if (empty($pid) || !is_numeric($pid)) {
        $pid = getServicePid($serviceName, $port);
    }
    if ($pid) {
        @exec("kill {$pid} 2>/dev/null");
        sleep(1);
        if (isServiceRunning($serviceName, $port)) {
            @exec("kill -9 {$pid} 2>/dev/null");
            sleep(1);
        }
    }
    return !isServiceRunning($serviceName, $port);
}

function isServiceRunning($serviceName, $port) {
   // 方法1: 使用单引号确保变量正确展开
    $cmd1 = "netstat -tlnp 2>/dev/null | grep ':" . $port . "' | grep LISTEN";
    $result1 = shell_exec($cmd1);

    // 方法2: 使用ss命令（更可靠）
    $cmd2 = "ss -tlnp 2>/dev/null | grep ':" . $port . "'";
    $result2 = shell_exec($cmd2);
    
    // 方法3: 使用lsof（你已验证有效）
    $cmd3 = "lsof -i :" . $port . " 2>/dev/null";
    $result3 = shell_exec($cmd3);
    
    // 方法4: 直接socket连接测试（最可靠）
    $fp = @fsockopen('127.0.0.1', $port, $errno, $errstr, 1);
    if ($fp) {
        fclose($fp);
        return true;
    }
    
    // 任意一个方法有结果都算服务在运行
    return !empty(trim($result1 ?? '')) || 
           !empty(trim($result2 ?? '')) || 
           !empty(trim($result3 ?? ''));
}

function getServicePid($serviceName, $port) {
    return trim(shell_exec("lsof -ti: {$port} 2>/dev/null | head -1"));
}                    
// ===================== 修改部分结束 =====================

// 处理服务起停
if (isset($_GET['action'])) {
    $serviceName = $_GET['service'] ?? '';
    $action = $_GET['action'] ?? '';
    
    if ($serviceName === 'smtp' || $serviceName === 'pop3') {
        $port = $serviceName === 'smtp' ? $smtpPort : $pop3Port;
        if ($action === 'start') {
            $result = startService($serviceName, $port);
            if ($result['success']) {
                $pid = $result['pid'];
                $serviceRepo->updateStatus($serviceName, true, $pid);
                $message = strtoupper($serviceName) . "服务已启动 (PID: {$pid})";
            } else {
                $error = strtoupper($serviceName) . "服务启动失败: " . $result['message'];
            }     
        } elseif ($action === 'stop') {
            $status = $serviceRepo->getStatus($serviceName);
            $pid = $status['pid'] ?? getServicePid($serviceName, $port);
            
            if (stopService($serviceName, $pid, $port)) {
                $serviceRepo->updateStatus($serviceName, false, null);
                $message = strtoupper($serviceName) . "服务已停止";
            } else {
                $error = strtoupper($serviceName) . "服务停止失败";
            }
            // ===================== 修改部分结束 =====================
        }
    }
}

// 获取服务状态（修改判断逻辑）
// ===================== 修改部分开始 =====================
$smtpStatus = $serviceRepo->getStatus('smtp');
$pop3Status = $serviceRepo->getStatus('pop3');

// 实际检查服务是否运行（使用配置的端口）
$smtpRunning = isServiceRunning('smtp', $smtpPort);
$pop3Running = isServiceRunning('pop3', $pop3Port);

// 如果数据库状态与实际状态不一致，更新数据库
if ($smtpStatus['is_running'] != $smtpRunning) {
    $pid = $smtpRunning ? getServicePid('smtp', $smtpPort) : null;
    $serviceRepo->updateStatus('smtp', $smtpRunning, $pid);
}

if ($pop3Status['is_running'] != $pop3Running) {
    $pid = $pop3Running ? getServicePid('pop3', $pop3Port) : null;
    $serviceRepo->updateStatus('pop3', $pop3Running, $pid);
}

// 重新获取更新后的状态
$smtpStatus = $serviceRepo->getStatus('smtp');
$pop3Status = $serviceRepo->getStatus('pop3');
$smtpRunning = $smtpStatus['is_running'];
$pop3Running = $pop3Status['is_running'];
// ===================== 修改部分结束 =====================
?>
<!DOCTYPE html>
<html>
<head>
    <title>服务管理 - 邮件服务器</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
        .header { background: #007bff; color: white; padding: 15px; margin: -20px -20px 20px -20px; }
        .menu { background: white; padding: 10px; margin-bottom: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .menu a { margin-right: 15px; text-decoration: none; color: #007bff; }
        .container { background: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .message { background: #d4edda; color: #155724; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        .error { background: #f8d7da; color: #721c24; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        .service-box { border: 1px solid #ddd; padding: 20px; margin-bottom: 20px; border-radius: 5px; }
        .service-box h3 { margin-top: 0; }
        .status { display: inline-block; padding: 6px 12px; border-radius: 4px; font-weight: 500; margin-right: 10px; }
        .status-running { background: #28a745; color: white; }
        .status-stopped { background: #dc3545; color: white; }
        .btn { padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; text-decoration: none; display: inline-block; }
        .btn-success { background: #28a745; color: white; }
        .btn-danger { background: #dc3545; color: white; }
        .info { color: #666; font-size: 14px; margin-top: 10px; }
        .note { background: #fff3cd; border: 1px solid #ffc107; padding: 15px; border-radius: 5px; margin-top: 20px; }
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
        <h2>服务管理</h2>
        
        <?php if ($message): ?>
            <div class="message"><?php echo $message; ?></div>
        <?php endif; ?>
        
        <?php if ($error): ?>
            <div class="error"><?php echo $error; ?></div>
        <?php endif; ?>
        
        <!-- SMTP服务 -->
        <div class="service-box">
            <h3>SMTP服务（邮件发送）</h3>
            <p>
                <span class="status status-<?php echo $smtpRunning ? 'running' : 'stopped'; ?>">
                    <?php echo $smtpRunning ? '运行中' : '已停止'; ?>
                </span>
                <?php if ($smtpRunning): ?>
                    <a href="?service=smtp&action=stop" class="btn btn-danger" onclick="return confirm('确定要停止SMTP服务吗？');">停止服务</a>
                <?php else: ?>
                    <a href="?service=smtp&action=start" class="btn btn-success" onclick="return confirm('确定要启动SMTP服务吗？');">启动服务</a>
                <?php endif; ?>
            </p>
            <div class="info">
                <strong>端口：</strong><?php echo $smtpPort; ?><br>
                <strong>进程ID：</strong><?php echo $smtpStatus['pid'] ?? '-'; ?><br>
                <strong>最后启动：</strong><?php echo $smtpStatus['last_started_at'] ?? '-'; ?><br>
                <strong>最后停止：</strong><?php echo $smtpStatus['last_stopped_at'] ?? '-'; ?>
            </div>
        </div>
        
        <!-- POP3服务 -->
        <div class="service-box">
            <h3>POP3服务（邮件接收）</h3>
            <p>
                <span class="status status-<?php echo $pop3Running ? 'running' : 'stopped'; ?>">
                    <?php echo $pop3Running ? '运行中' : '已停止'; ?>
                </span>
                <?php if ($pop3Running): ?>
                    <a href="?service=pop3&action=stop" class="btn btn-danger" onclick="return confirm('确定要停止POP3服务吗？');">停止服务</a>
                <?php else: ?>
                    <a href="?service=pop3&action=start" class="btn btn-success" onclick="return confirm('确定要启动POP3服务吗？');">启动服务</a>
                <?php endif; ?>
            </p>
            <div class="info">
                <strong>端口：</strong><?php echo $pop3Port; ?><br>
                <strong>进程ID：</strong><?php echo $pop3Status['pid'] ?? '-'; ?><br>
                <strong>最后启动：</strong><?php echo $pop3Status['last_started_at'] ?? '-'; ?><br>
                <strong>最后停止：</strong><?php echo $pop3Status['last_stopped_at'] ?? '-'; ?>
            </div>
        </div>
        
        <div class="note">
            <strong>注意：</strong>
            <ul>
                <li>如果服务启动失败，请检查是否没有该端口访问权限或端口被占用</li>
                <li>当前配置：SMTP端口 <?php echo $smtpPort; ?>, POP3端口 <?php echo $pop3Port; ?></li>
            </ul>
        </div>
    </div>
    
    <!-- ===================== 新增部分：自动刷新状态 ===================== 
    <script>
    // 每5秒自动检查服务状态
    setInterval(function() {
        fetch(window.location.href)
            .then(response => response.text())
            .then(html => {
                // 从返回的HTML中提取运行状态
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = html;
                
                // 检查SMTP状态是否变化
                const smtpStatusText = tempDiv.querySelector('.service-box:nth-child(1) .status')?.textContent;
                const currentSmtpStatus = document.querySelector('.service-box:nth-child(1) .status')?.textContent;
                
                // 检查POP3状态是否变化
                const pop3StatusText = tempDiv.querySelector('.service-box:nth-child(2) .status')?.textContent;
                const currentPop3Status = document.querySelector('.service-box:nth-child(2) .status')?.textContent;
                
                // 如果有变化，刷新页面
                if (smtpStatusText !== currentSmtpStatus || pop3StatusText !== currentPop3Status) {
                    console.log('服务状态变化，刷新页面...');
                    location.reload();
                }
            })
            .catch(error => console.error('状态检查失败:', error));
    }, 30000);
    </script>**-->
    
</body>
</html>