package com.easychat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * 联系人申请 数据库操作接口
 */
public interface UserContactApplyMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据ApplyId更新  applyId是主键
	 */
	 Integer updateByApplyId(@Param("bean") T t,@Param("applyId") Integer applyId);


	/**
	 * 根据ApplyId删除
	 */
	 Integer deleteByApplyId(@Param("applyId") Integer applyId);


	/**
	 * 根据ApplyId获取对象
	 */
	 T selectByApplyId(@Param("applyId") Integer applyId);


	/**
	 * 根据 申请人ID 接收人ID 联系人/群组ID 更新  复合索引
	 */
	 Integer updateByApplyUserIdAndReceiveUserIdAndContactId(@Param("bean") T t,@Param("applyUserId") String applyUserId,@Param("receiveUserId") String receiveUserId,@Param("contactId") String contactId);


	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId删除
	 */
	 Integer deleteByApplyUserIdAndReceiveUserIdAndContactId(@Param("applyUserId") String applyUserId,@Param("receiveUserId") String receiveUserId,@Param("contactId") String contactId);


	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId获取对象
	 */
	 T selectByApplyUserIdAndReceiveUserIdAndContactId(@Param("applyUserId") String applyUserId,@Param("receiveUserId") String receiveUserId,@Param("contactId") String contactId);


}
