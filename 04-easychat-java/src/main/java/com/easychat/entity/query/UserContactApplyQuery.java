package com.easychat.entity.query;


import lombok.Data;

/**
 * 联系人申请参数
 */
@Data
public class UserContactApplyQuery extends BaseParam {


	/**
	 * 自增ID
	 */
	private Integer applyId;

	/**
	 * 申请人ID
	 */
	private String applyUserId;

	private String applyUserIdFuzzy;

	/**
	 * 接收人ID
	 */
	private String receiveUserId;

	private String receiveUserIdFuzzy;

	/**
	 * 联系人类型 0:好友 1:群组
	 */
	private Integer contactType;

	/**
	 * 联系人/群组ID
	 */
	private String contactId;

	private String contactIdFuzzy;

	/**
	 * 最后申请时间
	 */
	private Long lastApplyTime;

	/**
	 * 状态：待处理 1:已同意 2:已拒绝 3:已拉黑
	 */
	private Integer status;

	/**
	 * 申请信息
	 */
	private String applyInfo;

	private String applyInfoFuzzy;

	private Boolean queryContactInfo;

	private Long LastApplyTimeStart;



}
