<?php
require_once __DIR__ . '/Database.php';

/**
 * 服务状态数据访问层
 */
class ServiceRepository {
    private $db;
    
    public function __construct() {
        $this->db = Database::getInstance();
    }
    
    /**
     * 获取服务状态
     * @param string $serviceName 服务名称 ('smtp' 或 'pop3')
     * @return array|null 服务状态或null
     */
    public function getStatus($serviceName) {
        $stmt = $this->db->prepare("SELECT * FROM service_status WHERE service_name = ?");
        $stmt->execute([$serviceName]);
        return $stmt->fetch();
    }
    
    /**
     * 更新服务状态
     * @param string $serviceName 服务名称
     * @param bool $isRunning 是否运行
     * @param int|null $pid 进程ID
     * @return bool 是否成功
     */
    public function updateStatus($serviceName, $isRunning, $pid = null) {
        $stmt = $this->db->prepare("
            INSERT INTO service_status (service_name, is_running, pid, last_started_at, last_stopped_at)
            VALUES (?, ?, ?, NOW(), NULL)
            ON DUPLICATE KEY UPDATE 
                is_running = VALUES(is_running),
                pid = VALUES(pid),
                last_started_at = IF(VALUES(is_running) = 1, NOW(), last_started_at),
                last_stopped_at = IF(VALUES(is_running) = 0, NOW(), last_stopped_at)
        ");
        return $stmt->execute([$serviceName, $isRunning ? 1 : 0, $pid]);
    }
    
    /**
     * 获取所有服务状态
     * @return array 服务状态列表
     */
    public function getAllStatus() {
        $stmt = $this->db->query("SELECT * FROM service_status ORDER BY service_name");
        return $stmt->fetchAll();
    }
    
    /**
     * 检查服务是否运行
     * @param string $serviceName 服务名称
     * @return bool 是否运行
     */
    public function isRunning($serviceName) {
        $status = $this->getStatus($serviceName);
        if (!$status) {
            return false;
        }
        
        // 检查进程是否真的在运行
        if ($status['pid'] && $status['is_running']) {
            // 检查进程是否存在
            $result = shell_exec("ps -p {$status['pid']} -o pid= 2>/dev/null");
            if (empty(trim($result))) {
                // 进程不存在，更新状态
                $this->updateStatus($serviceName, false, null);
                return false;
            }
        }
        
        return (bool)$status['is_running'];
    }
}

