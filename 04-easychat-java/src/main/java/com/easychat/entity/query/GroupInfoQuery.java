package com.easychat.entity.query;

import lombok.*;

import java.util.Date;


/**
 * 参数
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupInfoQuery extends BaseParam {


	/**
	 * 群ID
	 */
	private String groupId;

	private String groupIdFuzzy;

	/**
	 * 群姓名
	 */
	private String groupName;

	private String groupNameFuzzy;

	/**
	 * 群主ID
	 */
	private String groupOwnerId;

	private String groupOwnerIdFuzzy;

	/**
	 * 创建时间
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;

	/**
	 * 群公告
	 */
	private String groupNotice;

	private String groupNoticeFuzzy;

	/**
	 * 直接加入:0;管理员同意后加入:1
	 */
	private Integer joinType;

	/**
	 * 状态 1:正常；0:解散
	 */
	private Integer status;

	private Boolean queryMemberCount; // 是否查询群成员数量

	private Boolean queryGroupOwnerName; // 是否查询群主名称




}
