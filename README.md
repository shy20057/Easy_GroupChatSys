# Easy_GroupChatSys

**基于 Java NIO + Netty + WebSocket 的高性能即时通讯系统**

![Java](https://img.shields.io/badge/Java-17%2B-blue)
![Netty](https://img.shields.io/badge/Netty-4.x-orange)
![NIO](https://img.shields.io/badge/NIO-Non--blocking_I_O-red)
![WebSocket](https://img.shields.io/badge/WebSocket-Real--time-green)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F)
![Vue.js](https://img.shields.io/badge/Vue.js-3.x-4FC08D)
![Electron](https://img.shields.io/badge/Electron-Latest-47848F)

## 项目简介

Easy_GroupChatSys 是一款**以高性能网络通信为核心**的即时通讯系统。项目深度运用 **Java NIO、Netty 框架和 WebSocket 协议**构建底层通信架构，实现了低延迟、高并发的实时消息推送能力。

系统采用前后端分离设计：
- **后端核心**：基于 Netty 的非阻塞 I/O 模型处理海量并发连接
- **通信协议**：自定义 WebSocket 消息格式，支持文本/图片/文件/视频多类型消息
- **前端客户端**：Vue3 + Electron 跨平台桌面应用

## 核心技术亮点

### 🔥 后端通信架构（项目核心）

| 技术点 | 实现方式 | 说明 |
|--------|---------|------|
| **Java NIO** | Non-blocking I/O | 非阻塞式 I/O 多路复用，单线程处理数千连接 |
| **Netty 4.x** | Reactor 主从模型 | 高性能异步事件驱动网络框架 |
| **WebSocket** | 自定义握手协议 | 全双工实时通信，支持心跳检测与重连 |
| **Channel 管理** | ChannelGroup + Context | 连接池管理，支持在线状态同步 |
| **消息编解码** | ByteBuf + 自定义协议 | 高效的二进制序列化与反序列化 |
| **Handler 链** | Pipeline 设计模式 | 心跳、鉴权、消息分发等处理器链 |

### 📡 WebSocket 服务特性

```
┌─────────────────────────────────────────────┐
│           Netty WebSocket Server            │
│              (Port: 5051)                   │
├─────────────────────────────────────────────┤
│  Handler Chain:                             │
│  ┌───────────┐   ┌───────────┐             │
│  │ HeartBeat │ → │ AuthCheck │ → Message   │
│  │ Handler   │   │ Handler   │ │ Handler    │
│  └───────────┘   └───────────┘ └───────────┘
│                                             │
│  Features:                                  │
│  • 心跳保活机制                              │
│  • 用户在线状态管理                           │
│  • 消息可靠投递                              │
│  • 断线自动重连                              │
└─────────────────────────────────────────────┘
```

### 🎯 系统功能模块

#### 1. **用户服务层**
- 注册登录 / JWT Token 鉴权
- 个人资料 CRUD / 头像上传
- Redis Session 管理

#### 2. **好友关系系统**
- 好友搜索 / 申请审批流程
- 关系状态机（待确认/已同意/已拉黑）
- 好友列表缓存优化

#### 3. **群组管理**
- 创建群聊 / 邀请成员
- 群信息维护 / 权限控制
- 群消息广播（Netty Group 发送）

#### 4. **消息中心**
- **消息类型**：文本 / 图片(Base64) / 文件 / 视频
- **存储策略**：MySQL 持久化 + SQLite 本地缓存
- **未读计数**：Redis 原子计数器
- **已读回执**：实时更新会话状态

#### 5. **会话管理**
- 会话创建 / 更新 / 删除
- 置顶排序 / 最后消息时间戳
- 本地 SQLite 同步

---

## 技术架构图

```
┌──────────────────────────────────────────────────────────────┐
│                     Client Layer                             │
│         Electron Desktop App (Vue3 + Element Plus)          │
│              ↓ WebSocket Client (wsClient.js)                │
├──────────────────────────────────────────────────────────────┤
│                  Network Transport Layer                      │
│         TCP + WebSocket Protocol (Port: 5051)                │
│              ↓ Netty Bootstrap + EventLoopGroup               │
├──────────────────────────────────────────────────────────────┤
│                 Server Core Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  Connection │  │   Message   │  │   Heartbeat │         │
│  │  Manager    │  │  Dispatcher │  │  Detector   │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
├──────────────────────────────────────────────────────────────┤
│               Business Service Layer                          │
│  UserService │ ContactService │ ChatService │ GroupService  │
├──────────────────────────────────────────────────────────────┤
│                  Data Access Layer                            │
│        MyBatis Mapper ↔ MySQL 8.0  |  Redis Cache           │
└──────────────────────────────────────────────────────────────┘
```

---

## 快速开始

### 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 必须支持 NIO |
| Node.js | 18+ | 前端构建工具 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.0+ | 缓存 & 会话存储 |

### 启动步骤

```bash
# 1️⃣ 启动后端（Netty WebSocket 服务）
cd 04-easychat-java
mvn clean compile -DskipTests
mvn spring-boot:run
# 后端启动后监听:
#   • HTTP API: http://localhost:5050
#   • WebSocket: ws://localhost:5051/ws
#   • API文档: http://localhost:5050/doc.html

# 2️⃣ 启动前端桌面应用
cd 04-easychat-web/easychat-front
npm install
npm run dev
```

---

## 项目结构

```
Easy_GroupChatSys/
│
├── 04-easychat-java/                    # 🔧 后端核心（重点）
│   └── src/main/java/com/easychat/
│       ├── websocket/netty/             # ⭐ Netty WebSocket 实现
│       │   ├── NettyWebSocketStarter.java    # Netty 启动引导类
│       │   ├── HandlerWebSocket.java         # WebSocket 处理器
│       │   ├── HandlerHeartBeat.java         # 心跳处理器
│       │   └── ChannelContextUtils.java      # 连接上下文管理
│       │
│       ├── service/impl/                 # 业务逻辑层
│       │   ├── ChatMessageServiceImpl.java # 消息服务（含 WS 推送）
│       │   └── UserServiceImpl.java
│       │
│       ├── redis/                       # Redis 组件
│       │   ├── RedisComponent.java           # Redis 配置
│       │   └── RedisUtils.java              # 工具类
│       │
│       └── controller/                  # REST API 控制器
│           └── user/ChatController.java
│
├── 04-easychat-web/easychat-front/      # 💻 Electron 桌面客户端
│   └── src/
│       ├── main/
│       │   ├── wsClient.js              # ⭐ WebSocket 客户端实现
│       │   ├── ipc.js                   # IPC 进程间通信
│       │   └── db/                      # SQLite 本地数据库
│       │
│       └── renderer/src/                # Vue3 渲染进程
│           └── views/chat/              # 聊天界面组件
│
└── README.md
```

---

## 后端核心代码示例

### Netty WebSocket 服务器启动

```java
// NettyWebSocketStarter.java
public class NettyWebSocketStarter {
    
    public void start(int port) {
        // Boss 线程组：接受客户端连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // Worker 线程组：处理 I/O 操作
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)  // 使用 NIO 通道
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            // HTTP 编解码器
                            pipeline.addLast(new HttpServerCodec());
                            // 聚合器（将 HTTP 消息聚合成 FullHttpRequest）
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            // WebSocket 协议升级处理器
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                            // 自定义业务处理器
                            pipeline.addLast(new HandlerWebSocket());
                        }
                    });
            
            // 绑定端口，开始监听
            ChannelFuture future = bootstrap.bind(port).sync();
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

### WebSocket 消息处理

```java
// HandlerWebSocket.java
@ChannelHandler.Sharable
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String message = frame.text();
        
        // 解析 JSON 消息
        JsonObject msgJson = JsonUtils.parse(message);
        String messageType = msgJson.getString("messageType");
        
        switch (messageType) {
            case "CHAT": 
                handleChatMessage(ctx, msgJson);  // 处理聊天消息
                break;
            case "HEARTBEAT":
                handleHeartbeat(ctx);             // 处理心跳
                break;
            default:
                ctx.writeAndFlush(new TextWebSocketFrame("Unknown message type"));
        }
    }
}
```

---

## 性能特点

| 指标 | 说明 |
|------|------|
| **I/O 模型** | Non-blocking I/O (NIO)，单线程处理多连接 |
| **并发能力** | 基于 Netty Reactor 模型，支持万级并发 |
| **消息延迟** | < 100ms（局域网环境） |
| **连接管理** | ChannelGroup 统一管理，支持广播 |
| **心跳机制** | 定时 Ping/Pong 保活，自动清理死连接 |
| **断线重连** | 客户端指数退避重连策略 |

---

## 开发说明

### 关键端口

| 服务 | 端口 | 协议 |
|------|------|------|
| Spring Boot HTTP API | 5050 | HTTP |
| Netty WebSocket | 5051 | WebSocket |
| 本地文件服务 | 10341+ | HTTP |
| MySQL | 3306 | TCP |
| Redis | 6379 | TCP |

### 数据库表结构

- `chat_message` - 消息记录（主键: user_id + message_id）
- `chat_session_user` - 会话列表（本地 SQLite）
- `user_contact` - 好友关系
- `group_info` - 群组信息
- `user_contact_apply` - 好友申请记录

---

## 许可证

MIT License - 查看 [LICENSE](LICENSE) 文件了解详情

---

> 💡 **本项目适合学习 Java 网络编程、Netty 框架、WebSocket 实时通信等后端核心技术**
>
> ⭐ 如果这个项目对你有帮助，欢迎 Star 支持！
