<?php
require_once __DIR__ . '/../storage/Database.php';
require_once __DIR__ . '/../storage/UserRepository.php';
require_once __DIR__ . '/../storage/EmailRepository.php';

/**
 * 群发邮件服务
 */
class BroadcastService {
    private $db;
    private $userRepo;
    private $emailRepo;
    
    public function __construct() {
        $this->db = Database::getInstance();
        $this->userRepo = new UserRepository();
        $this->emailRepo = new EmailRepository();
    }
    
    /**
     * 群发邮件给所有用户
     * @param string $senderEmail 发件人邮箱
     * @param string $subject 主题
     * @param string $body 内容
     * @return array ['success' => int, 'failed' => int, 'errors' => array]
     */
    public function broadcastToAll($senderEmail, $subject, $body) {
        $users = $this->userRepo->getAll();
        $sender = $this->userRepo->findByUsername($senderEmail);
        
        if (!$sender) {
            return ['success' => 0, 'failed' => 0, 'errors' => ['发件人不存在']];
        }
        
        $success = 0;
        $failed = 0;
        $errors = [];
        
        foreach ($users as $user) {
            // 跳过发件人自己
            if ($user['username'] === $senderEmail) {
                continue;
            }
            
            // 跳过禁用的用户
            if (!$user['is_active']) {
                continue;
            }
            
            try {
                $this->sendEmail($sender['id'], $user['id'], $subject, $body);
                $success++;
            } catch (Exception $e) {
                $failed++;
                $errors[] = "发送给 {$user['username']} 失败: " . $e->getMessage();
            }
        }
        
        return [
            'success' => $success,
            'failed' => $failed,
            'errors' => $errors
        ];
    }
    
    /**
     * 群发邮件给指定用户列表
     * @param string $senderEmail 发件人邮箱
     * @param array $recipientEmails 收件人邮箱列表
     * @param string $subject 主题
     * @param string $body 内容
     * @return array ['success' => int, 'failed' => int, 'errors' => array]
     */
    public function broadcastToUsers($senderEmail, $recipientEmails, $subject, $body) {
        $sender = $this->userRepo->findByUsername($senderEmail);
        
        if (!$sender) {
            return ['success' => 0, 'failed' => 0, 'errors' => ['发件人不存在']];
        }
        
        $success = 0;
        $failed = 0;
        $errors = [];
        
        foreach ($recipientEmails as $email) {
            $recipient = $this->userRepo->findByUsername(trim($email));
            
            if (!$recipient) {
                $failed++;
                $errors[] = "用户 {$email} 不存在";
                continue;
            }
            
            if (!$recipient['is_active']) {
                $failed++;
                $errors[] = "用户 {$email} 已被禁用";
                continue;
            }
            
            try {
                $this->sendEmail($sender['id'], $recipient['id'], $subject, $body);
                $success++;
            } catch (Exception $e) {
                $failed++;
                $errors[] = "发送给 {$email} 失败: " . $e->getMessage();
            }
        }
        
        return [
            'success' => $success,
            'failed' => $failed,
            'errors' => $errors
        ];
    }
    
    /**
     * 发送邮件
     * @param int $senderId 发件人ID
     * @param int $recipientId 收件人ID
     * @param string $subject 主题
     * @param string $body 内容
     * @return bool 是否成功
     */
    private function sendEmail($senderId, $recipientId, $subject, $body) {
        $sender = $this->userRepo->findById($senderId);
        $recipient = $this->userRepo->findById($recipientId);
        
        if (!$sender || !$recipient) {
            throw new Exception("发件人或收件人不存在");
        }
        
        $sizeBytes = strlen($subject) + strlen($body);
        
        $stmt = $this->db->prepare("
            INSERT INTO emails (sender_id, recipient_id, sender, recipient, subject, body, size_bytes, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
        ");
        
        return $stmt->execute([
            $senderId,
            $recipientId,
            $sender['username'],
            $recipient['username'],
            $subject,
            $body,
            $sizeBytes
        ]);
    }
}

