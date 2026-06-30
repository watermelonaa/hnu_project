<?php
/**
 * 数据库配置文件
 * 使用环境变量或默认配置
 */
require_once __DIR__ . '/../../config/database.php';

class Database {
    private static $instance = null;
    private $connection;
    
    private function __construct() {
        $config = require __DIR__ . '/../../config/database.php'; 
        //自动每一条SQL提交一次
        // $config['options'][PDO::ATTR_AUTOCOMMIT] = true;
        $dsn = "mysql:host={$config['host']};port={$config['port']};dbname={$config['database']};charset={$config['charset']}";
        
        try {
            $this->connection = new PDO(
                $dsn,
                $config['username'],
                $config['password'],
                $config['options']
            );
        } catch (PDOException $e) {
            throw new Exception("数据库连接失败: " . $e->getMessage());
        }
    }
    
    public static function getInstance() {
        if (self::$instance === null) {
            self::$instance = new self();
        }
        return self::$instance->connection;
    }
}