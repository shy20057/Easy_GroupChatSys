package com.easychat.entity.dto;

import com.easychat.utils.StringTools;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略未知属性
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageSendDTO<T> implements Serializable {

    // 消息ID
    private Long messageId;
    // 会话ID
    private String sessionId;
    // 发送人
    private String sendUserId;
    // 发送人昵称
    private String sendUserNickName;
    // 联系人ID
    private String contactId;
    // 联系人昵称
    private String contactName;
    // 消息内容
    private String messageContent;
    // 最后的消息
    private String lastMessage;
    // 消息类型
    private Integer messageType;
    // 发送时间
    private Long sendTime;
    // 联系人类型
    private Integer contactType;
    // 扩展信息
    private T extendData;

    // 消息状态 0：发送中 1：已发送 对于文件上的异步上传用状态处理
    private Integer status;

    // 文件信息
    private Long fileSize;
    private String fileName;
    private Integer fileType;

    // 群员
    private Integer memberCount;

    public String getLastMessage() {
        if(StringTools.isEmpty(lastMessage)){
            return messageContent;
        }
        return lastMessage;
    }
}
