package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.UserDbPermission;
import com.baoma.natural_language_query.service.impl.TablePermissionServiceImpl;
import com.baoma.natural_language_query.utils.SqlTableExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 表权限服务测试类
 */
public class TablePermissionServiceTest {
    
    @Mock
    private UserDbPermissionService userDbPermissionService;
    
    @InjectMocks
    private TablePermissionServiceImpl tablePermissionService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testSqlTableExtractor() {
        // 测试SQL表名提取功能
        String sql1 = "SELECT * FROM film WHERE release_year > 2000";
        Set<String> tables1 = SqlTableExtractor.extractTableNames(sql1);
        assertTrue(tables1.contains("film"));
        
        String sql2 = "SELECT f.title, c.name FROM film f JOIN category c ON f.category_id = c.id";
        Set<String> tables2 = SqlTableExtractor.extractTableNames(sql2);
        assertTrue(tables2.contains("film"));
        assertTrue(tables2.contains("category"));
        
        String sql3 = "SELECT * FROM actor, film_actor WHERE actor.id = film_actor.actor_id";
        Set<String> tables3 = SqlTableExtractor.extractTableNames(sql3);
        assertTrue(tables3.contains("actor"));
        assertTrue(tables3.contains("film_actor"));
        
        // 测试只读检查
        assertTrue(SqlTableExtractor.isReadOnlyQuery("SELECT * FROM film"));
        assertFalse(SqlTableExtractor.isReadOnlyQuery("INSERT INTO film VALUES (1, 'Test')"));
        assertFalse(SqlTableExtractor.isReadOnlyQuery("UPDATE film SET title = 'New Title'"));
        assertFalse(SqlTableExtractor.isReadOnlyQuery("DELETE FROM film WHERE id = 1"));
    }
    
    @Test
    void testPermissionCheckWithAllowedTable() {
        // 模拟用户权限：允许访问film表
        UserDbPermission permission = createUserPermission(
            8L, 
            "[{\"table_ids\": [], \"table_names\": [\"film\"], \"db_connection_id\": 1}]"
        );
        
        when(userDbPermissionService.getByUserId(8L)).thenReturn(permission);
        
        // 测试允许的SQL
        String sql = "SELECT * FROM film WHERE release_year > 2000";
        TablePermissionService.PermissionCheckResult result = 
            tablePermissionService.checkSqlPermissions(8L, 1L, sql);
        
        assertTrue(result.isAllowed());
        assertEquals("权限检查通过", result.getMessage());
    }
    
    @Test
    void testPermissionCheckWithDeniedTable() {
        // 模拟用户权限：只允许访问film表
        UserDbPermission permission = createUserPermission(
            8L, 
            "[{\"table_ids\": [], \"table_names\": [\"film\"], \"db_connection_id\": 1}]"
        );
        
        when(userDbPermissionService.getByUserId(8L)).thenReturn(permission);
        
        // 测试访问未授权的表
        String sql = "SELECT * FROM actor WHERE name LIKE '%John%'";
        TablePermissionService.PermissionCheckResult result = 
            tablePermissionService.checkSqlPermissions(8L, 1L, sql);
        
        assertFalse(result.isAllowed());
        assertTrue(result.getMessage().contains("用户无权限访问以下表"));
        assertTrue(result.getMessage().contains("actor"));
    }
    
    @Test
    void testPermissionCheckWithMultipleTables() {
        // 模拟用户权限：允许访问film表
        UserDbPermission permission = createUserPermission(
            8L, 
            "[{\"table_ids\": [], \"table_names\": [\"film\"], \"db_connection_id\": 1}]"
        );
        
        when(userDbPermissionService.getByUserId(8L)).thenReturn(permission);
        
        // 测试JOIN查询，包含允许和不允许的表
        String sql = "SELECT f.title, c.name FROM film f JOIN category c ON f.category_id = c.id";
        TablePermissionService.PermissionCheckResult result = 
            tablePermissionService.checkSqlPermissions(8L, 1L, sql);
        
        assertFalse(result.isAllowed());
        assertTrue(result.getMessage().contains("category"));
    }
    
    @Test
    void testPermissionCheckWithNoPermission() {
        // 模拟用户没有权限配置
        when(userDbPermissionService.getByUserId(8L)).thenReturn(null);
        
        String sql = "SELECT * FROM film";
        TablePermissionService.PermissionCheckResult result = 
            tablePermissionService.checkSqlPermissions(8L, 1L, sql);
        
        assertFalse(result.isAllowed());
        assertEquals("用户未分配数据库访问权限", result.getMessage());
    }
    
    @Test
    void testPermissionCheckWithWriteOperation() {
        // 模拟用户权限
        UserDbPermission permission = createUserPermission(
            8L, 
            "[{\"table_ids\": [], \"table_names\": [\"film\"], \"db_connection_id\": 1}]"
        );
        
        when(userDbPermissionService.getByUserId(8L)).thenReturn(permission);
        
        // 测试写操作
        String sql = "INSERT INTO film (title) VALUES ('New Film')";
        TablePermissionService.PermissionCheckResult result = 
            tablePermissionService.checkSqlPermissions(8L, 1L, sql);
        
        assertFalse(result.isAllowed());
        assertTrue(result.getMessage().contains("不允许执行非只读操作"));
    }
    
    @Test
    void testGetUserAccessibleTables() {
        // 模拟用户权限
        UserDbPermission permission = createUserPermission(
            8L, 
            "[{\"table_ids\": [], \"table_names\": [\"film\", \"actor\"], \"db_connection_id\": 1}]"
        );
        
        when(userDbPermissionService.getByUserId(8L)).thenReturn(permission);
        
        Set<String> accessibleTables = tablePermissionService.getUserAccessibleTables(8L, 1L);
        
        assertEquals(2, accessibleTables.size());
        assertTrue(accessibleTables.contains("film"));
        assertTrue(accessibleTables.contains("actor"));
    }
    
    private UserDbPermission createUserPermission(Long userId, String permissionDetails) {
        UserDbPermission permission = new UserDbPermission();
        permission.setId(1L);
        permission.setUserId(userId);
        permission.setPermissionDetails(permissionDetails);
        permission.setLastGrantUserId(2L);
        permission.setIsAssigned(1);
        permission.setLastGrantTime(LocalDateTime.now());
        permission.setUpdateTime(LocalDateTime.now());
        return permission;
    }
}
