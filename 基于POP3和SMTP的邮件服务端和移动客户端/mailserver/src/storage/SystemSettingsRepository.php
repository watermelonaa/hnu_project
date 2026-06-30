<?php
require_once __DIR__ . '/Database.php';

/**
 * 系统设置数据访问层
 */
class SystemSettingsRepository {
    private $db;
    
    public function __construct() {
        $this->db = Database::getInstance();
    }
    
    /**
     * 获取设置值
     * @param string $key 设置键
     * @param mixed $default 默认值
     * @return mixed 设置值
     */
    public function get($key, $default = null) {
        $stmt = $this->db->prepare("SELECT setting_value FROM system_settings WHERE setting_key = ?");
        $stmt->execute([$key]);
        $result = $stmt->fetch();
        return $result ? $result['setting_value'] : $default;
    }
    
    /**
     * 设置值
     * @param string $key 设置键
     * @param mixed $value 设置值
     * @return bool 是否成功
     */
    public function set($key, $value) {
        $stmt = $this->db->prepare("
            INSERT INTO system_settings (setting_key, setting_value) 
            VALUES (?, ?) 
            ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)
        ");
        return $stmt->execute([$key, $value]);
    }
    
    /**
     * 获取所有设置
     * @return array 所有设置
     */
    public function getAll() {
        $stmt = $this->db->query("SELECT setting_key, setting_value FROM system_settings");
        $results = $stmt->fetchAll();
        $settings = [];
        foreach ($results as $row) {
            $settings[$row['setting_key']] = $row['setting_value'];
        }
        return $settings;
    }
}

