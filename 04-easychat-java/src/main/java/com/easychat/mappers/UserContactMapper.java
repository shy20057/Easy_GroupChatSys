package com.easychat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * 联系人 数据库操作接口
 */
public interface UserContactMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据UserId And ContactId更新 复合主键
	 */
	 Integer updateByUserIdAndContactId(@Param("bean") T t,@Param("userId") String userId,@Param("contactId") String contactId);


	/**
	 * 根据UserId And ContactId删除
	 */
	 Integer deleteByUserIdAndContactId(@Param("userId") String userId,@Param("contactId") String contactId);


	/**
	 * 根据UserId And ContactId获取对象
	 */
	 T selectByUserIdAndContactId(@Param("userId") String userId,@Param("contactId") String contactId);


}
