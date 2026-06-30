<?php
/**
 * 测试用户注册功能
 * 用法: php scripts/test_register.php
 */

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/storage/Database.php';
require_once __DIR__ . '/../src/storage/UserRepository.php';
require_once __DIR__ . '/../src/utils/Validator.php';
require_once __DIR__ . '/../src/utils/Security.php';

echo "=== 用户注册功能测试 ===\n\n";

try {
    $userRepo = new UserRepository();
    
    // 测试1: 验证邮箱格式
    echo "测试1: 验证邮箱格式\n";
    $testEmails = [
        'valid@test.com' => true,
        'invalid-email' => false,
        'test@test.com' => true,
        'user@wrong.com' => false,
    ];
    
    foreach ($testEmails as $email => $expected) {
        $isValid = Validator::validateEmail($email);
        $domainValid = Validator::validateEmailDomain($email, 'test.com');
        $result = $isValid && ($expected ? $domainValid : !$domainValid);
        echo "  {$email}: " . ($result ? "✓" : "✗") . "\n";
    }
    
    // 测试2: 验证密码强度
    echo "\n测试2: 验证密码强度\n";
    $testPasswords = [
        '12345' => false,  // 太短
        '123456' => true,  // 符合最小长度
        'password123' => true,
    ];
    
    foreach ($testPasswords as $password => $expected) {
        $validation = Validator::validatePassword($password, 6);
        $result = $validation['valid'] === $expected;
        echo "  '{$password}': " . ($result ? "✓" : "✗") . "\n";
        if (!$validation['valid']) {
            echo "    错误: " . implode(', ', $validation['errors']) . "\n";
        }
    }
    
    // 测试3: 检查用户名是否存在
    echo "\n测试3: 检查用户名是否存在\n";
    $existingUser = $userRepo->findByUsername('admin@test.com');
    if ($existingUser) {
        echo "  admin@test.com 存在: ✓\n";
    } else {
        echo "  admin@test.com 不存在: ✗\n";
    }
    
    // 测试4: 创建测试用户（如果不存在）
    echo "\n测试4: 创建测试用户\n";
    $testUsername = 'testuser@test.com';
    
    if ($userRepo->usernameExists($testUsername)) {
        echo "  测试用户已存在，跳过创建\n";
    } else {
        try {
            $newUser = $userRepo->create($testUsername, 'test123456', false, true);
            echo "  创建用户成功: ✓\n";
            echo "    用户ID: {$newUser['id']}\n";
            echo "    用户名: {$newUser['username']}\n";
            echo "    是否管理员: " . ($newUser['is_admin'] ? '是' : '否') . "\n";
        } catch (Exception $e) {
            echo "  创建用户失败: ✗ - " . $e->getMessage() . "\n";
        }
    }
    
    // 测试5: 验证密码
    echo "\n测试5: 验证密码\n";
    $testUser = $userRepo->findByUsername($testUsername);
    if ($testUser) {
        $verified = $userRepo->verifyPassword($testUsername, 'test123456');
        if ($verified) {
            echo "  密码验证成功: ✓\n";
        } else {
            echo "  密码验证失败: ✗\n";
        }
    }
    
    // 测试6: 获取所有用户
    echo "\n测试6: 获取用户列表\n";
    $users = $userRepo->getAll(10);
    echo "  用户总数: " . count($users) . "\n";
    foreach ($users as $user) {
        echo "    - {$user['username']} (ID: {$user['id']}, " . 
             ($user['is_admin'] ? '管理员' : '普通用户') . ", " .
             ($user['is_active'] ? '激活' : '禁用') . ")\n";
    }
    
    echo "\n=== 测试完成 ===\n";
    
} catch (Exception $e) {
    echo "错误: " . $e->getMessage() . "\n";
    echo "堆栈跟踪:\n" . $e->getTraceAsString() . "\n";
}

