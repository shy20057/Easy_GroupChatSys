package com.easychat.websocket.netty;


import com.easychat.entity.config.Appconfig;
import com.easychat.utils.StringTools;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyWebSocketStarter implements Runnable{

    /*
    * Netty是一个基于Java NIO的异步事件驱动的网络应用框架，用于快速开发可维护的高性能协议服务器和客户端
    * EventLoopGroup：事件循环组，包含多个EventLoop线程
    * bossGroup：负责接收连接请求
    * workerGroup：负责处理已建立连接的I/O操作
    * Channel：网络连接的抽象，代表一个打开的连接
    * ChannelPipeline：处理器链，用于处理入站和出站数据
    * Handler：业务逻辑处理器，分为ChannelInboundHandler和ChannelOutboundHandler
    * */


    private  EventLoopGroup bossGroup = new NioEventLoopGroup(1); // bossGroup作为主Reactor，专门处理连接请求

    private  EventLoopGroup workGroup = new NioEventLoopGroup(); // workGroup作为从Reactor，专门处理已建立连接的I/O操作

    @Resource
    private Appconfig appConfig;

    @Resource
    private HandlerWebSocket handlerWebSocket;
    @PreDestroy // 销毁前回调方法
    public void close(){
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }

    @Override
    public void run() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap(); // 创建ServerBootstrap对象，它是Netty用于启动NIO服务端的辅助启动类
            serverBootstrap.group(bossGroup, workGroup); // 绑定两个NIO线程组
            serverBootstrap.channel(NioServerSocketChannel.class) // 指定使用的通道类型为 NioServerSocketChannel（异步IO模式）
                    .handler(new LoggingHandler(LogLevel.DEBUG))  // .handler()：设置 服务端channel的处理器，使用 LoggingHandler 记录日志
                    .childHandler(new ChannelInitializer() { // .childHandler()：设置 客户端channel的处理器，使用匿名内部类 ChannelInitializer
                        @Override
                        protected void initChannel(Channel channel) throws Exception { // 重写 initChannel 方法，当新的客户端连接建立时被调用
                            ChannelPipeline pipeline = channel.pipeline(); // 获取channel的处理链 ChannelPipeline
                            // 设置几个重要的处理器
                            // 对http协议的支持，使用http的编码器，解码器
                            pipeline.addLast(new HttpServerCodec());
                            // 聚合解码 httpRequest/httpContent/lastHttpContent到FullHttpRequest
                            // 添加HTTP对象聚合器，将HTTP请求聚合成完整的FullHttpRequest对象
                            pipeline.addLast(new HttpObjectAggregator(64*1024));
                            // 心跳  long readerIdleTime, long writerIdleTime, long allIdleTime（总超时时间）, TimeUnit unit(单位)
                            // readerIdleTime：读超时，即测试端一定时间内未接收到被测试端的消息，超过这个时间没有读数据，就触发一次IdleState.READER_IDLE事件
                            // writerIdleTime：写超时，即测试端一定时间向被测试端发送消息，超过这个时间没有写数据，就触发一次IdleState.WRITER_IDLE事件
                            // allIdleTime：总超时，超过这个时间没有读或写数据，就触发一次IdleState.ALL_IDLE事件
                            pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                            // 心跳处理器
                            pipeline.addLast(new HandlerHeartBeat());
                            // 添加WebSocket协议处理器 将http协议升级为ws协议，对websocket的支持
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws",null, true,65536, true, true,10000L));
                            pipeline.addLast(handlerWebSocket);
                        }
                    });

            Integer wsPort = appConfig.getWsPort();
            String wsPortStr = System.getProperty("wsPort");
            if(!StringTools.isEmpty(wsPortStr)){
                wsPort = Integer.parseInt(wsPortStr);
            }
            ChannelFuture channelFuture = serverBootstrap.bind(wsPort).sync();  // 绑定端口  sync是一个1保证同步的方法
            log.info("netty服务启动成功，端口:{}",appConfig.getWsPort());
            channelFuture.channel().closeFuture().sync(); // 等待服务端channel关闭，这会使线程阻塞直到服务被关闭

        } catch (Exception e) {
            log.error("启动netty失败", e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }


}
