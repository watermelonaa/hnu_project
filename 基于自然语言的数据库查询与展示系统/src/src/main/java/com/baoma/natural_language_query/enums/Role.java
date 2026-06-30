package com.baoma.natural_language_query.enums;

/**
 * 系统角色枚举
 */
public enum Role {
    /** 系统管理员 (roleId=1) */
    ADMIN(1, "系统管理员"),
    /** 数据管理员 (roleId=2) */
    DATA_ADMIN(2, "数据管理员"),
    /** 普通用户 (roleId=3) */
    USER(3, "普通用户");

    private final int roleId;
    private final String roleName;

    Role(int roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
    }

    public int getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public static Role fromRoleId(Integer roleId) {
        if (roleId == null) {
            return USER;
        }
        for (Role role : values()) {
            if (role.roleId == roleId) {
                return role;
            }
        }
        return USER;
    }
}


