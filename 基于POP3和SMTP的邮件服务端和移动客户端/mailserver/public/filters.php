<?php
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../src/storage/Database.php';
require_once __DIR__ . '/../src/storage/FilterRepository.php';
require_once __DIR__ . '/../src/utils/Validator.php';
require_once __DIR__ . '/../src/utils/Security.php';

//开启所有错误日志报告
error_reporting(E_ALL);
ini_set('display_errors', 1);

session_start();

// 身份验证
if (!isset($_SESSION['user_id'])) {
    header('Location: index.php');
    exit;
}

$filterRepo = new FilterRepository();
$message = '';
$error = '';

if (isset($_POST['toggle_id'])) {
    $id = (int)$_POST['toggle_id'];
    $row = $filterRepo->getById($id);
    if ($row) {
        $filterRepo->updateStatus($id, !(bool)$row['is_active']);
    }
    header('Location: filters.php');   // 302 跳回干净地址
    exit;
}



// 处理创建过滤规则
if (isset($_POST['create_filter'])) {
    $ruleType = $_POST['rule_type'] ?? '';
    $ruleValue = trim($_POST['rule_value'] ?? '');
    $action = $_POST['action'] ?? 'block';
    $description = trim($_POST['description'] ?? '');
    
    if (empty($ruleValue)) {
        $error = "规则值不能为空";
    } else {
        if ($ruleType === 'email') {
            if (!Validator::validateEmail($ruleValue)) {
                $error = "邮箱格式无效";
            }
        } elseif ($ruleType === 'ip') {
            if (!Validator::validateIP($ruleValue)) {
                $error = "IP地址格式无效";
            }
        } else {
            $error = "规则类型无效";
        }
        
        if (empty($error)) {
            try {
                if ($filterRepo->create($ruleType, $ruleValue, $action, $description)) {
                    $message = "过滤规则创建成功";
                } else {
                    $error = "创建失败，可能已存在相同规则";
                }
            } catch (Exception $e) {
                $error = "创建失败: " . $e->getMessage();
            }
        }
    }
}

// 处理删除规则
if (isset($_GET['delete'])) {
    $id = (int)$_GET['delete'];
    if ($filterRepo->delete($id)) {
        $message = "规则删除成功";
    } else {
        $error = "删除失败";
    }
}

/*------------------------------------
// 处理切换规则状态
if (isset($_GET['toggle'])) {
    $id = (int)$_GET['toggle'];
    $row = $filterRepo->getById($id);   // 改用 public 方法
    if ($row) {
        $newStatus = !(bool)$row['is_active'];
        if ($filterRepo->updateStatus($id, $newStatus)) {
            $message = "规则状态已更新";
        } else {
            $error  = "更新失败";
        }
    } else {
        $error = "规则不存在";
    }
}
------------------------------------ */
// 获取所有规则
$rules = $filterRepo->getAll();

?>
<!DOCTYPE html>
<html>
<head>
    <title>过滤规则 - 邮件服务器</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
        .header { background: #007bff; color: white; padding: 15px; margin: -20px -20px 20px -20px; }
        .menu { background: white; padding: 10px; margin-bottom: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .menu a { margin-right: 15px; text-decoration: none; color: #007bff; }
        .container { background: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .message { background: #d4edda; color: #155724; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        .error { background: #f8d7da; color: #721c24; padding: 12px; border-radius: 5px; margin-bottom: 20px; }
        .form-group { margin-bottom: 15px; }
        .form-group label { display: block; margin-bottom: 5px; font-weight: 500; }
        .form-group input, .form-group select, .form-group textarea { width: 100%; max-width: 500px; padding: 8px; border: 1px solid #ddd; border-radius: 4px; }
        .form-inline { display: flex; gap: 10px; align-items: flex-end; }
        .form-inline .form-group { flex: 1; margin-bottom: 0; }
        .btn { padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; text-decoration: none; display: inline-block; }
        .btn-primary { background: #007bff; color: white; }
        .btn-danger { background: #dc3545; color: white; }
        .btn-success { background: #28a745; color: white; }
        .btn-warning { background: #ffc107; color: #000; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background: #f8f9fa; }
        .badge { padding: 4px 8px; border-radius: 3px; font-size: 12px; font-weight: 500; }
        .badge-email { background: #17a2b8; color: white; }
        .badge-ip { background: #6c757d; color: white; }
        .badge-block { background: #dc3545; color: white; }
        .badge-allow { background: #28a745; color: white; }
        .badge-active { background: #28a745; color: white; }
        .badge-inactive { background: #6c757d; color: white; }
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
        <h2>过滤规则管理</h2>
        
        <?php if ($message): ?>
            <div class="message"><?php echo $message; ?></div>
        <?php endif; ?>
        
        <?php if ($error): ?>
            <div class="error"><?php echo $error; ?></div>
        <?php endif; ?>
        
        <!-- 创建过滤规则 -->
        <h3>创建过滤规则</h3>
        <form method="POST" class="form-inline">
            <div class="form-group">
                <label>规则类型</label>
                <select name="rule_type" required>
                    <option value="email">邮箱过滤</option>
                    <option value="ip">IP地址过滤</option>
                </select>
            </div>
            <div class="form-group">
                <label>规则值</label>
                <input type="text" name="rule_value" placeholder="邮箱或IP地址" required>
            </div>
            <div class="form-group">
                <label>动作</label>
                <select name="action" required>
                    <option value="block">阻止</option>
                    <option value="allow">允许</option>
                </select>
            </div>
            <div class="form-group">
                <label>描述</label>
                <input type="text" name="description" placeholder="规则描述（可选）">
            </div>
            <div class="form-group">
                <button type="submit" name="create_filter" class="btn btn-primary">创建规则</button>
            </div>
        </form>
        
        <!-- 规则列表 -->
        <h3>过滤规则列表 (<?php echo count($rules); ?>)</h3>
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>类型</th>
                    <th>规则值</th>
                    <th>动作</th>
                    <th>描述</th>
                    <th>状态</th>
                    <th>创建时间</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                <?php if (empty($rules)): ?>
                    <tr>
                        <td colspan="8" style="text-align: center; padding: 40px;">暂无过滤规则</td>
                    </tr>
                <?php else: ?>
                    <?php foreach ($rules as $rule): ?>
                    <tr>
                        <td><?php echo $rule['id']; ?></td>
                        <td>
                            <span class="badge badge-<?php echo $rule['rule_type']; ?>">
                                <?php echo $rule['rule_type'] === 'email' ? '邮箱' : 'IP'; ?>
                            </span>
                        </td>
                        <td><?php echo htmlspecialchars($rule['rule_value']); ?></td>
                        <td>
                            <span class="badge badge-<?php echo $rule['action']; ?>">
                                <?php echo $rule['action'] === 'block' ? '阻止' : '允许'; ?>
                            </span>
                        </td>
                        <td><?php echo htmlspecialchars($rule['description'] ?? '-'); ?></td>
                        <td>
                            <span class="badge badge-<?php echo $rule['is_active'] ? 'active' : 'inactive'; ?>">
                                <?php echo $rule['is_active'] ? '激活' : '禁用'; ?>
                            </span>
                        </td>
                        <td><?php echo $rule['created_at']; ?></td>
                        <td>
                            <form method="post" style="display:inline;">
                                <input type="hidden" name="toggle_id" value="<?php echo $rule['id']; ?>">
                                <button type="submit" class="btn btn-warning">
                                    <?php echo $rule['is_active'] ? '禁用' : '启用'; ?>
                                </button>
                            </form>
                            <a href="?delete=<?php echo $rule['id']; ?>" class="btn btn-danger" onclick="return confirm('确定要删除此规则吗？');">删除</a>
                        </td>
                    </tr>
                    <?php endforeach; ?>
                <?php endif; ?>
            </tbody>
        </table>
    </div>
</body>
</html>

