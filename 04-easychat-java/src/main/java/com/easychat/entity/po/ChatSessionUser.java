package com.easychat.entity.po;

import com.easychat.entity.enums.UserContactTypeEnum;
import com.easychat.utils.StringTools;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * 会话用户
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionUser implements Serializable {


	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 联系人ID
	 */
	private String contactId;

	/**
	 * 会话ID
	 */
	private String sessionId;

	/**
	 * 联系人名称
	 */
	private String contactName;

	private String lastMessage; // 最后一条消息

	private Long lastReceiveTime; // 最后接收时间

	private Integer memberCount; // 群组成员数量

	private Integer contactType;


	public Integer contactType(){
		if(StringTools.isEmpty(contactId)){
			return null;
		}
		return UserContactTypeEnum.getByPrefix(contactId).getType();
	}




	@Override
	public String toString (){
		return "用户ID:"+(userId == null ? "空" : userId)+"，联系人ID:"+(contactId == null ? "空" : contactId)+"，会话ID:"+(sessionId == null ? "空" : sessionId)+"，联系人名称:"+(contactName == null ? "空" : contactName);
	}
}
