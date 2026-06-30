<?php
require_once __DIR__ . '/../storage/Database.php';

/**
 * 一次性把 users.username 中 @oldDomain 批量替换成 @newDomain
 */
class SyncDomainService
{
    private $db;
    public function __construct()
    {
        $this->db = Database::getInstance();
    }

    /**
     * @return int 实际被更新的用户数
     */
    public function run(string $oldDomain, string $newDomain): int
    {
        // 防止注入，直接用 REPLACE 函数最省事
        $sql = "UPDATE users
                SET username = REPLACE(username, :old, :new)
                WHERE username LIKE :like";
        $stmt = $this->db->prepare($sql);
        $stmt->execute([
            ':old'  => '@' . $oldDomain,
            ':new'  => '@' . $newDomain,
            ':like' => '%@' . $oldDomain
        ]);
        return $stmt->rowCount();
    }
}