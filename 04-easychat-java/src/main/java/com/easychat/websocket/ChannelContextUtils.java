    package com.easychat.websocket;

    import com.easychat.entity.constants.Constants;
    import com.easychat.entity.dto.MessageSendDTO;
    import com.easychat.entity.dto.WsInitData;
    import com.easychat.entity.enums.MessageTypeEnum;
    import com.easychat.entity.enums.UserContactApplyStatusEnum;
    import com.easychat.entity.enums.UserContactTypeEnum;
    import com.easychat.entity.po.ChatMessage;
    import com.easychat.entity.po.ChatSessionUser;
    import com.easychat.entity.po.User;
    import com.easychat.entity.po.UserContactApply;
    import com.easychat.entity.query.*;
    import com.easychat.mappers.ChatMessageMapper;
    import com.easychat.mappers.ChatSessionUserMapper;
    import com.easychat.mappers.UserContactApplyMapper;
    import com.easychat.mappers.UserMapper;
    import com.easychat.redis.RedisComponent;
    import com.easychat.utils.JsonUtils;
    import com.easychat.utils.StringTools;
    import io.netty.channel.Channel;
    import io.netty.channel.group.ChannelGroup;
    import io.netty.channel.group.DefaultChannelGroup;
    import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
    import io.netty.util.Attribute;
    import io.netty.util.AttributeKey;
    import io.netty.util.concurrent.GlobalEventExecutor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Component;

    import javax.annotation.Resource;
    import java.util.Date;
    import java.util.List;
    import java.util.concurrent.ConcurrentHashMap;
    import java.util.stream.Collectors;

    @Component
    @Slf4j
    public class ChannelContextUtils {
        // 用于存储用户ID与Channel的映射关系
        private static final ConcurrentHashMap<String,Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();
        // 用于存储群组ID与ChannelGroup的映射关系
        private static final ConcurrentHashMap<String, ChannelGroup> GROUP_CONTEXT_MAP = new ConcurrentHashMap<>();

        @Resource
        private  RedisComponent redisComponent;
        @Resource
        private  UserMapper<User, UserQuery> userMapper;
//        @Resource
//        @Lazy
//        private  ChatSessionUserService chatSessionUserService;

        @Resource
        private ChatSessionUserMapper<ChatSessionUser,ChatSessionUserQuery> chatSessionUserMapper;
        @Resource
        private ChatMessageMapper<ChatMessage,ChatMessageQuery> chatMessageMapper;
        @Resource
        private UserContactApplyMapper<UserContactApply,UserContactApplyQuery> userContactApplyMapper;





        // 添加用户通道  userId：由token获取的用户 就是登录用户
        public void addContext(String userId, Channel channel){
            String channelId = channel.id().toString();
            log.info("添加用户{}通道{}", userId, channelId);
            //  AttributeKey 通道的属性 可用于存储数据 如：用户ID
            AttributeKey attributeKey = null;
            if(!AttributeKey.exists(channelId)){ // 判断channelId的通道属性是否存在
                attributeKey = AttributeKey.newInstance(channelId); // 不存在则创建
            }else{
                attributeKey = AttributeKey.valueOf(channelId); // 存在则获取
            }

            // 将 用户ID 存储到 通道的属性 中 全局可用
            channel.attr(attributeKey).set(userId);

            List<String> contactIdList = redisComponent.getContactList(userId); // 从Redis中获取当前用户的所有联系人ID列表 包括好友和群组
            contactIdList = JsonUtils.convertArray2List(contactIdList);

            for (String groupId : contactIdList) { // 先处理群组 添加到channel
              if(groupId.startsWith(UserContactTypeEnum.GROUP.getPrefix())){ // 如果是群组则添加到
                  add2Group(groupId,channel);
              }
            }

            USER_CONTEXT_MAP.put(userId, channel); // 将登录用户自己添加到channel
            redisComponent.saveHeartBeat(userId); // 保存用户心跳 存一次心跳

//            String groupId = "10000";
//            add2Group(groupId,channel);

            // 更新用户最后连接时间
            User update = new User();
            update.setLastLoginTime(new Date());
            userMapper.updateByUserId(update, userId);

            // 给用户发消息
            User user = userMapper.selectByUserId(userId);
            Long sourceLastOffTime = user.getLastOffTime();
            Long lastOffTime = sourceLastOffTime;
            if(sourceLastOffTime != null && (System.currentTimeMillis() - Constants.MILLISECONDS_3DAYS_AGO) > sourceLastOffTime){
                lastOffTime = Constants.MILLISECONDS_3DAYS_AGO; // 如果用户离线时间超过3天，则将离线时间设置为3天前 也就是说我最多查离线三天的会话数据
            }

            /*
            *  1 查询会话信息 查询用户所有的会话信息 保证换了设备会话同步
            * */

            ChatSessionUserQuery sessionUserQuery = new ChatSessionUserQuery();
            sessionUserQuery.setUserId(user.getUserId()); // 登录用户的ID
            sessionUserQuery.setOrderBy("last_receive_time desc");
            List<ChatSessionUser> ChatsessionUserList = chatSessionUserMapper.selectList(sessionUserQuery);
            for (ChatSessionUser chatSessionUser : ChatsessionUserList){
                if(chatSessionUser.getContactId().substring(0,1).equals(UserContactTypeEnum.GROUP.getPrefix())){
                     chatSessionUser.setContactType(UserContactTypeEnum.GROUP.getType());
                }else{
                     chatSessionUser.setContactType(UserContactTypeEnum.USER.getType());
                }
            }

            WsInitData wsInitData = new WsInitData();
            wsInitData.setChatSessionList(ChatsessionUserList); // 会话列表√  消息列表  申请列表

            /*
            *  2 查询聊天消息
            * */
            // 查询所有的联系人

            List<String> groupIdList = contactIdList.stream().filter(item-> item.startsWith(UserContactTypeEnum.GROUP.getPrefix())).collect(Collectors.toList());
            groupIdList.add(userId);  // 添加自己
            ChatMessageQuery chatMessageQuery = new ChatMessageQuery();
            chatMessageQuery.setContactIdList(contactIdList);
            chatMessageQuery.setLastReceiveTime(lastOffTime);

            List<ChatMessage> chatMessageList = chatMessageMapper.selectList(chatMessageQuery);

            wsInitData.setChatMessageList(chatMessageList); // 会话列表  消息列表√  申请列表



            /*
            *  3 查询好友申请
            * */
            UserContactApplyQuery applyQuery = new UserContactApplyQuery();
            applyQuery.setReceiveUserId(userId);
            applyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());

            applyQuery.setLastApplyTimeStart(lastOffTime);
            Integer applyCount = userContactApplyMapper.selectCount(applyQuery);
            wsInitData.setApplyCount(applyCount); // 会话列表  消息列表  申请列表√


            /*
            *  4 发送消息
            * */
            MessageSendDTO messageSendDTO = new MessageSendDTO();
            messageSendDTO.setMessageType(MessageTypeEnum.INIT.getType());
            messageSendDTO.setContactId(userId);
            messageSendDTO.setExtendData(wsInitData);

            sendMsg(messageSendDTO,userId);
        }

        public void addUser2Group(String userId, String groupId) {
            Channel channel = USER_CONTEXT_MAP.get(userId);
            add2Group(groupId, channel);
        }


        // 用于将通道添加到指定的群组 而这个通道来自握手成功后的处理事件中，其中获取当前与服务端连接的客户端的channel
        private void add2Group(String groupId,Channel  channel){
            ChannelGroup group = GROUP_CONTEXT_MAP.get(groupId); // 获取群组 获取的是键值 也就是 群组通道 ChannelGroup
            if(group == null){      // GlobalEventExecutor.INSTANCE是 Netty 提供的一个全局单线程事件执行器，：DefaultChannelGroup 需要一个 EventExecutor 来执行批量操作（如 writeAndFlush）时的任务调度
                group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE); // ChannelGroup 是一个线程安全的Channel的集合 用于管理多个channel
                GROUP_CONTEXT_MAP.put(groupId, group); // 如果当前这个群组id的channel不存在 则给他申请一个 并进行绑定
            }
            if(channel ==  null){
                return;
            }
            group.add(channel); // 将用户连接的Channel添加到群组的ChannelGroup 由ChannelGroup进行统一的管理
        }

        // 移除用户的WebSocket连接上下文并更新用户状态
        public void removeContext(Channel channel){ // attr()方法 获取channel中自定义的AttributeKey的值
            Attribute<String> attribute =  channel.attr(AttributeKey.valueOf(channel.id().toString()));
            String userId = attribute.get();

            if(!StringTools.isEmpty(userId)){
              USER_CONTEXT_MAP.remove(userId);
            }

            redisComponent.removeUserHeartBeat(userId);

            // 更新用户最后离线时间
            User user = new User();
            user.setLastOffTime(System.currentTimeMillis());
            userMapper.updateByUserId(user,userId);

        }


        // 发送消息
        public void sendMessage(MessageSendDTO messageSendDTO){
            UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(messageSendDTO.getContactId().substring(0,1));
            switch (contactTypeEnum){
                case USER:
                    send2User(messageSendDTO);
                    break;
                case GROUP:
                    send2Group(messageSendDTO);
                    break;
            }

        }

        // 单聊 发送给用户
        private void send2User(MessageSendDTO messageSendDTO){
            String contactId = messageSendDTO.getContactId();
            if(contactId == null){
                return;
            }
            sendMsg(messageSendDTO,contactId); // 将消息发送给指定用户
            // 判断消息类型是否为强制下线
            if (MessageTypeEnum.FORCE_OFF_LINE.getType().equals(messageSendDTO.getMessageType())) {
              closeContext(contactId);
            }
        }

        // 强制下线 清除token 关闭通道
        public void closeContext(String userId){
            if(StringTools.isEmpty(userId)){
                return;
            }

            redisComponent.cleanUserTokenByUserId(userId);
            Channel channel = USER_CONTEXT_MAP.get(userId);
            if(channel == null){
                return;
            }

            channel.close();

        }

        // 群聊 发送给群组
        public void send2Group(MessageSendDTO messageSendDTO){
            if(StringTools.isEmpty(messageSendDTO.getContactId())){
                return;
            }
            ChannelGroup channelGroup = GROUP_CONTEXT_MAP.get(messageSendDTO.getContactId());
            if(channelGroup == null){
                return;
            }

            channelGroup.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(messageSendDTO)));

            // ### P37 移除群聊
            MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(messageSendDTO.getMessageType());
            if(MessageTypeEnum.LEAVE_GROUP == messageTypeEnum || MessageTypeEnum.REMOVE_GROUP == messageTypeEnum){
                String userId = (String) messageSendDTO.getExtendData();
                redisComponent.removeUserContact(userId,messageSendDTO.getContactId());
                Channel channel = USER_CONTEXT_MAP.get(userId);
                if(channel==null){
                    return;
                }
                channelGroup.remove(channel);
            }
            if(MessageTypeEnum.DISSOLUTION_GROUP == messageTypeEnum){
                GROUP_CONTEXT_MAP.remove(messageSendDTO.getContactId());
                channelGroup.close();
            }
        }

        // 发送消息
        public void sendMsg(MessageSendDTO messageSendDTO,String receiveId){
            if(receiveId == null){
                return;
            }
            // 获取接收者的通道
            Channel sendChannel = USER_CONTEXT_MAP.get(receiveId);
            if(sendChannel == null){
                return;
            }

            // 相对于客户端而言 联系人就是发送人 所以这里转换一下再发送 好友申请的时候不处理
            if(MessageTypeEnum.ADD_FRIEND_SELF.getType().equals(messageSendDTO.getMessageType())){ // 加好友的特殊消息
               User user = (User) messageSendDTO.getExtendData(); // 获取发送人
               messageSendDTO.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
               messageSendDTO.setContactId(user.getUserId()); // 设置联系人为被添加的用户信息
               messageSendDTO.setContactName(user.getNickName());
               messageSendDTO.setExtendData(null); // 清空扩展数据
            }else{ // 普通消息 则将信息改为发送者的ID和名称 在接收者的客户端进行数据的展示
                messageSendDTO.setContactId(messageSendDTO.getSendUserId());
                messageSendDTO.setContactName(messageSendDTO.getSendUserNickName());
            }

            sendChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(messageSendDTO)));

        }
    }
