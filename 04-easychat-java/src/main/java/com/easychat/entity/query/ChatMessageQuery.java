package com.easychat.entity.query;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天消息表参数
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChatMessageQuery extends BaseParam {

	/**
	 * 消息自增ID
	 */
	private Long messageId;

	/**
	 * 会话ID
	 */
	private String sessionId;

	private String sessionIdFuzzy;

	/**
	 * 消息类型
	 */
	private Integer messageType;

	/**
	 * 消息内容
	 */
	private String messageContent;

	private String messageContentFuzzy;

	/**
	 * 发送人ID
	 */
	private String sendUserId;

	private String sendUserIdFuzzy;

	/**
	 * 发送人昵称
	 */
	private String sendUserNickName;

	private String sendUserNickNameFuzzy;

	/**
	 * 发送时间
	 */
	private Long sendTime;

	/**
	 * 接收联系人ID
	 */
	private String contactId;

	private String contactIdFuzzy;

	/**
	 * 文件类型: 0.单聊；1.群聊
	 */
	private Integer contactType;

	/**
	 * 文件大小
	 */
	private Long fileSize;

	/**
	 * 文件名
	 */
	private String fileName;

	private String fileNameFuzzy;

	/**
	 * 文件类型
	 */
	private Integer fileType;

	/**
	 * 状态: 0.正在发送；1.已发送
	 */
	private Integer status;

	private List<String> contactIdList;

	private Long lastReceiveTime;


}
