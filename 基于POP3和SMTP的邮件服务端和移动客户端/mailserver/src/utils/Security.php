<?php
/**
 * 安全工具类
 * 提供密码加密、防攻击等功能
 */

class Security {
    /**
     * 加密密码
     * @param string $password 明文密码
     * @return string 加密后的密码哈希
     */
    public static function hashPassword($password) {
        return password_hash($password, PASSWORD_BCRYPT, ['cost' => 10]);
    }
    
    /**
     * 验证密码
     * @param string $password 明文密码
     * @param string $hash 密码哈希
     * @return bool 是否匹配
     */
    public static function verifyPassword($password, $hash) {
        return password_verify($password, $hash);
    }
    
    /**
     * 清理输入，防止XSS攻击
     * @param string $input 用户输入
     * @return string 清理后的字符串
     */
    public static function sanitizeInput($input) {
        return htmlspecialchars(strip_tags(trim($input)), ENT_QUOTES, 'UTF-8');
    }
    
    /**
     * 生成CSRF令牌
     * @return string CSRF令牌
     */
    public static function generateCSRFToken() {
        if (!isset($_SESSION['csrf_token'])) {
            $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
        }
        return $_SESSION['csrf_token'];
    }
    
    /**
     * 验证CSRF令牌
     * @param string $token 待验证的令牌
     * @return bool 是否有效
     */
    public static function verifyCSRFToken($token) {
        return isset($_SESSION['csrf_token']) && hash_equals($_SESSION['csrf_token'], $token);
    }
    
    /**
     * 获取客户端IP地址
     * @return string IP地址
     */
    public static function getClientIP() {
        $ipKeys = ['HTTP_CLIENT_IP', 'HTTP_X_FORWARDED_FOR', 'REMOTE_ADDR'];
        foreach ($ipKeys as $key) {
            if (array_key_exists($key, $_SERVER) === true) {
                foreach (explode(',', $_SERVER[$key]) as $ip) {
                    $ip = trim($ip);
                    if (filter_var($ip, FILTER_VALIDATE_IP, FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE) !== false) {
                        return $ip;
                    }
                }
            }
        }
        return $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
    }
    
    /**
     * 防止暴力破解：检查登录尝试次数
     * @param string $username 用户名
     * @param int $maxAttempts 最大尝试次数
     * @param int $lockoutTime 锁定时间（秒）
     * @return bool 是否允许登录
     */
    public static function checkLoginAttempts($username, $maxAttempts = 5, $lockoutTime = 300) {
        $key = 'login_attempts_' . md5($username);
        
        if (!isset($_SESSION[$key])) {
            $_SESSION[$key] = ['count' => 0, 'time' => time()];
            return true;
        }
        
        $attempts = $_SESSION[$key];
        
        // 如果超过锁定时间，重置计数
        if (time() - $attempts['time'] > $lockoutTime) {
            $_SESSION[$key] = ['count' => 0, 'time' => time()];
            return true;
        }
        
        // 检查是否超过最大尝试次数
        if ($attempts['count'] >= $maxAttempts) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 记录登录失败尝试
     * @param string $username 用户名
     */
    public static function recordLoginAttempt($username) {
        $key = 'login_attempts_' . md5($username);
        
        if (!isset($_SESSION[$key])) {
            $_SESSION[$key] = ['count' => 1, 'time' => time()];
        } else {
            $_SESSION[$key]['count']++;
            $_SESSION[$key]['time'] = time();
        }
    }
    
    /**
     * 清除登录尝试记录
     * @param string $username 用户名
     */
    public static function clearLoginAttempts($username) {
        $key = 'login_attempts_' . md5($username);
        unset($_SESSION[$key]);
    }
}
