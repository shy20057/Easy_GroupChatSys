package com.easychat.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;


/**
 * 聊天消息表
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage implements Serializable {


	/**
	 * 消息自增ID
	 */
	private Long messageId;

	/**
	 * 会话ID
	 */
	private String sessionId;

	/**
	 * 消息类型
	 */
	private Integer messageType;

	/**
	 * 消息内容
	 */
	private String messageContent;

	/**
	 * 发送人ID
	 */
	private String sendUserId;

	/**
	 * 发送人昵称
	 */
	private String sendUserNickName;

	/**
	 * 发送时间
	 */
	private Long sendTime;

	/**
	 * 接收联系人ID
	 */
	private String contactId;


	/**
	 * 文件大小
	 */
	private Long fileSize;

	/**
	 * 文件名
	 */
	private String fileName;

	/**
	 * 文件类型
	 */
	private Integer fileType;

	/**
	 * 状态: 0.正在发送；1.已发送
	 */
	private Integer status;

	/**
	 * 联系人 好友 群组
	 */
	private Integer contactType;


}
