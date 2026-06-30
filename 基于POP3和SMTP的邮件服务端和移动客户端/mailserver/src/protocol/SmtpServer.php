<?php
/**
 * 一体化的SMTP服务器
 * 
 * 注意：本类同时包含：
 * 1. 网络层（Server功能）- 负责socket通信
 * 2. 协议层（Handler功能）- 负责SMTP协议处理
 * 
 * 这种设计对于教学项目是合适的，保持了简单性。
 * 生产环境可以考虑拆分为 SmtpServer + SmtpHandler。
 */

ini_set('default_charset', 'UTF-8');
mb_internal_encoding('UTF-8'); 

/**
 * 最简SMTP服务器 - 负责收信
 * 运行：php src/SmtpServer.php
 */

require_once __DIR__ . '/../storage/MailboxRepository.php';
require_once __DIR__ . '/../storage/UserRepository.php';
require_once __DIR__ . '/../storage/FilterRepository.php';
require_once __DIR__ . '/../storage/SystemSettingsRepository.php';

$settingsRepo = new SystemSettingsRepository();
// 获取端口设置（放在函数定义之前）
$smtpPort = $settingsRepo->get('smtp_port', 25);
$pop3Port = $settingsRepo->get('pop3_port', 110);

class SimpleSmtpServer
{
    private $socket;
    private $isRunning = false;
    private $db;  // 数据库连接
    private $filterRepo;
    private $mailboxRepo;
    private $userRepo;

    // 新增：存储主机和端口
    private $host;
    private $port;
    private $clientIp; 
    
    public function __construct($host = '0.0.0.0', $port = 25)
    {
        echo "SMTP服务器启动在 {$host}:{$port}\n";
        echo "按 Ctrl+C 停止\n\n";
        
        // 存储配置，而不是立即使用
        $this->host = $host;
        $this->port = $port;
        // 连接数据库
        $this->connectDB();
        
        // 初始化Repository
        $this->filterRepo = new FilterRepository();
        $this->mailboxRepo = new MailboxRepository();
        $this->userRepo = new UserRepository();
    }
    
    private function connectDB()
    {
        try {
            $this->db = new PDO(
                'mysql:host=127.0.0.1;port=3308;dbname=mail_server',
                'mail_user',
                'user123',
                [
                PDO::MYSQL_ATTR_INIT_COMMAND => "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci",
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
                ]
            );
           // $this->db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            echo "数据库连接成功\n";
        } catch (PDOException $e) {
            echo "数据库连接失败: " . $e->getMessage() . "\n";
            exit(1);
        }
    }
    
    public function start()
    {
        // 创建socket（邮局开门）
        $this->socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
        socket_set_option($this->socket, SOL_SOCKET, SO_REUSEADDR, 1);
        socket_bind($this->socket, '0.0.0.0',$this->port);
        socket_listen($this->socket, 5);
        
        $this->isRunning = true;
        
        while ($this->isRunning) {
            // 等待客户连接（有人来寄信）
            $client = socket_accept($this->socket);
            if ($client !== false) {
                $this->handleClient($client);
                socket_close($client);
            }
        }
        
        socket_close($this->socket);
    }
    
    private function handleClient($client)
    {
        // 获取客户端IP地址
        socket_getpeername($client, $clientIp);
        $clientIp = $clientIp ?: 'unknown';
        $this->clientIp = $clientIp;
        // 记录连接日志
        $this->log("客户端连接",$this->clientIp);
        
        try {
            // 1. 说欢迎语
            $this->send($client, "220 mail.simple.com SMTP Ready");
            
            // 2. 等待客户说 HELO
            $this->waitForCommand($client, 'HELO', 'HELO或EHLO');
            
            // 3. 检查IP过滤规则
            if ($this->filterRepo->isIPBlocked($clientIp)) {
                $this->send($client, "550 IP address blocked");
                $this->log("IP被阻止: {$clientIp}", $clientIp);
                return;
            }
            
            $this->send($client, "250 OK - Hello");
            
            // 4. 问：谁寄的？
            $from = $this->waitForCommand($client, 'MAIL FROM:', '发件人邮箱');
            
            // 检查发件人邮箱过滤规则
            if ($this->filterRepo->isEmailBlocked($from)) {
                $this->send($client, "550 Sender email blocked");
                $this->log("发件人邮箱被阻止: {$from}", $clientIp);
                return;
            }
            
            $this->send($client, "250 Sender OK");
            
            // 5. 支持多收件人
            $recipients = [];
            $this->send($client, "250 Recipient OK");
            
            // 循环接收多个RCPT TO命令
            while (true) {
                $input = socket_read($client, 1024);
                if ($input === false) break;
                
                $input = trim($input);
                //echo "客户端: {$input}\n";
                
                if (stripos($input, 'RCPT TO:') === 0) {
                    // 提取收件人邮箱
                    if (preg_match('/<(.+?)>/', $input, $matches)) {
                        $to = $matches[1];
                        // 新增：收件人过滤
                        if ($this->filterRepo->isEmailBlocked($to)) {
                            $this->send($client, "550 Recipient email blocked");
                            $this->log("收件人邮箱被过滤: {$to}", $this->clientIp);
                            continue;   // 直接拒绝，不加入 $recipients
                        }
                        
                        // 初步检查收件人邮箱大小限制（使用估算值，实际检查在接收邮件内容后）
                        $user = $this->userRepo->findByUsername($to);
                        if ($user) {
                            $usage = $this->mailboxRepo->getUsage($user['id']);
                            $estimatedSize = 50000; // 估算邮件大小50KB
                            
                            if ($usage['used'] + $estimatedSize > $usage['limit']) {
                                $this->send($client, "552 Mailbox full");
                                $this->log("收件人邮箱已满（初步检查）: {$to}", $clientIp);
                                continue;
                            }
                        }
                        
                        $recipients[] = $to;
                        $this->send($client, "250 Recipient OK");
                    }
                } elseif (stripos($input, 'DATA') === 0) {
                    // 收到DATA命令，跳出循环
                    break;
                } elseif (strtoupper($input) === 'QUIT') {
                    $this->send($client, "221 Bye");
                    return;
                } else {
                    $this->send($client, "500 Error: Expected RCPT TO or DATA");
                }
            }
            
            if (empty($recipients)) {
                $this->send($client, "503 No valid recipients");
                $this->log("没有有效收件人", $clientIp);
                return;
            }
            
            // 6. 说：开始写内容吧
            $this->send($client, "354 Start mail input, end with <CRLF>.<CRLF>");
            
            // 7. 接收邮件内容
            $emailContent = $this->receiveEmailContent($client);
            $emailSize = strlen($emailContent);
            
            // 8. 再次检查邮箱大小限制（使用实际邮件大小）
            $validRecipients = [];
            foreach ($recipients as $to) {
                $user = $this->userRepo->findByUsername($to);
                if ($user) {
                    $usage = $this->mailboxRepo->getUsage($user['id']);
                    if ($usage['used'] + $emailSize > $usage['limit']) {
                        $this->log("收件人邮箱已满（实际检查）: {$to}", $clientIp);
                        continue;
                    }
                }
                $validRecipients[] = $to;
            }
            
            if (empty($validRecipients)) {
                $this->send($client, "552 All recipients' mailboxes are full");
                $this->log("所有收件人邮箱已满", $clientIp);
                return;
            }
            
            // 9. 保存到数据库（支持多收件人）
            $successCount = 0;
            foreach ($validRecipients as $to) {
                if ($this->saveEmail($from, $to, $emailContent, $this->clientIp, $emailSize)) {
                    $successCount++;
                }
            }
            
            // 10. 告诉客户：收到了
            if ($successCount > 0) {
                $this->send($client, "250 Mail accepted");
                $this->log("邮件发送成功: {$from} -> " . implode(', ', $validRecipients) . " ({$successCount}个收件人)", $clientIp);
            } else {
                $this->send($client, "550 Mail delivery failed");
                $this->log("邮件发送失败: {$from} -> " . implode(', ', $validRecipients), $clientIp);
            }
            
            // 11. 等客户说再见
            $this->waitForCommand($client, 'QUIT', 'QUIT');
            $this->send($client, "221 Bye");
            
            echo "收到一封邮件：{$from} -> " . implode(', ', $validRecipients) . "\n";
            
        } catch (Exception $e) {
            $this->log("处理客户端错误: " . $e->getMessage(), $clientIp);
            $this->send($client, "500 Internal server error");
        }
    }
    
    private function send($client, $message)
    {
        socket_write($client, $message . "\r\n");
    }
    
    private function waitForCommand($client, $expected, $description)
    {
        while (true) {
            $input = socket_read($client, 1024);
            if ($input === false) break;
            
            $input = trim($input);
            //echo "客户端: {$input}\n";
            
            if (stripos($input, $expected) === 0) {
                // 提取邮箱地址
                if (preg_match('/<(.+?)>/', $input, $matches)) {
                    return $matches[1];
                }
                return $input;
            }
            
            // 如果收到QUIT，直接退出
            if (strtoupper($input) === 'QUIT') {
                $this->send($client, "221 Bye");
                exit(0);
            }
            
            $this->send($client, "500 Error: Expected {$description}");
        }
    }
    
    private function receiveEmailContent($client)
    {
        $content = "";

        $buffer = "";
        
        while (true) {
            $data = socket_read($client, 1024, PHP_BINARY_READ);
            if ($data === false || $data === '') {
                break;
            }
            
            $buffer .= $data;
            
            // 按行处理
            while (($pos = strpos($buffer, "\r\n")) !== false) {
                $line = substr($buffer, 0, $pos);
                $buffer = substr($buffer, $pos + 2);
                
                // 如果遇到单独一行的 '.' 就结束
                if (trim($line) === '.') {
                    return $content;
                }
                
                $content .= $line . "\r\n";
            }
        }
        
        return $content;
    }
    
   private function saveEmail($from, $to, $content, $clientIp = 'unknown', $emailSize = null)
    {
        try {
            // 1. 解析邮件内容（先不转换编码）
            $lines = explode("\r\n", $content);
            $subject = "无主题";
            $body = "";
            $inBody = false;
            
            foreach ($lines as $line) {
                if (!$inBody && stripos($line, 'Subject:') === 0) {
                    $subject = trim(substr($line, 8));
                    // 解码MIME编码的主题
                    $subject = $this->decodeMimeHeader($subject);
                }
                
                if (!$inBody && trim($line) === '') {
                    $inBody = true;
                    continue;
                }
                
                if ($inBody) {
                    $body .= $line . "\n";
                }
            }
            
            // 2. 清理正文
            $body = trim($body);
            
            // 3. 检测当前编码 - 直接假设为UTF-8
            $detectedEncoding = 'UTF-8'; // 直接指定，不检测了
            echo "使用编码: {$detectedEncoding}\n";

            // 4. 验证确实是UTF-8，如果不是就转换
            if (!mb_check_encoding($subject, 'UTF-8')) {
                $subject = mb_convert_encoding($subject, 'UTF-8', 'auto');
            }
            if (!mb_check_encoding($body, 'UTF-8')) {
                $body = mb_convert_encoding($body, 'UTF-8', 'auto');
            }
            
            // 5. 计算邮件大小（如果未提供则计算）
            if ($emailSize === null) {
                $emailSize = strlen($content);
            }
            
            // 6. 获取收件人用户ID
            $user = $this->userRepo->findByUsername($to);
            $recipientId = $user ? $user['id'] : null;
            
            // 7. 使用参数绑定，确保UTF-8传输
            $stmt = $this->db->prepare(
                "INSERT INTO emails (sender, recipient, recipient_id, subject, body, size_bytes) VALUES (?, ?, ?, ?, ?, ?)"
            );
            
            // 绑定参数时指定字符集
            $stmt->bindValue(1, $from, PDO::PARAM_STR);
            $stmt->bindValue(2, $to, PDO::PARAM_STR);
            $stmt->bindValue(3, $recipientId, PDO::PARAM_INT);
            $stmt->bindValue(4, $subject, PDO::PARAM_STR);
            $stmt->bindValue(5, $body, PDO::PARAM_STR);
            $stmt->bindValue(6, $emailSize, PDO::PARAM_INT);
            
            $stmt->execute();
            
            echo "邮件保存成功：{$from} -> {$to}\n";
            echo "   主题: {$subject}\n";
            echo "   长度: {$emailSize} 字节\n";
            
            return true;
            
        } catch (Exception $e) {
            echo "保存邮件失败: " . $e->getMessage() . "\n";
            error_log("邮件保存错误: " . $e->getMessage() . "\n" . $e->getTraceAsString());
            $this->log("保存邮件失败: " . $e->getMessage(), $clientIp);
            return false;
        }
    }
    
    /**
     * 记录日志到数据库
     */
    private function log($message, $clientIp = 'unknown', $userId = null)
    {
        try {
            $stmt = $this->db->prepare(
                "INSERT INTO server_logs (log_type, message, client_ip, user_id) VALUES (?, ?, ?, ?)"
            );
            $stmt->execute(['SMTP', $message, $clientIp, $userId]);
        } catch (Exception $e) {
            // 日志记录失败不影响主流程
            error_log("日志记录失败: " . $e->getMessage());
        }
    }

    // 添加MIME头解码方法
    private function decodeMimeHeader($header)
    {
         // 处理 =?UTF-8?B?5L2g5aW9?= 这样的MIME编码
        $decoded = '';
        $parts = preg_split('/(=\?[^?]+\?[BQ]\?[^?]+\?=)/i', $header, -1, PREG_SPLIT_DELIM_CAPTURE);
        
        foreach ($parts as $part) {
            if (preg_match('/=\?([^\?]+)\?([BQ])\?([^\?]+)\?=/i', $part, $matches)) {
                $charset = $matches[1];
                $encoding = strtoupper($matches[2]);
                $text = $matches[3];
                
                if ($encoding === 'B') {
                    // Base64解码
                    $decodedText = base64_decode($text);
                } elseif ($encoding === 'Q') {
                    // Quoted-Printable解码
                    $decodedText = quoted_printable_decode(str_replace('_', ' ', $text));
                }
                
                if (isset($decodedText)) {
                    $decoded .= mb_convert_encoding($decodedText, 'UTF-8', $charset);
                }
            } else {
                $decoded .= $part;
            }
        }
        
        return $decoded ?: $header;
    }
    
    public function stop()
    {
        $this->isRunning = false;
    }
}

// 如果直接运行这个文件
if (basename(__FILE__) == basename($_SERVER['PHP_SELF'])) {
    // 检查是否有权限监听端口（需要sudo）
    if (posix_getuid() != 0) {
        echo "注意：需要sudo权限监听该端口\n";
        echo "请运行：sudo php " . __FILE__ . "\n";
        exit(1);
    }
    
    $server = new SimpleSmtpServer('0.0.0.0',$smtpPort);
    $server->start();
}
?>