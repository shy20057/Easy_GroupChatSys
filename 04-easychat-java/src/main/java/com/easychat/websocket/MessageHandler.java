package com.easychat.websocket;

import com.easychat.entity.dto.MessageSendDTO;
import com.easychat.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 消息处理器 做消息分发的 没有太多的业务逻辑 做了一个消息中转
 * Created by xiaohao on 2025/12/18.
 */
@Slf4j
@Component("messageHandler")
public class MessageHandler {

    private static final String MESSAGE_TOPIC = "message.topic";

    @Resource
    private RedissonClient redissonClient; // Redis客户端，用于实现发布订阅模式 导入依赖后自动注入
    @Resource
    private ChannelContextUtils channelContextUtils; // 实际发送消息到WebSocket连接的工具类

    /**
     * 负责订阅并处理消息（"处理" --> 接受订阅广播）
     */
    @PostConstruct // 用于标记一个方法在依赖注入完成后自动执行
    public void lisMessage(){
        RTopic rtopic = redissonClient.getTopic(MESSAGE_TOPIC); // 订阅名为"message.topic"的Redis主题
        // 消息监听 -- 接收到消息后，通过ChannelContextUtils.sendMessage()将消息推送到对应的WebSocket连接
        // 在Redis中创建订阅关系 当有消息发布publish时 这段代码触发 通知所有订阅者
        rtopic.addListener(MessageSendDTO.class, (MessageSendDTO, sendDTO) -> { // 第一个参数:指定期望接收的消息类型 第二个参数：回调函数 当有消息到达时执行
            log.info("接收到广播消息:{}", JsonUtils.convertObj2Json(sendDTO)); // 讲对象序列化为字符串 便于查看
            channelContextUtils.sendMessage(sendDTO);
        });
    }

    /**
     * 负责发布消息
     */
    // 封装ChannelContextUtils.java和MessageHandler.java有关集群发送消息的方法
    // 对外提供统一的消息发送入口
    public void sendMessage(MessageSendDTO sendDTO){
        /*edisson内部会根据topic名称在Redis中创建对应的key，格式通常为{topic_name} 如果该topic已存在则直接返回引用，否则创建新的topic实例*/
        RTopic rtopic = redissonClient.getTopic(MESSAGE_TOPIC); // 通过Redisson客户端获取指定主题的RTopic实例
        rtopic.publish(sendDTO); // 将消息序列化后发布到Redis的Pub/Sub通道

    }

    /*
    * 消息发布机制：
             rtopic.publish(sendDTO) 将消息序列化后发布到Redis的Pub/Sub通道
             Redisson会将MessageSendDTO对象通过JSON或其他序列化方式转换为 字节数组（字符串）
             使用Redis的PUBLISH命令将消息推送到 对应频道
             所有 订阅 该频道的客户端都会收到这条消息
    * */
}
