package com.easychat.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * 会话信息
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChatSession implements Serializable {


	/**
	 * 会话ID
	 */
	private String sessionId;

	/**
	 * 最后接受的消息
	 */
	private String lastMessage;

	/**
	 * 最后接受消息时间毫秒
	 */
	private Long lastReceiveTime;




	@Override
	public String toString (){
		return "会话ID:"+(sessionId == null ? "空" : sessionId)+"，最后接受的消息:"+(lastMessage == null ? "空" : lastMessage)+"，最后接受消息时间毫秒:"+(lastReceiveTime == null ? "空" : lastReceiveTime);
	}
}
