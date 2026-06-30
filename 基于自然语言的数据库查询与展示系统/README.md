# 自然语言数据库查询系统 - 项目总说明

## 项目概述

**自然语言数据库查询系统**是一个基于大语言模型的智能数据库查询平台，支持用户通过自然语言描述查询需求，系统自动调用大模型解析查询意图，生成并执行SQL语句，将查询结果以图表形式可视化展示。

### 核心特性

- **智能查询**：采用自然语言描述查询请求，系统调用大模型感知数据库元数据并解析用户查询请求
- **权限管理**：完善的用户权限控制系统，管理员可以管理和授权用户，支持表级别的权限控制
- **SQL生成与验证**：采用SQL引擎生成和验证SQL语句，确保查询安全性和准确性
- **数据可视化**：查询结果采用Chart图表显示，支持多种图表类型
- **多轮对话**：支持用户调整查询内容，系统根据用户调整请求修改SQL语句，重新查询和可视化查询结果
- **复杂查询支持**：支持多表联合等复杂SQL语句查询
- **并发支持**：支持至少5个及以上并发用户，每个用户的任务在Docker容器独立运行
- **社交功能**：支持查询分享、好友关系、实时聊天等社交功能

---

## 技术架构

### 后端技术栈

- **框架**：Spring Boot 3.5.7
- **开发语言**：Java 21 (JDK 21)
- **关系型数据库**：MySQL 8.0+
- **非关系型数据库**：MongoDB 4.0+
- **ORM框架**：MyBatis Plus 3.5.9
- **安全框架**：Spring Security + JWT
- **缓存**：Redis
- **实时通信**：WebSocket
- **容器化**：Docker
- **构建工具**：Maven

### 前端技术栈

- **框架**：Vue 3.5.25
- **开发语言**：TypeScript 5.9
- **构建工具**：Vite 7.2.4
- **路由**：Vue Router 4.6.3
- **状态管理**：Pinia 3.0.4
- **UI框架**：Tailwind CSS 4.1.18
- **图表库**：Chart.js 4.5.1 + Vue-ChartJS 5.3.3
- **WebSocket**：SockJS + STOMP.js

### 系统架构

```
┌─────────────┐
│  前端应用    │ Vue 3 + TypeScript
│ (Vite)      │
└──────┬──────┘
       │ HTTP/WebSocket
┌──────▼─────────────────┐
│   后端应用              │
│ (Spring Boot)          │
│                        │
│ ┌──────────────────┐   │
│ │  Controller层    │   │ RESTful API
│ └────────┬─────────┘   │
│ ┌────────▼─────────┐   │
│ │   Service层      │   │ 业务逻辑
│ └────────┬─────────┘   │
│ ┌────────▼─────────┐   │
│ │  Mapper/Repository│  │ 数据访问
│ └────────┬─────────┘   │
└──────────┼─────────────┘
           │
    ┌──────┼──────┐
    │      │      │
┌───▼──┐ ┌▼──┐ ┌─▼────┐
│MySQL │ │Mongo│ │Redis│
└──────┘ └───┘ └──────┘
```

---

## 项目目录结构

```
xm/
├── doc/                          # 文档目录
│   ├── README.md                 # 项目总说明（本文件）
│   ├── process/                  # 开发过程文档
│   │   ├── meeting/              # 会议记录
│   │   └── weekly/               # 周报（按周组织）
│   └── project/                  # 项目文档
│       ├── 01-需求文档/          # 需求规格说明书、用例文档等
│       ├── 02-设计文档/          # UML设计文档、数据库设计文档
│       ├── 03-计划文档/          # 迭代开发计划
│       ├── 04-用户手册/          # 用户使用手册
│       └── 05-测试报告/          # 测试报告
│
├── src/                          # 源代码目录
│   ├── frontend/                 # 前端项目
│   │   ├── src/
│   │   │   ├── components/       # Vue组件
│   │   │   ├── views/            # 页面视图
│   │   │   ├── router/           # 路由配置
│   │   │   ├── services/         # API服务
│   │   │   ├── composables/      # Composition API
│   │   │   ├── types/            # TypeScript类型定义
│   │   │   └── utils/            # 工具函数
│   │   ├── package.json          # 前端依赖配置
│   │   └── vite.config.ts        # Vite配置
│   │
│   ├── src/                      # 后端项目
│   │   └── main/
│   │       ├── java/             # Java源代码
│   │       │   └── com/example/springboot_demo/
│   │       │       ├── controller/    # 控制器层
│   │       │       ├── service/       # 服务层
│   │       │       ├── mapper/        # MyBatis Mapper
│   │       │       ├── repository/    # MongoDB Repository
│   │       │       ├── entity/        # 实体类
│   │       │       ├── dto/           # 数据传输对象
│   │       │       ├── vo/            # 视图对象
│   │       │       ├── config/        # 配置类
│   │       │       ├── utils/         # 工具类
│   │       │       └── exception/     # 异常处理
│   │       └── resources/
│   │           └── application.yml    # 应用配置文件
│   │
│   ├── pom.xml                   # Maven配置文件
│   └── README.md                 # 后端模块说明文档
│
└── scripts/                      # 脚本目录
    ├── export-data.sh/.bat       # 数据导出脚本
    └── import-data.sh/.bat       # 数据导入脚本
```

---

## 快速开始

### 环境要求

- **JDK**：21 或更高版本
- **Node.js**：^20.19.0 或 >=22.12.0
- **MySQL**：8.0 或更高版本
- **MongoDB**：4.0 或更高版本
- **Redis**：最新稳定版
- **Maven**：3.6+（用于后端构建）
- **Docker**：可选（用于容器化部署）

### 后端启动

1. **配置数据库连接**
   
   编辑 `src/src/main/resources/application.yml`，配置MySQL、MongoDB和Redis连接信息。

2. **构建项目**
   ```bash
   cd src
   mvn clean install
   ```

3. **启动应用**
   ```bash
   mvn spring-boot:run
   ```
   
   或直接运行主类：`com.example.springboot_demo.NaturalLanguageQueryApplication`

4. **验证启动**
   
   访问 `http://localhost:8173/actuator/health` 检查应用健康状态

### 前端启动

1. **安装依赖**
   ```bash
   cd src/frontend
   npm install
   ```

2. **启动开发服务器**
   ```bash
   npm run dev
   ```

3. **构建生产版本**
   ```bash
   npm run build
   ```

4. **访问应用**
   
   开发环境：`http://localhost:5173`
   生产环境：根据部署配置访问

### Docker部署

项目支持Docker容器化部署，具体配置请参考 `src/docker-compose.yml`。

---

## 文档导航

### 需求与设计文档

- **[需求文档](project/01-需求文档/README.md)**：包含需求规格说明书、用例文档、项目前景与范围文档
- **[设计文档](project/02-设计文档/README.md)**：包含UML设计文档（活动图、顺序图、类图）和数据库设计文档
- **[计划文档](project/03-计划文档/README.md)**：包含迭代开发计划

### 用户与测试文档

- **[用户手册](project/04-用户手册/README.md)**：用户使用指南和操作说明
- **[测试报告](project/05-测试报告/README.md)**：系统测试报告和测试用例

### 开发过程文档

- **[会议记录](process/meeting/)**：项目开发过程中的会议纪要
- **[周报](process/weekly/)**：按周组织的开发计划和总结

### 技术文档

- **[后端模块说明](../src/README.md)**：详细的后端架构和模块说明

---

## 核心功能模块

### 1. 用户认证与授权

- 用户注册、登录、密码重置
- JWT Token认证
- 基于角色的访问控制（RBAC）
- 表级别的权限控制

### 2. 自然语言查询

- 自然语言转SQL
- 数据库元数据感知
- SQL生成与验证
- 多轮对话支持
- 查询结果可视化

### 3. 数据库管理

- 数据库连接管理
- 表元数据管理
- 字段元数据管理
- 数据库结构导出

### 4. 查询管理

- 查询日志记录
- 查询收藏
- 查询分享
- SQL缓存
- 对话历史记录

### 5. 社交功能

- 好友关系管理
- 实时聊天（WebSocket）
- 查询分享
- 通知系统

### 6. 系统管理

- 大模型配置管理
- 系统健康监控
- 性能指标统计
- 错误日志管理
- 操作日志审计

---

## 安全机制

1. **JWT认证**：使用JWT Token进行用户认证，Token包含用户ID和用户名
2. **权限控制**：基于角色的访问控制（RBAC），支持表级别的权限控制
3. **密码加密**：使用Spring Security的BCryptPasswordEncoder加密密码
4. **Token黑名单**：支持Token失效和单点登录
5. **SQL注入防护**：使用参数化查询和SQL验证
6. **跨域配置**：配置CORS允许跨域请求

---

## 测试

### 单元测试

后端测试使用JUnit，前端测试使用Vitest。

```bash
# 后端测试
cd src
mvn test

# 前端测试
cd src/frontend
npm run test:unit
```

### API测试

可以使用 `src/api-test.http` 文件进行API接口测试（支持IntelliJ IDEA HTTP Client或VS Code REST Client）。

---

## 系统性能

- **并发支持**：支持至少5个及以上并发用户
- **响应时间**：SQL查询响应时间 < 3秒（取决于数据量和数据库性能）
- **可用性**：系统可用性 > 99%

---

## 开发规范

### 代码规范

- **后端**：遵循Java编码规范，使用Lombok简化代码
- **前端**：遵循Vue 3 + TypeScript编码规范，使用ESLint和Prettier进行代码格式化

### Git提交规范

建议使用以下格式：
```
<type>(<scope>): <subject>

<body>

<footer>
```

类型（type）：
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具链相关

---

## 开发团队

**团队名称**：神奇宝码队

**项目版本**：2.0

**开发时间**：2025

---

## 更新日志

详细的版本更新记录请参考各迭代开发计划和周报文档。

---

## 贡献指南

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交Pull Request

---

## 许可证

本项目遵循相应的开源许可证。

---

## 联系方式

如有问题或建议，请联系开发团队。

---

## 未来规划

- [ ] 支持更多数据库类型（PostgreSQL、Oracle等）
- [ ] 增强大模型提示词工程，提高SQL生成准确性
- [ ] 优化查询性能，支持更大数据量查询
- [ ] 增强数据可视化功能，支持更多图表类型
- [ ] 增加数据分析功能

---

*最后更新时间：2025年*

