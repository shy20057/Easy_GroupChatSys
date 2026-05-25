package com.easychat.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.easychat.entity.enums.DateTimePatternEnum;
import com.easychat.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * 
 */
@Data
public class GroupInfo implements Serializable {


	/**
	 * 群ID
	 */
	private String groupId; // 群ID

	/**
	 * 群姓名
	 */
	private String groupName; // 群姓名

	/**
	 * 群主ID
	 */
	private String groupOwnerId; // 群主ID

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime; // 创建时间

	/**
	 * 群公告
	 */
	private String groupNotice; // 群公告

	/**
	 * 直接加入:0;管理员同意后加入:1
	 */
	private Integer joinType; // 直接加入:0;管理员同意后加入:1

	/**
	 * 状态 1:正常；0:解散
	 */
	private Integer status; // 状态 1:正常；0:解散

	private Integer memberCount; // 成员数量

	private String groupOwnerNickName;




	@Override
	public String toString (){
		return "群ID:"+(groupId == null ? "空" : groupId)+"，群姓名:"+(groupName == null ? "空" : groupName)+"，群主ID:"+(groupOwnerId == null ? "空" : groupOwnerId)+"，创建时间:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，群公告:"+(groupNotice == null ? "空" : groupNotice)+"，直接加入:0;管理员同意后加入:1:"+(joinType == null ? "空" : joinType)+"，状态 1:正常；0:解散:"+(status == null ? "空" : status);
	}


}
