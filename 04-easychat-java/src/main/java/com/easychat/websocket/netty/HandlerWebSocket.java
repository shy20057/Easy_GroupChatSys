package com.easychat.websocket.netty;

import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.redis.RedisComponent;
import com.easychat.utils.StringTools;
import com.easychat.websocket.ChannelContextUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@ChannelHandler.Sharable       //  专门用于处理入站事件和数据
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Resource
    private RedisComponent redisComponent;
    @Resource
    private ChannelContextUtils channelContextUtils;

    // 通道就绪后调用（客户端连接成功）
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("有新的连接加入，通道ID：{}", ctx.channel().id());
        super.channelActive(ctx); // 确保父类方法被调用，保证事件传递
    }

    // 通道关闭时调用（客户端断开连接）
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("连接断开，通道ID：{}", ctx.channel().id());
        super.channelInactive(ctx); // 确保父类方法被调用

    channelContextUtils.removeContext(ctx.channel());
    }

    // 接收客户端消息
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame textWebSocketFrame) throws Exception {
        Channel channel = ctx.channel(); // channel 上下文 在连接之间实现数据的传输
        String message = textWebSocketFrame.text();

        // 可以添加消息处理逻辑（比如回复消息）
        //ctx.writeAndFlush(new TextWebSocketFrame("服务器已收到消息：" + message));

        // attr()方法 获取channel中自定义的AttributeKey的值
        Attribute<String> attribute =  channel.attr(AttributeKey.valueOf(channel.id().toString()));

        String userId = attribute.get();
        //log.info("收到用户{}通道{}的消息：{}", userId,channel.id(), message);
        redisComponent.saveHeartBeat(userId); // 保存用户心跳
    }

    // 处理用户事件（握手、心跳等）
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) { // 判断事件是否为WebSocket握手完成事件
            WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String url = handshakeComplete.requestUri(); // 获取连接的URL（包含参数）
            log.info("WebSocket握手完成，URL：{}", url);

            // 解析Token（你的现有逻辑）
            String token = getToken(url); // 从URL中获取Token
            if (token == null) {
                log.warn("连接{}握手失败：未携带Token", ctx.channel().id());
                ctx.channel().close(); // 未携带Token，关闭连接
                return;
            }

            TokenUserInfoDTO tokenUserInfoDTO = redisComponent.getTokenUserInfoDTO(token);
            if (tokenUserInfoDTO == null) {
                log.warn("连接{}握手失败：Token无效", ctx.channel().id());
                ctx.channel().close(); // Token无效，关闭连接
                return;
            }

            // 握手成功后的逻辑（比如存储用户与通道的映射关系）
            log.info("连接{}握手成功，用户ID：{}", ctx.channel().id(), tokenUserInfoDTO.getUserId());

            channelContextUtils.addContext(tokenUserInfoDTO.getUserId(), ctx.channel());

        } else {
            // 关键修正：其他事件（如心跳事件）继续传递给下一个handler
            super.userEventTriggered(ctx, evt);
        }
    }

    // 解析URL中的Token参数  前端必须得当参数带过来
    private String getToken(String url) {
        if (StringTools.isEmpty(url) || url.indexOf("?") == -1) {
            return null;
        }
        String query = url.split("\\?")[1]; // 获取查询参数部分（如 "token=xxx"）
        String[] params = query.split("=");
        if (params.length == 2 && "token".equals(params[0])) {
            return params[1]; // 返回Token值
        }
        return null;
    }

    // 处理异常（关键：避免异常导致连接断开）
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("连接{}发生异常：{}", ctx.channel().id(), cause.getMessage(), cause);
        ctx.close(); // 发生异常时关闭连接
    }
}