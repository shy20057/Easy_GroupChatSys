# EasyChat - 简易即时通讯系统

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.x-4FC08D)](https://vuejs.org/)
[![Electron](https://img.shields.io/badge/Electron-Latest-47848F)](https://www.electronjs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> 一个基于 Java + Vue3 + Electron 构建的跨平台即时通讯应用，支持单聊、群聊、文件传输等功能。

---

## 项目简介

EasyChat 是一个功能完整的即时通讯系统，采用前后端分离架构：

- **后端**：Spring Boot + MyBatis + Netty WebSocket + Redis
- **前端桌面端**：Vue3 + Element Plus + Electron
- **数据库**：SQLite（本地存储）+ MySQL（服务端）

---

## 功能特性

### 用户功能
- [x] 用户注册与登录
- [x] 个人资料管理
- [x] 头像上传与修改
- [x] 密码修改

### 好友系统
- [x] 搜索添加好友
- [x] 好友申请处理（同意/拒绝/拉黑）
- [x] 好友列表管理
- [x] 删除好友

### 群聊系统
- [x] 创建群聊
- [x] 群成员管理
- [x] 群信息编辑
- [x] 退出/解散群聊

### 消息系统
- [x] 实时消息收发（WebSocket）
- [x] 文本消息
- [x] 图片消息
- [x] 文件传输
- [x] 视频消息
- [x] 消息已读状态
- [x] 未读消息计数

### 会话管理
- [x] 会话列表展示
- [x] 会话置顶
- [x] 删除会话
- [x] 本地消息存储

### 其他功能
- [x] 文件管理
- [x] 系统设置
- [x] 本地文件服务

---

## 技术栈

### 后端技术 (04-easychat-java)

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | 编程语言 |
| Spring Boot | 3.x | 应用框架 |
| MyBatis | 3.x | ORM 框架 |
| Netty | 4.x | WebSocket 通信 |
| Redis | 6.x+ | 缓存与会话管理 |
| MySQL | 8.x | 主数据库 |
| Maven | 3.8+ | 构建工具 |
| Knife4j | 4.x | API 文档 |

### 前端技术 (04-easychat-web)

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 3.x | 前端框架 |
| Element Plus | 2.x | UI 组件库 |
| Electron | 最新 | 桌面应用框架 |
| Vite | 4.x | 构建工具 |
| SCSS | - | 样式预处理 |
| SQLite | 3.x | 本地数据库 |

---

## 项目结构

```
EasyChat/
├── 04-easychat-java/          # Java 后端服务
│   ├── src/main/java/         # Java 源代码
│   │   └── com/easychat/      # 主包路径
│   │       ├── controller/    # 控制器层
│   │       ├── service/       # 业务逻辑层
│   │       ├── mappers/       # 数据访问层
│   │       ├── entity/        # 实体类
│   │       ├── websocket/     # WebSocket 服务
│   │       └── utils/         # 工具类
│   ├── src/main/resources/    # 配置文件
│   │   ├── application.properties
│   │   └── com/easychat/mappers/  # MyBatis XML
│   └── pom.xml                # Maven 配置
│
├── 04-easychat-web/           # 前端桌面应用
│   └── easychat-front/
│       ├── src/
│       │   ├── main/          # Electron 主进程
│       │   │   ├── db/        # 本地数据库操作
│       │   │   ├── ipc.js     # IPC 通信
│       │   │   ├── wsClient.js # WebSocket 客户端
│       │   │   └── ...
│       │   └── renderer/      # 渲染进程（Vue 应用）
│       │       ├── src/
│       │       │   ├── views/ # 页面组件
│       │       │   ├── components/ # 公共组件
│       │       │   ├── stores/     # Pinia 状态管理
│       │       │   ├── router/     # 路由配置
│       │       │   └── utils/      # 工具函数
│       │       └── index.html
│       ├── package.json
│       └── electron.vite.config.js
│
└── README.md                  # 项目说明文档
```

---

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+

### 1. 克隆项目

```bash
git clone https://gitee.com/little-rock-star/simple-chat.git
cd simple-chat
```

### 2. 启动后端服务

```bash
cd 04-easychat-java

# 配置数据库（修改 application.properties）
# spring.datasource.url=jdbc:mysql://localhost:3306/easychat
# spring.datasource.username=root
# spring.datasource.password=your_password

# 编译并运行
mvn clean compile
mvn spring-boot:run
```

后端服务默认运行在：
- HTTP: http://localhost:5050
- WebSocket: ws://localhost:5051/ws
- API 文档: http://localhost:5050/doc.html

### 3. 启动前端应用

```bash
cd 04-easychat-web/easychat-front

# 安装依赖
npm install

# 开发模式运行
npm run dev

# 构建生产版本
npm run build
```

---

## 开发指南

### 后端开发

1. **数据库初始化**
   - 创建 MySQL 数据库 `easychat`
   - 运行项目自动创建表结构

2. **Redis 配置**
   - 确保 Redis 服务已启动
   - 默认连接本地 Redis（端口 6379）

3. **WebSocket 服务**
   - Netty WebSocket 端口：5051
   - 处理实时消息推送

### 前端开发

1. **Electron 架构**
   - 主进程：负责系统级操作、数据库、文件管理
   - 渲染进程：Vue3 应用，负责 UI 展示
   - IPC 通信：主进程与渲染进程通过 IPC 交互

2. **本地数据库**
   - SQLite 存储在：`C:\Users\{用户名}\.easychat\`
   - 包含：会话列表、消息记录、用户设置

3. **WebSocket 客户端**
   - 自动连接后端 WebSocket 服务
   - 处理消息收发、心跳检测

---

## 打包发布

### 前端桌面应用打包

```bash
cd 04-easychat-web/easychat-front

# Windows 安装包
npm run build:win

# macOS 安装包
npm run build:mac

# Linux 安装包
npm run build:linux
```

打包输出目录：`dist/`

---

## 常见问题

### Q: 后端启动报错 `NoClassDefFoundError`
A: 执行 `mvn clean compile -DskipTests` 重新编译

### Q: 前端页面空白
A: 检查 WebSocket 连接是否正常，查看控制台日志

### Q: 消息发送失败
A: 确认后端服务正常运行，检查网络连接

### Q: 头像/文件上传失败
A: 检查本地文件服务是否启动（端口 10341）

---

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 开源协议

本项目基于 [MIT](LICENSE) 协议开源。

---

## 联系方式

- 作者：小石石
- Gitee：[little-rock-star](https://gitee.com/little-rock-star)
- 项目地址：https://gitee.com/little-rock-star/simple-chat

---

> 如果这个项目对你有帮助，请给个 ⭐ Star 支持一下！
