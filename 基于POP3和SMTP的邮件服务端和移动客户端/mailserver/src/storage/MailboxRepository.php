<?php
require_once __DIR__ . '/Database.php';

/**
 * 邮箱管理数据访问层
 * 
 * @package storage
 */
class MailboxRepository {
    private $db;
    
    public function __construct() {
        $this->db = Database::getInstance();
    }
    
    /**
     * 获取用户邮箱大小限制
     * @param int $userId 用户ID
     * @return int 大小限制（字节）
     */
    public function getSizeLimit($userId) {
        $stmt = $this->db->prepare("
            SELECT size_limit_bytes FROM user_mailbox_limits WHERE user_id = ?
        ");
        $stmt->execute([$userId]);
        $result = $stmt->fetch();
        
        if ($result) {
            return (int)$result['size_limit_bytes'];
        }
        
        // 返回默认值
        return 104857600; // 100MB
    }
    
    /**
     * 设置用户邮箱大小限制
     * @param int $userId 用户ID
     * @param int $sizeBytes 大小限制（字节）
     * @return bool 是否成功
     */
    public function setSizeLimit($userId, $sizeBytes) {
        $stmt = $this->db->prepare("
            INSERT INTO user_mailbox_limits (user_id, size_limit_bytes)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE size_limit_bytes = VALUES(size_limit_bytes)
        ");
        return $stmt->execute([$userId, $sizeBytes]);
    }
    
    /**
     * 获取用户当前邮箱使用大小
     * @param int $userId 用户ID
     * @return int 已使用大小（字节）
     */
    public function getUsedSize($userId) {
        $stmt = $this->db->prepare("
            SELECT COALESCE(SUM(size_bytes), 0) as total_size
            FROM emails
            WHERE recipient_id = ? AND is_deleted = 0
        ");
        $stmt->execute([$userId]);
        $result = $stmt->fetch();
        return (int)($result['total_size'] ?? 0);
    }
    
    /**
     * 获取用户邮箱使用情况
     * @param int $userId 用户ID
     * @return array ['limit' => int, 'used' => int, 'percentage' => float]
     */
    public function getUsage($userId) {
        $limit = $this->getSizeLimit($userId);
        $used = $this->getUsedSize($userId);
        $percentage = $limit > 0 ? ($used / $limit) * 100 : 0;
        
        return [
            'limit' => $limit,
            'used' => $used,
            'percentage' => round($percentage, 2)
        ];
    }
}

