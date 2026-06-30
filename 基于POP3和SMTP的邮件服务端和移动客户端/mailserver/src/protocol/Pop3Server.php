<?php
/**
 * 最简POP3服务器 - 负责取信
 * 运行：php src/Pop3Server.php
 */

require_once __DIR__ . '/../storage/MailboxRepository.php';
require_once __DIR__ . '/../storage/UserRepository.php';
require_once __DIR__ . '/../storage/FilterRepository.php';
require_once __DIR__ . '/../storage/SystemSettingsRepository.php';

$settingsRepo = new SystemSettingsRepository();
// 获取端口设置（放在函数定义之前）
$smtpPort = $settingsRepo->get('smtp_port', 25);
$pop3Port = $settingsRepo->get('pop3_port', 110);

class SimplePop3Server
{
    private $socket;
    private $isRunning = false;
    private $db;
    private $currentUser = null;
    private $currentUserId = null;
    private $userEmails = [];
    private $deletedEmails = []; // 存储待删除的邮件ID
    
    // 新增：存储主机和端口
    private $host;
    private $port;

    public function __construct($host = '0.0.0.0', $port = 110)
    {
        echo "POP3服务器启动在 {$host}:{$port}\n";
        echo "按 Ctrl+C 停止\n\n";

        $this->host = $host;
        $this->port = $port;

        $this->connectDB();
    }
    
    private function connectDB()
    {
        try {
            $this->db = new PDO(
                'mysql:host=127.0.0.1;port=3308;dbname=mail_server',
                'mail_user',
                'user123'
            );
            $this->db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            echo "数据库连接成功\n";
        } catch (PDOException $e) {
            echo "数据库连接失败: " . $e->getMessage() . "\n";
            exit(1);
        }
    }
    
    public function start()
    {
        $this->socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
        socket_set_option($this->socket, SOL_SOCKET, SO_REUSEADDR, 1);
        socket_bind($this->socket,$this->host, $this->port);
        socket_listen($this->socket, 5);
        
        $this->isRunning = true;
        
        while ($this->isRunning) {
            $client = socket_accept($this->socket);
            if ($client !== false) {
                $this->handleClient($client);
                socket_close($client);
                $this->currentUser = null; // 重置用户状态
            }
        }
    }
    
    private function handleClient($client)
    {
        // 获取客户端IP地址
        socket_getpeername($client, $clientIp);
        $clientIp = $clientIp ?: 'unknown';
        
        // 记录连接日志
        $this->log("客户端连接", $clientIp);
        
        $this->send($client, "+OK POP3 Simple Server Ready");
        
        $state = 'AUTH'; // 状态：AUTH -> TRANSACTION -> UPDATE
        $this->deletedEmails = []; // 重置删除列表
        
        try {
            while (true) {
                $input = socket_read($client, 1024);
                if ($input === false || trim($input) === '') {
                    break;
                }
                
                $input_trimmed = trim($input);
                $command = strtoupper($input_trimmed);
                //echo "客户端: {$input_trimmed}\n";
                
                if ($state === 'AUTH') {
                    // 认证阶段
                    if (strpos($command, 'USER ') === 0) {
                        $username = trim(substr($input_trimmed, 5)); // 使用原始输入，保持大小写
                        if ($this->userExists($username)) {
                            $this->currentUser = $username;
                            $this->send($client, "+OK User found");
                            $this->log("用户认证: USER {$username}", $clientIp);
                        } else {
                            $this->send($client, "-ERR User not found");
                            $this->log("用户不存在: {$username}", $clientIp);
                        }
                    } elseif (strpos($command, 'PASS ') === 0) {
                        if ($this->currentUser) {
                            // 提取密码
                            $password = substr($input, 5); // 保留原始大小写
                            $password = trim($password);
                            
                            // 验证密码
                            if ($this->verifyPassword($this->currentUser, $password)) {
                                $this->loadUserEmails();
                                $this->send($client, "+OK Logged in, {$this->userEmails['count']} messages");
                                $state = 'TRANSACTION';
                                $this->log("登录成功: {$this->currentUser}", $clientIp, $this->currentUserId);
                            } else {
                                $this->send($client, "-ERR Invalid password");
                                $this->currentUser = null; // 重置用户状态
                                $this->log("密码错误: {$this->currentUser}", $clientIp);
                            }
                        } else {
                            $this->send($client, "-ERR USER first");
                        }
                    } elseif ($command === 'QUIT') {
                        $this->send($client, "+OK Bye");
                        $this->log("客户端断开连接", $clientIp);
                        break;
                    }
                } elseif ($state === 'TRANSACTION') {
                    // 事务阶段
                    if ($command === 'STAT') {
                        // 统计时排除已标记删除的邮件
                        $activeCount = $this->userEmails['count'] - count($this->deletedEmails);
                        $activeSize = $this->userEmails['total_size'];
                        foreach ($this->deletedEmails as $deletedId) {
                            if (isset($this->userEmails['emails'][$deletedId])) {
                                $activeSize -= $this->userEmails['emails'][$deletedId]['size'];
                            }
                        }
                        $this->send($client, "+OK {$activeCount} {$activeSize}");
                    } elseif ($command === 'LIST') {
                        $activeCount = $this->userEmails['count'] - count($this->deletedEmails);
                        $response = "+OK {$activeCount} messages\n";
                        foreach ($this->userEmails['emails'] as $id => $email) {
                            // 跳过已标记删除的邮件
                            if (!in_array($id, $this->deletedEmails)) {
                                $response .= "{$id} {$email['size']}\n";
                            }
                        }
                        $response .= ".";
                        $this->send($client, $response);
                    } elseif (strpos($command, 'RETR ') === 0) {
                        $msgId = intval(substr($command, 5));
                        if (isset($this->userEmails['emails'][$msgId]) && !in_array($msgId, $this->deletedEmails)) {
                            $email = $this->userEmails['emails'][$msgId];
                            $response = "+OK {$email['size']} octets\n";
                            $response .= "From: {$email['sender']}\n";
                            $response .= "To: {$email['recipient']}\n";
                            $response .= "Subject: {$email['subject']}\n";
                            $response .= "Date: {$email['date']}\n\n";
                            $response .= $email['body'] . "\n.";
                            $this->send($client, $response);
                            $this->log("读取邮件: ID {$msgId}", $clientIp, $this->currentUserId);
                        } else {
                            $this->send($client, "-ERR No such message");
                        }
                    } elseif (strpos($command, 'DELE ') === 0) {
                        // 删除邮件命令
                        $msgId = intval(substr($command, 5));
                        if (isset($this->userEmails['emails'][$msgId]) && !in_array($msgId, $this->deletedEmails)) {
                            $this->deletedEmails[] = $msgId;
                            $this->send($client, "+OK Message {$msgId} deleted");
                            $this->log("标记删除邮件: ID {$msgId}", $clientIp, $this->currentUserId);
                        } else {
                            $this->send($client, "-ERR No such message");
                        }
                    } elseif ($command === 'QUIT') {
                        // 在QUIT时执行实际删除
                        $this->processDeletions();
                        $this->send($client, "+OK Bye");
                        $this->log("客户端断开连接", $clientIp, $this->currentUserId);
                        $state = 'UPDATE';
                        break;
                    } else {
                        $this->send($client, "-ERR Unknown command");
                    }
                }
            }
        } catch (Exception $e) {
            $this->log("处理客户端错误: " . $e->getMessage(), $clientIp);
        } finally {
            // 重置状态
            $this->currentUser = null;
            $this->currentUserId = null;
            $this->deletedEmails = [];
        }
    }
    
    private function send($client, $message)
    {
        socket_write($client, $message . "\r\n");
        echo "服务器: {$message}\n";
    }
    
    private function userExists($username)
    {
        $stmt = $this->db->prepare("SELECT id FROM users WHERE username = ? AND is_active = 1");
        $stmt->execute([$username]);
        return $stmt->rowCount() > 0;
    }
    
    private function verifyPassword($username, $password)
    {
        try {
            $stmt = $this->db->prepare("SELECT password_hash FROM users WHERE username = ? AND is_active = 1");
            $stmt->execute([$username]);
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
            
            if ($user && isset($user['password_hash'])) {
                return password_verify($password, $user['password_hash']);
            }
            
            return false;
        } catch (Exception $e) {
            echo "密码验证错误: " . $e->getMessage() . "\n";
            return false;
        }
    }
    
    private function loadUserEmails()
    {
        $stmt = $this->db->prepare(
            "SELECT id, sender, recipient, subject, body, created_at, size_bytes
             FROM emails 
             WHERE recipient = ? AND is_deleted = 0
             ORDER BY created_at"
        );
        $stmt->execute([$this->currentUser]);
        
        $this->userEmails = ['count' => 0, 'total_size' => 0, 'emails' => []];
        
        while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
            $id = $row['id'];
            // 使用数据库中的size_bytes，如果没有则估算
            $size = $row['size_bytes'] > 0 ? $row['size_bytes'] : (strlen($row['body']) + 100);
            
            $this->userEmails['emails'][$id] = [
                'sender' => $row['sender'],
                'recipient' => $row['recipient'],
                'subject' => $row['subject'],
                'body' => $row['body'],
                'date' => $row['created_at'],
                'size' => $size
            ];
            
            $this->userEmails['count']++;
            $this->userEmails['total_size'] += $size;
        }
        
        // 获取用户ID
        $userStmt = $this->db->prepare("SELECT id FROM users WHERE username = ?");
        $userStmt->execute([$this->currentUser]);
        $user = $userStmt->fetch();
        $this->currentUserId = $user ? $user['id'] : null;
        
        echo "为用户 {$this->currentUser} 加载了 {$this->userEmails['count']} 封邮件\n";
    }
    
    /**
     * 处理删除操作（在QUIT时执行）
     */
    private function processDeletions()
    {
        if (empty($this->deletedEmails)) {
            return;
        }
        
        try {
            $placeholders = implode(',', array_fill(0, count($this->deletedEmails), '?'));
            $stmt = $this->db->prepare(
                "UPDATE emails SET is_deleted = 1 WHERE id IN ({$placeholders}) AND recipient = ?"
            );
            $params = array_merge($this->deletedEmails, [$this->currentUser]);
            $stmt->execute($params);
            
            $deletedCount = $stmt->rowCount();
            echo "删除了 {$deletedCount} 封邮件\n";
        } catch (Exception $e) {
            echo "删除邮件失败: " . $e->getMessage() . "\n";
            error_log("删除邮件错误: " . $e->getMessage());
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
            $stmt->execute(['POP3', $message, $clientIp, $userId]);
        } catch (Exception $e) {
            // 日志记录失败不影响主流程
            error_log("日志记录失败: " . $e->getMessage());
        }
    }
}

// 如果直接运行这个文件
if (basename(__FILE__) == basename($_SERVER['PHP_SELF'])) {
    $server = new SimplePop3Server('0.0.0.0',$pop3Port);
    $server->start();
}
?>