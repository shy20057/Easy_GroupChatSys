package com.easychat.websocket.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable // Netty注解，标记该处理器可以在多个Channel之间共享使用  Duplex??? 哈哈哈 我想起来了
public class HandlerHeartBeat extends ChannelDuplexHandler { // ChannelDuplexHandler 是一个同时处理入站（inbound）和出站（outbound）事件的 ChannelHandler 实现

    private static final Logger logger = LoggerFactory.getLogger(HandlerHeartBeat.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // ctx是通道处理器上下文，evt是触发的事件对象
        if (evt instanceof IdleStateEvent) { // IdleStateEvent : Netty的空闲状态事件
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) { // 读超时：客户端长时间没发消息，断开连接
                Channel channel = ctx.channel(); // 获取当前通道Channel实例
                // 通过 通道ID 获取 存储在 通道属性 中的 用户ID   潜在问题 这里应该定义一个固定的常量键名去存储数据
                Attribute<String> attribute =  channel.attr(AttributeKey.valueOf(channel.id().toString()));
                String userId = attribute.get();
                logger.info("客户端用户{}心跳超时，断开连接", userId);
                ctx.close(); // 关闭连接
            } else if (e.state() == IdleState.WRITER_IDLE) { // 写超时：服务器长时间没发消息，发送心跳
                logger.info("服务器发送心跳");
                // 关键修正：心跳消息必须封装成 TextWebSocketFrame
                ctx.writeAndFlush(new TextWebSocketFrame("heart")); // 通过ctx.writeAndFlush()发送心跳消息给客户端
            }
        } else {
            // 其他事件继续传递（比如握手事件） 如果不是空闲状态事件，则调用父类方法继续传递事件
            super.userEventTriggered(ctx, evt);
        }
    }
}