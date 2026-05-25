package com.easychat.entity.dto;

import com.easychat.entity.po.ChatMessage;
import com.easychat.entity.po.ChatSession;
import com.easychat.entity.po.ChatSessionUser;
import lombok.Data;

import java.util.List;

@Data
public class WsInitData {
    // 会话列表
    private List<ChatSessionUser> chatSessionList;

    // 消息列表
    private List<ChatMessage> chatMessageList;

    // 申请列表
    private Integer applyCount;
}
