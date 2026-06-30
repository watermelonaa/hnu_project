<?php
require_once __DIR__ . '/Database.php';

/**
 * 过滤规则数据访问层
 */
class FilterRepository {
    private $db;
    
    public function __construct() {
        $this->db = Database::getInstance();
    }
    
    /**
     * 创建过滤规则
     * @param string $type 规则类型 ('email' 或 'ip')
     * @param string $value 规则值
     * @param string $action 动作 ('block' 或 'allow')
     * @param string $description 描述
     * @return bool 是否成功
     */
    public function create($type, $value, $action = 'block', $description = '') {
        $stmt = $this->db->prepare("
            INSERT INTO filter_rules (rule_type, rule_value, action, description, is_active)
            VALUES (?, ?, ?, ?, 1)
        ");
        return $stmt->execute([$type, $value, $action, $description]);
    }
    
    /**
     * 获取所有过滤规则
     * @return array 规则列表
     */
    public function getAll() {
        $stmt = $this->db->query("
            SELECT * FROM filter_rules 
            ORDER BY rule_type, created_at DESC
        ");
        return $stmt->fetchAll();
    }
    
    /**
     * 获取激活的过滤规则
     * @return array 规则列表
     */
    public function getActive() {
        $stmt = $this->db->query("
            SELECT * FROM filter_rules 
            WHERE is_active = 1
            ORDER BY rule_type, created_at DESC
        ");
        return $stmt->fetchAll();
    }
    
    /**
     * 检查邮箱是否被过滤
     * @param string $email 邮箱地址
     * @return bool 是否被阻止
     */
    public function isEmailBlocked($email) {
        $stmt = $this->db->prepare("
            SELECT action FROM filter_rules 
            WHERE rule_type = 'email' 
            AND is_active = 1 
            AND (rule_value = ? OR rule_value LIKE ?)
            ORDER BY action DESC
            LIMIT 1
        ");
        $stmt->execute([$email, $email]);
        $result = $stmt->fetch();
        return $result && $result['action'] === 'block';
    }
    
    /**
     * 检查IP是否被过滤
     * @param string $ip IP地址
     * @return bool 是否被阻止
     */
    public function isIPBlocked($ip) {
        $stmt = $this->db->prepare("
            SELECT action FROM filter_rules 
            WHERE rule_type = 'ip' 
            AND is_active = 1 
            AND rule_value = ?
            ORDER BY action DESC
            LIMIT 1
        ");
        $stmt->execute([$ip]);
        $result = $stmt->fetch();
        return $result && $result['action'] === 'block';
    }
    
    /**
     * 删除规则
     * @param int $id 规则ID
     * @return bool 是否成功
     */
    public function delete($id) {
        $stmt = $this->db->prepare("DELETE FROM filter_rules WHERE id = ?");
        return $stmt->execute([$id]);
    }
    
    /**
     * 更新规则状态
     * @param int $id 规则ID
     * @param bool $isActive 是否激活
     * @return bool 是否成功
     */
    public function updateStatus($id, $isActive) {
        $stmt = $this->db->prepare("UPDATE filter_rules SET is_active = ? WHERE id = ?");
        return $stmt->execute([$isActive ? 1 : 0, $id]);
        $this->db->commit();  
    }

    public function getById(int $id): ?array
    {
        $stmt = $this->db->prepare("SELECT * FROM filter_rules WHERE id = ? LIMIT 1");
        $stmt->execute([$id]);
        return $stmt->fetch() ?: null;
    }
}