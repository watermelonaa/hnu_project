# 自然语言数据库查询系统 - 模块说明文档

## 项目概述

基于自然语言的数据库查询和展示系统，支持用户通过自然语言描述查询需求，系统自动调用大模型解析查询意图，生成并执行SQL语句，将查询结果以图表形式可视化展示。

### 核心功能

- **用户管理**：管理员可以管理和授权用户
- **权限控制**：授权的用户可以查询授权的数据库和表格
- **自然语言查询**：采用自然语言描述查询请求，系统调用大模型感知数据库元数据并解析用户查询请求
- **SQL生成与验证**：采用SQL引擎生成和验证SQL语句，测试通过后将查询的数据采用Chart图显示
- **多轮对话**：用户可调整查询内容，系统根据用户调整请求修改SQL语句，重新查询和可视化查询结果
- **复杂查询支持**：支持多表联合等复杂SQL语句查询
- **并发支持**：支持至少5个及以上并发用户，每个用户的任务在Docker容器独立运行

### 技术栈

- **后端框架**：Spring Boot 2.x/3.x
- **开发语言**：Java 21 (JDK 21)
- **关系型数据库**：MySQL
- **非关系型数据库**：MongoDB
- **ORM框架**：MyBatis Plus
- **安全框架**：Spring Security + JWT
- **容器化**：Docker
- **实时通信**：WebSocket

---

## 模块结构说明

### 1. 主应用入口 (`NaturalLanguageQueryApplication`)

**位置**：`com.baoma.natural_language_query.NaturalLanguageQueryApplication`

**功能**：Spring Boot 应用启动类，负责初始化应用程序上下文。

---

### 2. 控制器层 (`controller`)

**位置**：`com.baoma.natural_language_query.controller`

**功能**：处理HTTP请求，提供RESTful API接口。

#### 核心控制器

- **`QueryController`**：自然语言查询接口，处理用户查询请求
- **`AuthController`**：用户认证接口，处理登录、注册、密码重置等
- **`UserController`**：用户管理接口，处理用户CRUD操作
- **`DbConnectionController`**：数据库连接管理接口
- **`TableMetadataController`**：表元数据管理接口
- **`ColumnMetadataController`**：字段元数据管理接口
- **`QueryLogController`**：查询日志管理接口
- **`QueryShareController`**：查询分享管理接口
- **`ChatWebSocketController`**：WebSocket聊天控制器，支持实时通信
- **`DialogController`**：对话记录管理接口
- **`DashboardController`**：仪表盘数据接口
- **`NotificationController`**：通知管理接口
- **`FriendRelationController`**：好友关系管理接口
- **`FriendChatController`**：好友聊天接口
- **`LLMConfigController`**：大模型配置管理接口
- **`SystemHealthController`**：系统健康检查接口

#### 其他控制器

- **`RoleController`**：角色管理
- **`UserDbPermissionController`**：用户数据库权限管理
- **`QueryCollectionController`**：查询收藏管理
- **`CollectionRecordController`**：收藏记录管理
- **`SqlCacheController`**：SQL缓存管理
- **`ErrorLogController`**：错误日志管理
- **`OperationLogController`**：操作日志管理
- **`PerformanceMetricController`**：性能指标管理
- **`TokenConsumeController`**：Token消费记录管理
- **`UserSearchController`**：用户搜索接口

---

### 3. 服务层 (`service`)

**位置**：`com.baoma.natural_language_query.service`

**功能**：实现核心业务逻辑，提供业务服务接口和实现。

#### 核心服务

- **`QueryService`**：自然语言查询核心服务
  - 调用大模型解析用户自然语言查询
  - 感知数据库元数据（表结构、字段信息等）
  - 生成并验证SQL语句
  - 检查用户权限（表权限）
  - 执行SQL查询数据库
  - 将查询结果转换为表格数据和图表数据
  - 保存对话记录和查询日志

- **`LLMService`**：大模型调用服务
  - 调用大模型API，传入用户自然语言查询和数据库结构信息
  - 大模型根据用户意图和数据库结构生成SQL语句
  - 解析大模型返回的结果，提取SQL、表格数据、图表数据等

- **`AuthService`**：用户认证服务
  - 用户登录认证
  - JWT Token生成和验证
  - 密码加密和验证
  - 单点登录支持

- **`DatabaseSchemaService`**：数据库结构服务
  - 获取数据库表结构信息
  - 获取表字段元数据
  - 数据库结构导出

- **`TablePermissionService`**：表权限服务
  - 检查用户对表的访问权限
  - 权限验证和过滤

#### 数据管理服务

- **`UserService`**：用户管理服务
- **`RoleService`**：角色管理服务
- **`DbConnectionService`**：数据库连接管理服务
- **`TableMetadataService`**：表元数据服务
- **`ColumnMetadataService`**：字段元数据服务
- **`UserDbPermissionService`**：用户数据库权限服务

#### 查询相关服务

- **`QueryLogService`**：查询日志服务
- **`QueryShareService`**：查询分享服务
- **`QueryCollectionService`**：查询收藏服务
- **`SqlCacheService`**：SQL缓存服务
- **`DialogService`**：对话记录服务
- **`DialogDetailService`**：对话详情服务
- **`ConversationContextService`**：对话上下文服务

#### 社交功能服务

- **`FriendRelationService`**：好友关系服务
- **`FriendRequestService`**：好友请求服务
- **`FriendChatService`**：好友聊天服务
- **`NotificationService`**：通知服务

#### 系统管理服务

- **`LLMConfigService`**：大模型配置服务
- **`LLMStatusService`**：大模型状态服务
- **`SystemHealthService`**：系统健康检查服务
- **`PerformanceMetricService`**：性能指标服务
- **`ErrorLogService`**：错误日志服务
- **`OperationLogService`**：操作日志服务
- **`TokenConsumeService`**：Token消费服务
- **`AiInteractionLogService`**：AI交互日志服务

#### 其他服务

- **`EmailService`**：邮件发送服务
- **`TokenBlacklistService`**：Token黑名单服务
- **`UserNotificationReadService`**：用户通知已读服务
- **`CollectionRecordService`**：收藏记录服务
- **`DbConnectionLogService`**：数据库连接日志服务
- **`DbTypeService`**：数据库类型服务
- **`ErrorTypeService`**：错误类型服务
- **`PriorityService`**：优先级服务
- **`DashboardService`**：仪表盘服务

---

### 4. 数据访问层

#### 4.1 Mapper层 (`mapper`)

**位置**：`com.baoma.natural_language_query.mapper`

**功能**：MyBatis Plus Mapper接口，提供数据库CRUD操作。

**主要Mapper**：
- `UserMapper`：用户数据访问
- `RoleMapper`：角色数据访问
- `DbConnectionMapper`：数据库连接数据访问
- `TableMetadataMapper`：表元数据访问
- `ColumnMetadataMapper`：字段元数据访问
- `QueryLogMapper`：查询日志访问
- `UserDbPermissionMapper`：用户数据库权限访问
- `QueryShareMapper`：查询分享访问
- `NotificationMapper`：通知数据访问
- `FriendRelationMapper`：好友关系访问
- `FriendRequestMapper`：好友请求访问
- `LLMConfigMapper`：大模型配置访问
- `SystemHealthMapper`：系统健康数据访问
- `PerformanceMetricMapper`：性能指标访问
- `ErrorLogMapper`：错误日志访问
- `OperationLogMapper`：操作日志访问
- `TokenConsumeMapper`：Token消费记录访问
- 等其他Mapper接口

#### 4.2 Repository层 (`repository`)

**位置**：`com.baoma.natural_language_query.repository`

**功能**：MongoDB数据访问接口，使用Spring Data MongoDB。

**主要Repository**：
- `DialogRecordRepository`：对话记录MongoDB访问
- `DialogDetailRepository`：对话详情MongoDB访问
- `CollectionRecordRepository`：收藏记录MongoDB访问
- `QueryCollectionRepository`：查询收藏MongoDB访问
- `SqlCacheRepository`：SQL缓存MongoDB访问
- `FriendChatRepository`：好友聊天MongoDB访问
- `AiInteractionLogRepository`：AI交互日志MongoDB访问

---

### 5. 实体类 (`entity`)

#### 5.1 MySQL实体 (`entity.mysql`)

**位置**：`com.baoma.natural_language_query.entity.mysql`

**功能**：MySQL数据库表对应的实体类，使用MyBatis Plus注解。

**主要实体**：
- `User`：用户实体
- `Role`：角色实体
- `DbConnection`：数据库连接实体
- `DbType`：数据库类型实体
- `TableMetadata`：表元数据实体
- `ColumnMetadata`：字段元数据实体
- `UserDbPermission`：用户数据库权限实体
- `QueryLog`：查询日志实体
- `QueryShare`：查询分享实体
- `DialogRecord`：对话记录实体（MySQL版本）
- `Notification`：通知实体
- `FriendRelation`：好友关系实体
- `FriendRequest`：好友请求实体
- `LLMConfig`：大模型配置实体
- `LLMStatus`：大模型状态实体
- `SystemHealth`：系统健康实体
- `PerformanceMetric`：性能指标实体
- `ErrorLog`：错误日志实体
- `OperationLog`：操作日志实体
- `TokenConsume`：Token消费记录实体
- `UserNotificationRead`：用户通知已读实体
- `UserSearch`：用户搜索实体
- `DbConnectionLog`：数据库连接日志实体
- `ErrorType`：错误类型实体
- `Priority`：优先级实体

#### 5.2 MongoDB实体 (`entity.mongodb`)

**位置**：`com.baoma.natural_language_query.entity.mongodb`

**功能**：MongoDB集合对应的实体类，使用Spring Data MongoDB注解。

**主要实体**：
- `DialogRecord`：对话记录实体（MongoDB版本）
- `DialogDetail`：对话详情实体
- `CollectionRecord`：收藏记录实体
- `QueryCollection`：查询收藏实体
- `SqlCache`：SQL缓存实体
- `FriendChat`：好友聊天实体
- `AiInteractionLog`：AI交互日志实体

---

### 6. 数据传输对象 (`dto`)

**位置**：`com.baoma.natural_language_query.dto`

**功能**：数据传输对象，用于Controller层接收前端请求参数。

**主要DTO**：
- `LoginDTO`：登录请求DTO
- `QueryRequestDTO`：查询请求DTO
- `RecommendationRequestDTO`：推荐请求DTO
- `ChangePasswordDTO`：修改密码DTO
- `ResetPasswordDTO`：重置密码DTO
- `ShareQueryDTO`：分享查询DTO
- `SendChatMessageDTO`：发送聊天消息DTO
- `SendFriendRequestDTO`：发送好友请求DTO
- `QueryLogDTO`：查询日志DTO
- `QueryCollectionDTO`：查询收藏DTO
- `CollectionRecordDTO`：收藏记录DTO
- `ChatMessagePageDTO`：聊天消息分页DTO

---

### 7. 视图对象 (`vo`)

**位置**：`com.baoma.natural_language_query.vo`

**功能**：视图对象，用于Controller层返回给前端的数据封装。

**主要VO**：
- `QueryResponseVO`：查询响应VO
- `LoginVO`：登录响应VO
- 等其他VO对象

---

### 8. 工具类 (`utils`)

**位置**：`com.baoma.natural_language_query.utils`

**功能**：提供各种工具方法和辅助功能。

**主要工具类**：
- `JwtUtil`：JWT Token工具类，用于生成和解析Token
- `PermissionUtil`：权限工具类，用于权限检查和验证
- `SqlManager`：SQL管理工具类，用于SQL语句处理
- `SqlTableExtractor`：SQL表提取工具类，从SQL中提取表名
- `SqlSimilarityUtil`：SQL相似度工具类，用于SQL缓存匹配
- `DynamicDatabaseExecutor`：动态数据库执行器，用于执行动态SQL
- `DatabaseSchemaExporter`：数据库结构导出工具
- `PromptManager`：提示词管理工具类，管理大模型提示词模板
- `PromptMeta`：提示词元数据
- `PromptTemplateType`：提示词模板类型枚举
- `FollowupQuestionGenerator`：后续问题生成器

---

### 9. 配置类 (`config`)

**位置**：`com.baoma.natural_language_query.config`

**功能**：Spring Boot配置类，配置各种组件和拦截器。

**主要配置类**：
- `SecurityConfig`：Spring Security安全配置
- `WebMvcConfig`：Web MVC配置
- `CorsConfig`：跨域配置
- `JwtInterceptor`：JWT拦截器，用于Token验证
- `WebSocketConfig`：WebSocket配置
- `WebSocketAuthInterceptor`：WebSocket认证拦截器
- `WebSocketChannelInterceptor`：WebSocket通道拦截器
- `WebSocketEventListener`：WebSocket事件监听器

---

### 10. 切面 (`aspect`)

**位置**：`com.baoma.natural_language_query.aspect`

**功能**：AOP切面，提供横切关注点功能。

**主要切面**：
- `PermissionAspect`：权限切面，用于方法级别的权限控制

---

### 11. 注解 (`annotation`)

**位置**：`com.baoma.natural_language_query.annotation`

**功能**：自定义注解，用于标记和配置。

**主要注解**：
- `RequirePermission`：权限要求注解，标记需要权限验证的方法

---

### 12. 异常处理 (`exception`)

**位置**：`com.baoma.natural_language_query.exception`

**功能**：异常定义和全局异常处理。

**主要类**：
- `BusinessException`：业务异常类
- `GlobalExceptionHandler`：全局异常处理器

---

### 13. 枚举 (`enums`)

**位置**：`com.baoma.natural_language_query.enums`

**功能**：枚举类型定义。

**主要枚举**：
- `Role`：角色枚举

---

### 14. 通用类 (`common`)

**位置**：`com.baoma.natural_language_query.common`

**功能**：通用响应类和工具类。

**主要类**：
- `Result`：统一响应结果封装类

---

## 核心业务流程

### 1. 用户认证流程

```
用户登录 → AuthController.login() 
→ AuthService.login() 
→ Spring Security认证 
→ 生成JWT Token 
→ 返回Token和用户信息
```

### 2. 自然语言查询流程

```
用户输入自然语言查询 → QueryController.executeQuery() 
→ QueryService.executeQuery() 
→ 获取数据库元数据 (DatabaseSchemaService)
→ 调用大模型生成SQL (LLMService)
→ 提取SQL中的表名 (SqlTableExtractor)
→ 检查用户权限 (TablePermissionService)
→ 执行SQL查询 (DynamicDatabaseExecutor)
→ 转换查询结果为表格和图表数据
→ 保存对话记录到MongoDB (DialogService)
→ 记录查询日志 (QueryLogService)
→ 返回查询结果
```

### 3. 权限验证流程

```
请求到达 → JwtInterceptor拦截 
→ 验证Token有效性 
→ PermissionAspect切面 
→ 检查用户权限 (PermissionUtil)
→ 执行目标方法或抛出权限异常
```

### 4. WebSocket实时通信流程

```
客户端连接WebSocket → WebSocketAuthInterceptor认证 
→ WebSocketChannelInterceptor拦截 
→ ChatWebSocketController处理消息 
→ FriendChatService处理业务逻辑 
→ 保存聊天记录到MongoDB 
→ 推送消息给目标用户
```

---

## 数据库设计

### MySQL数据库（关系型数据）

存储用户、角色、权限、数据库连接、元数据、日志等结构化数据。

**主要表**：
- `user`：用户表
- `role`：角色表
- `db_connection`：数据库连接表
- `table_metadata`：表元数据表
- `column_metadata`：字段元数据表
- `user_db_permission`：用户数据库权限表
- `query_log`：查询日志表
- `query_share`：查询分享表
- `notification`：通知表
- `friend_relation`：好友关系表
- `friend_request`：好友请求表
- `llm_config`：大模型配置表
- `system_health`：系统健康表
- `performance_metric`：性能指标表
- `error_log`：错误日志表
- `operation_log`：操作日志表
- `token_consume`：Token消费记录表

### MongoDB数据库（非关系型数据）

存储对话记录、聊天记录、缓存等非结构化数据。

**主要集合**：
- `dialog_record`：对话记录集合
- `dialog_detail`：对话详情集合
- `collection_record`：收藏记录集合
- `query_collection`：查询收藏集合
- `sql_cache`：SQL缓存集合
- `friend_chat`：好友聊天集合
- `ai_interaction_log`：AI交互日志集合

---

## 安全机制

1. **JWT认证**：使用JWT Token进行用户认证，Token包含用户ID和用户名
2. **权限控制**：基于角色的访问控制（RBAC），支持表级别的权限控制
3. **密码加密**：使用Spring Security的BCryptPasswordEncoder加密密码
4. **Token黑名单**：支持Token失效和单点登录
5. **SQL注入防护**：使用参数化查询和SQL验证
6. **跨域配置**：配置CORS允许跨域请求

---

## 部署说明

1. **环境要求**：
   - JDK 21
   - MySQL 8.0+
   - MongoDB 4.0+
   - Docker（可选）

2. **配置文件**：
   - `application.yml`：应用配置文件，包含数据库连接、Redis配置、大模型API配置等

3. **启动方式**：
   - 直接运行：`NaturalLanguageQueryApplication.main()`
   - Maven命令：`mvn spring-boot:run`
   - Docker部署：构建Docker镜像并运行容器

---

## 版本信息

- **版本**：2.0
- **作者**：神奇宝码队
- **日期**：2025

---

## 注意事项

1. 确保MySQL和MongoDB数据库已正确配置并启动
2. 配置大模型API密钥和端点地址
3. 根据实际需求配置Redis（用于Token黑名单等）
4. 生产环境建议配置HTTPS和安全策略
5. 定期备份数据库数据

---

## 联系方式

如有问题或建议，请联系开发团队。

