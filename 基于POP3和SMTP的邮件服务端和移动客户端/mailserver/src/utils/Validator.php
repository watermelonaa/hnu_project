<?php
/**
 * 输入验证工具类
 * 检查输入是否合法
 */

class Validator {
    /**
     * 验证邮箱格式
     * @param string $email 邮箱地址
     * @return bool 是否有效
     */
    public static function validateEmail($email) {
        return filter_var($email, FILTER_VALIDATE_EMAIL) !== false;
    }
    
    /**
     * 验证邮箱域名（检查是否属于指定域名）
     * @param string $email 邮箱地址
     * @param string $domain 允许的域名（如：test.com）
     * @return bool 是否属于指定域名
     */
    public static function validateEmailDomain($email, $domain = 'test.com') {
        if (!self::validateEmail($email)) {
            return false;
        }
        
        $emailDomain = substr(strrchr($email, "@"), 1);
        return strtolower($emailDomain) === strtolower($domain);
    }
    
    /**
     * 验证密码强度
     * @param string $password 密码
     * @param int $minLength 最小长度
     * @return array ['valid' => bool, 'errors' => array] 验证结果和错误信息
     */
    public static function validatePassword($password, $minLength = 6) {
        $errors = [];
        
        if (strlen($password) < $minLength) {
            $errors[] = "密码长度至少需要 {$minLength} 个字符";
        }
        
        if (preg_match('/^[a-zA-Z0-9]+$/', $password) && strlen($password) < 8) {
            // 如果密码只包含字母和数字，且长度小于8，建议使用更复杂的密码
            // 但不强制要求
        }
        
        return [
            'valid' => empty($errors),
            'errors' => $errors
        ];
    }
    
    /**
     * 验证用户名格式
     * @param string $username 用户名（邮箱格式）
     * @return array ['valid' => bool, 'errors' => array] 验证结果和错误信息
     */
    public static function validateUsername($username) {
        $errors = [];
        
        if (empty($username)) {
            $errors[] = "用户名不能为空";
        } elseif (!self::validateEmail($username)) {
            $errors[] = "用户名必须是有效的邮箱格式";
        }
        
        return [
            'valid' => empty($errors),
            'errors' => $errors
        ];
    }
    
    /**
     * 验证IP地址格式
     * @param string $ip IP地址
     * @return bool 是否有效
     */
    public static function validateIP($ip) {
        return filter_var($ip, FILTER_VALIDATE_IP) !== false;
    }
    
    /**
     * 验证端口号
     * @param int $port 端口号
     * @return bool 是否有效（1-65535）
     */
    public static function validatePort($port) {
        return is_numeric($port) && $port >= 1 && $port <= 65535;
    }
    
    /**
     * 验证非空字符串
     * @param string $value 待验证的值
     * @param string $fieldName 字段名称（用于错误提示）
     * @return array ['valid' => bool, 'errors' => array] 验证结果和错误信息
     */
    public static function validateRequired($value, $fieldName = '字段') {
        $errors = [];
        
        if (empty(trim($value))) {
            $errors[] = "{$fieldName}不能为空";
        }
        
        return [
            'valid' => empty($errors),
            'errors' => $errors
        ];
    }
    
    /**
     * 验证字符串长度
     * @param string $value 待验证的值
     * @param int $min 最小长度
     * @param int $max 最大长度
     * @param string $fieldName 字段名称
     * @return array ['valid' => bool, 'errors' => array] 验证结果和错误信息
     */
    public static function validateLength($value, $min, $max, $fieldName = '字段') {
        $errors = [];
        $length = mb_strlen($value, 'UTF-8');
        
        if ($length < $min) {
            $errors[] = "{$fieldName}长度不能少于 {$min} 个字符";
        }
        
        if ($length > $max) {
            $errors[] = "{$fieldName}长度不能超过 {$max} 个字符";
        }
        
        return [
            'valid' => empty($errors),
            'errors' => $errors
        ];
    }
    
    /**
     * 验证两个密码是否匹配
     * @param string $password 密码
     * @param string $confirmPassword 确认密码
     * @return array ['valid' => bool, 'errors' => array] 验证结果和错误信息
     */
    public static function validatePasswordMatch($password, $confirmPassword) {
        $errors = [];
        
        if ($password !== $confirmPassword) {
            $errors[] = "两次输入的密码不一致";
        }
        
        return [
            'valid' => empty($errors),
            'errors' => $errors
        ];
    }
}
