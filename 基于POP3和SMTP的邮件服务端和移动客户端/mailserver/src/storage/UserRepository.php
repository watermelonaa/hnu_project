<?php
require_once __DIR__ . '/Database.php';
require_once __DIR__ . '/../utils/Security.php';
/**
 * 用户数据访问层
 * 查客户信息、创建用户等
 */

class UserRepository {
    private $db;
    
    public function __construct() {
        $this->db = Database::getInstance();
    }
    
    /**
     * 根据用户名查找用户
     * @param string $username 用户名（邮箱）
     * @return array|null 用户信息或null
     */
    public function findByUsername($username) {
        $stmt = $this->db->prepare("SELECT * FROM users WHERE username = ?");
        $stmt->execute([$username]);
        return $stmt->fetch();
    }
    
    /**
     * 根据ID查找用户
     * @param int $id 用户ID
     * @return array|null 用户信息或null
     */
    public function findById($id) {
        $stmt = $this->db->prepare("SELECT * FROM users WHERE id = ?");
        $stmt->execute([$id]);
        return $stmt->fetch();
    }
    
    /**
     * 检查用户名是否已存在
     * @param string $username 用户名
     * @return bool 是否存在
     */
    public function usernameExists($username) {
        $user = $this->findByUsername($username);
        return $user !== false;
    }
    
    /**
     * 创建新用户
     * @param string $username 用户名（邮箱）
     * @param string $password 明文密码
     * @param bool $isAdmin 是否为管理员
     * @param bool $isActive 是否激活
     * @return array 创建的用户信息
     * @throws Exception 如果创建失败
     */
    public function create($username, $password, $isAdmin = false, $isActive = true) {
        // 检查用户名是否已存在
        if ($this->usernameExists($username)) {
            throw new Exception("用户名已存在");
        }
        
        // 加密密码
        $passwordHash = Security::hashPassword($password);
        
        // 插入数据库
        $stmt = $this->db->prepare("
            INSERT INTO users (username, password_hash, is_admin, is_active, created_at)
            VALUES (?, ?, ?, ?, NOW())
        ");
        
        $stmt->execute([$username, $passwordHash, $isAdmin ? 1 : 0, $isActive ? 1 : 0]);
        
        // 返回创建的用户信息
        $userId = $this->db->lastInsertId();
        return $this->findById($userId);
    }
    
    /**
     * 更新用户信息
     * @param int $id 用户ID
     * @param array $data 要更新的数据 ['password' => string, 'is_admin' => bool, 'is_active' => bool]
     * @return bool 是否成功
     */
    public function update($id, $data) {
        $updates = [];
        $params = [];
        
        if (isset($data['password'])) {
            $updates[] = "password_hash = ?";
            $params[] = Security::hashPassword($data['password']);
        }
        
        if (isset($data['is_admin'])) {
            $updates[] = "is_admin = ?";
            $params[] = $data['is_admin'] ? 1 : 0;
        }
        
        if (isset($data['is_active'])) {
            $updates[] = "is_active = ?";
            $params[] = $data['is_active'] ? 1 : 0;
        }
        
        if (empty($updates)) {
            return false;
        }
        
        $params[] = $id;
        $sql = "UPDATE users SET " . implode(", ", $updates) . " WHERE id = ?";
        $stmt = $this->db->prepare($sql);
        return $stmt->execute($params);
    }
    
    /**
     * 删除用户
     * @param int $id 用户ID
     * @return bool 是否成功
     */
    public function delete($id) {
        $stmt = $this->db->prepare("DELETE FROM users WHERE id = ?");
        return $stmt->execute([$id]);
    }
    
    /**
     * 获取所有用户列表
     * @param int $limit 限制数量
     * @param int $offset 偏移量
     * @return array 用户列表
     */
    public function getAll($limit = null, $offset = 0) {
        $sql = "SELECT id, username, is_admin, is_active, created_at FROM users ORDER BY created_at DESC";
        
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
     * 获取用户总数
     * @return int 用户总数
     */
    public function getCount() {
        $stmt = $this->db->query("SELECT COUNT(*) as count FROM users");
        $result = $stmt->fetch();
        return (int)$result['count'];
    }
    
    /**
     * 验证用户密码
     * @param string $username 用户名
     * @param string $password 明文密码
     * @return array|null 用户信息或null（如果验证失败）
     */
    public function verifyPassword($username, $password) {
        $user = $this->findByUsername($username);
        
        if (!$user) {
            return null;
        }
        
        if (!Security::verifyPassword($password, $user['password_hash'])) {
            return null;
        }
        
        return $user;
    }
}
