<?php
require_once __DIR__ . '/Database.php';

/**
 * 邮件数据访问层
 * 信件管理员 - 存信、取信
 */
class EmailRepository {
    private $db;
    
    public function __construct() {
        $this->db = Database::getInstance();
    }
    
    /**
     * 根据ID查找邮件
     * @param int $id 邮件ID
     * @return array|null 邮件信息或null
     */
    public function findById($id) {
        $stmt = $this->db->prepare("
            SELECT e.*, 
                   u1.username as sender_username,
                   u2.username as recipient_username
            FROM emails e
            LEFT JOIN users u1 ON e.sender_id = u1.id
            LEFT JOIN users u2 ON e.recipient_id = u2.id
            WHERE e.id = ? AND e.is_deleted = 0
        ");
        $stmt->execute([$id]);
        return $stmt->fetch();
    }
    
    /**
     * 获取用户的收件箱邮件
     * @param int $userId 用户ID
     * @param int $limit 限制数量
     * @param int $offset 偏移量
     * @return array 邮件列表
     */
    public function getInbox($userId, $limit = null, $offset = 0) {
        $sql = "
            SELECT e.*, 
                   COALESCE(u1.username, e.sender) as sender_name,
                   COALESCE(u2.username, e.recipient) as recipient_name
            FROM emails e
            LEFT JOIN users u1 ON e.sender_id = u1.id
            LEFT JOIN users u2 ON e.recipient_id = u2.id
            WHERE (e.recipient_id = ? OR e.recipient = (SELECT username FROM users WHERE id = ?))
            AND e.is_deleted = 0
            ORDER BY e.created_at DESC
        ";
        
        if ($limit !== null) {
            $sql .= " LIMIT ? OFFSET ?";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([$userId, $userId, $limit, $offset]);
        } else {
            $stmt = $this->db->prepare($sql);
            $stmt->execute([$userId, $userId]);
        }
        
        return $stmt->fetchAll();
    }
    
    /**
     * 获取用户的发件箱邮件
     * @param int $userId 用户ID
     * @param int $limit 限制数量
     * @param int $offset 偏移量
     * @return array 邮件列表
     */
    public function getSent($userId, $limit = null, $offset = 0) {
        $sql = "
            SELECT e.*, 
                   COALESCE(u1.username, e.sender) as sender_name,
                   COALESCE(u2.username, e.recipient) as recipient_name
            FROM emails e
            LEFT JOIN users u1 ON e.sender_id = u1.id
            LEFT JOIN users u2 ON e.recipient_id = u2.id
            WHERE (e.sender_id = ? OR e.sender = (SELECT username FROM users WHERE id = ?))
            AND e.is_deleted = 0
            ORDER BY e.created_at DESC
        ";
        
        if ($limit !== null) {
            $sql .= " LIMIT ? OFFSET ?";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([$userId, $userId, $limit, $offset]);
        } else {
            $stmt = $this->db->prepare($sql);
            $stmt->execute([$userId, $userId]);
        }
        
        return $stmt->fetchAll();
    }
    
    /**
     * 获取所有邮件（管理员功能）
     * @param int $limit 限制数量
     * @param int $offset 偏移量
     * @return array 邮件列表
     */
    public function getAll($limit = null, $offset = 0) {
        $sql = "
            SELECT e.*, 
                   COALESCE(u1.username, e.sender) as sender_name,
                   COALESCE(u2.username, e.recipient) as recipient_name
            FROM emails e
            LEFT JOIN users u1 ON e.sender_id = u1.id
            LEFT JOIN users u2 ON e.recipient_id = u2.id
            WHERE e.is_deleted = 0
            ORDER BY e.created_at DESC
        ";
        
        if ($limit !== null) {
            $sql .= " LIMIT ? OFFSET ?";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([$limit, $offset]);
        } else {
            $stmt = $this->db->query($sql);
        }
        
        return $stmt->fetchAll();
    }
    
    /**
     * 获取邮件总数
     * @param int|null $userId 用户ID（如果提供，只统计该用户的邮件）
     * @return int 邮件总数
     */
    public function getCount($userId = null) {
        if ($userId !== null) {
            $stmt = $this->db->prepare("
                SELECT COUNT(*) as count FROM emails 
                WHERE (recipient_id = ? OR sender_id = ?) AND is_deleted = 0
            ");
            $stmt->execute([$userId, $userId]);
        } else {
            $stmt = $this->db->query("SELECT COUNT(*) as count FROM emails WHERE is_deleted = 0");
        }
        
        $result = $stmt->fetch();
        return (int)$result['count'];
    }
    
    /**
     * 标记邮件为已读
     * @param int $id 邮件ID
     * @return bool 是否成功
     */
    public function markAsRead($id) {
        $stmt = $this->db->prepare("UPDATE emails SET is_read = 1 WHERE id = ?");
        return $stmt->execute([$id]);
    }
    
    /**
     * 删除邮件（软删除）
     * @param int $id 邮件ID
     * @return bool 是否成功
     */
    public function delete($id) {
        $stmt = $this->db->prepare("UPDATE emails SET is_deleted = 1 WHERE id = ?");
        return $stmt->execute([$id]);
    }
    
    /**
     * 永久删除邮件
     * @param int $id 邮件ID
     * @return bool 是否成功
     */
    public function permanentDelete($id) {
        $stmt = $this->db->prepare("DELETE FROM emails WHERE id = ?");
        return $stmt->execute([$id]);
    }
}
