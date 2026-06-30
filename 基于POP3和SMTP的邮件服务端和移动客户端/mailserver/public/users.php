<?php
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/storage/Database.php';
require_once __DIR__ . '/../src/storage/UserRepository.php';
require_once __DIR__ . '/../src/utils/Validator.php';
require_once __DIR__ . '/../src/utils/Security.php';
require_once __DIR__ . '/../src/storage/SystemSettingsRepository.php';

session_start();

// 身份验证
if (!isset($_SESSION['user_id'])) {
    header('Location: index.php');
    exit;
}

$settingsRepo = new SystemSettingsRepository();
// 获取域名设置（放在函数定义之前）
$domain = $settingsRepo->get('domain', 'test.com');

$userRepo = new UserRepository();
$message = '';
$error = '';

// 处理创建用户
if (isset($_POST['create_user'])) {
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';
    $isAdmin = isset($_POST['is_admin']) ? 1 : 0;
    $isActive = isset($_POST['is_active']) ? 1 : 0;
    
    $usernameValidation = Validator::validateUsername($username);
    if (!$usernameValidation['valid']) {
        $error = implode('<br>', $usernameValidation['errors']);
    } else {
        if (!Validator::validateEmailDomain($username, $domain)) {
            $error = "邮箱域名必须是 @".$domain;
        } else {
            $passwordValidation = Validator::validatePassword($password, 6);
            if (!$passwordValidation['valid']) {
                $error = implode('<br>', $passwordValidation['errors']);
            } else {
                try {
                    if ($userRepo->usernameExists($username)) {
                        $error = "用户名已存在";
                    } else {
                        $userRepo->create($username, $password, $isAdmin, $isActive);
                        $message = "用户创建成功";
                    }
                } catch (Exception $e) {
                    $error = "创建失败: " . $e->getMessage();
                }
            }
        }
    }
}

// 处理更新用户
if (isset($_POST['update_user'])) {
    $userId = (int)$_POST['user_id'];
    $data = [];
    
    if (!empty($_POST['new_password'])) {
        $passwordValidation = Validator::validatePassword($_POST['new_password'], 6);
        if (!$passwordValidation['valid']) {
            $error = implode('<br>', $passwordValidation['errors']);
        } else {
            $data['password'] = $_POST['new_password'];
        }
    }
    
    /**if (isset($_POST['is_admin'])) {
        $data['is_admin'] = (int)$_POST['is_admin'];
    }
    
    if (isset($_POST['is_active'])) {
        $data['is_active'] = (int)$_POST['is_active'];
    }**/
    // 管理员权限总是更新
    $data['is_admin'] = isset($_POST['is_admin']) ? 1 : 0;
    
    // 激活状态也是
    $data['is_active'] = isset($_POST['is_active']) ? 1 : 0;
    
    if (empty($error) && !empty($data)) {
        if ($userRepo->update($userId, $data)) {
            $message = "用户更新成功";
        } else {
            $error = "更新失败";
        }
    }
}

// 处理删除用户
if (isset($_GET['delete'])) {
    $userId = (int)$_GET['delete'];
    if ($userId != $_SESSION['user_id']) { // 不能删除自己
        if ($userRepo->delete($userId)) {
            $message = "用户删除成功";
        } else {
            $error = "删除失败";
        }
    } else {
        $error = "不能删除自己的账号";
    }
}

// 获取所有用户
$users = $userRepo->getAll();
?>
<!DOCTYPE html>
<html>
<head>
    <title>用户管理 - 邮件服务器</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
        .header { background: #007bff; color: white; padding: 15px; margin: -20px -20px 20px -20px; }
        .menu { background: white; padding: 10px; margin-bottom: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .menu a { margin-right: 15px; text-decoration: none; color: #007bff; }
        .container { background: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .message { background: #d4edda; color: #155724; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        .error { background: #f8d7da; color: #721c24; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background: #f8f9fa; font-weight: 600; }
        tr:hover { background: #f8f9fa; }
        .btn { padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; text-decoration: none; display: inline-block; }
        .btn-primary { background: #007bff; color: white; }
        .btn-danger { background: #dc3545; color: white; }
        .btn-success { background: #28a745; color: white; }
        .btn-small { padding: 4px 8px; font-size: 12px; }
        .form-group { margin-bottom: 15px; }
        .form-group label { display: block; margin-bottom: 5px; font-weight: 500; }
        .form-group input, .form-group select { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; }
        .form-inline { display: flex; gap: 10px; align-items: flex-end; }
        .form-inline .form-group { flex: 1; margin-bottom: 0; }
        .badge { padding: 4px 8px; border-radius: 3px; font-size: 12px; font-weight: 500; }
        .badge-admin { background: #ffc107; color: #000; }
        .badge-active { background: #28a745; color: white; }
        .badge-inactive { background: #6c757d; color: white; }
        .modal { display: none; position: fixed; z-index: 1000; left: 0; top: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); }
        .modal-content { background: white; margin: 50px auto; padding: 20px; width: 500px; border-radius: 5px; }
        .close { float: right; font-size: 28px; font-weight: bold; cursor: pointer; }
    </style>
</head>
<body>
    <div class="header">
        <h1>邮件服务器管理后台</h1>
        <div>欢迎, <?php echo htmlspecialchars($_SESSION['username']); ?> 
            (<a href="logout.php" style="color: white;">退出</a>)
        </div>
    </div>
    
    <div class="menu">
        <a href="index.php">仪表盘</a>
        <a href="users.php">用户管理</a>
        <a href="broadcast.php">群发邮件</a>
        <a href="filters.php">过滤规则</a>
        <a href="logs.php">系统日志</a>
        <a href="services.php">服务管理</a>
        <a href="settings.php">系统设置</a>
        <a href="help.php">帮助</a>
    </div>
    
    <div class="container">
        <h2>用户管理</h2>
        
        <?php if ($message): ?>
            <div class="message"><?php echo $message; ?></div>
        <?php endif; ?>
        
        <?php if ($error): ?>
            <div class="error"><?php echo $error; ?></div>
        <?php endif; ?>
        
        <!-- 创建用户表单 -->
        <h3>创建新用户</h3>
        <form method="POST" class="form-inline">
            <div class="form-group">
                <label>邮箱地址</label>
                <input type="email" name="username" placeholder="user@<?= htmlspecialchars($domain) ?>" required>
            </div>
            <div class="form-group">
                <label>密码</label>
                <input type="password" name="password" placeholder="至少6个字符" required minlength="6">
            </div>
            <div class="form-group">
                <label>管理员</label>
                <input type="checkbox" name="is_admin" value="1">
            </div>
            <div class="form-group">
                <label>激活</label>
                <input type="checkbox" name="is_active" value="1" checked>
            </div>
            <div class="form-group">
                <button type="submit" name="create_user" class="btn btn-primary">创建用户</button>
            </div>
        </form>
        
        <!-- 用户列表 -->
        <h3>用户列表 (<?php echo count($users); ?>)</h3>
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>用户名</th>
                    <th>角色</th>
                    <th>状态</th>
                    <th>创建时间</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($users as $user): ?>
                <tr>
                    <td><?php echo $user['id']; ?></td>
                    <td><?php echo htmlspecialchars($user['username']); ?></td>
                    <td>
                        <?php if ($user['is_admin']): ?>
                            <span class="badge badge-admin">管理员</span>
                        <?php else: ?>
                            <span>普通用户</span>
                        <?php endif; ?>
                    </td>
                    <td>
                        <?php if ($user['is_active']): ?>
                            <span class="badge badge-active">激活</span>
                        <?php else: ?>
                            <span class="badge badge-inactive">禁用</span>
                        <?php endif; ?>
                    </td>
                    <td><?php echo $user['created_at']; ?></td>
                    <td>
                        <a href="#" onclick="editUser(<?php echo htmlspecialchars(json_encode($user)); ?>); return false;" class="btn btn-primary btn-small">编辑</a>
                        <?php if ($user['id'] != $_SESSION['user_id']): ?>
                            <a href="?delete=<?php echo $user['id']; ?>" class="btn btn-danger btn-small" onclick="return confirm('确定要删除此用户吗？');">删除</a>
                        <?php endif; ?>
                    </td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>
    
    <!-- 编辑用户模态框 -->
    <div id="editModal" class="modal">
        <div class="modal-content">
            <span class="close" onclick="closeModal()">&times;</span>
            <h3>编辑用户</h3>
            <form method="POST">
                <input type="hidden" name="user_id" id="edit_user_id">
                <div class="form-group">
                    <label>用户名</label>
                    <input type="text" id="edit_username" readonly style="background: #f5f5f5;">
                </div>
                <div class="form-group">
                    <label>新密码（留空则不修改）</label>
                    <input type="password" name="new_password" placeholder="留空则不修改">
                </div>
                <div class="form-group">
                    <label>
                        <input type="checkbox" name="is_admin" id="edit_is_admin" value="1"> 管理员
                    </label>
                </div>
                <div class="form-group">
                    <label>
                        <input type="checkbox" name="is_active" id="edit_is_active" value="1"> 激活
                    </label>
                </div>
                <button type="submit" name="update_user" class="btn btn-success">保存</button>
                <button type="button" onclick="closeModal()" class="btn">取消</button>
            </form>
        </div>
    </div>
    
    <script>
        function editUser(user) {
            document.getElementById('edit_user_id').value = user.id;
            document.getElementById('edit_username').value = user.username;
            document.getElementById('edit_is_admin').checked = user.is_admin == 1;
            document.getElementById('edit_is_active').checked = user.is_active == 1;
            document.getElementById('editModal').style.display = 'block';
        }
        
        function closeModal() {
            document.getElementById('editModal').style.display = 'none';
        }
        
        window.onclick = function(event) {
            var modal = document.getElementById('editModal');
            if (event.target == modal) {
                closeModal();
            }
        }
    </script>
</body>
</html>

