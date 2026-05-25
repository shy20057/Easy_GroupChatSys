package com.easychat.service;

import java.util.List;

import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.entity.dto.UserContactSearchResultDTO;
import com.easychat.entity.enums.UserContactStatusEnum;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.vo.PaginationResultVO;


/**
 * 联系人 业务接口
 */
public interface UserContactService {

	/**
	 * 根据条件查询列表 Query
	 */
	List<UserContact> findListByParam(UserContactQuery param);

	/**
	 * 根据条件查询总数
	 */
	Integer findCountByParam(UserContactQuery param);

	/**
	 * 分页查询 分页查询
	 */
	PaginationResultVO<UserContact> findListByPage(UserContactQuery param);

	/**
	 * 新增
	 */
	Integer add(UserContact bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserContact> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<UserContact> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(UserContact bean,UserContactQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(UserContactQuery param);


	// ****** 根据主键 IUSD  ******//
	/**
	 * 根据UserIdAndContactId查询对象
	 */
	UserContact getUserContactByUserIdAndContactId(String userId,String contactId);


	/**
	 * 根据UserIdAndContactId修改
	 */
	Integer updateUserContactByUserIdAndContactId(UserContact bean,String userId,String contactId);


	/**
	 * 根据UserIdAndContactId删除
	 */
	Integer deleteUserContactByUserIdAndContactId(String userId,String contactId);

	/**
	 * 搜索联系人
	 */
	UserContactSearchResultDTO searchContact(String userId, String contactId);

	/**
	 * 申请添加联系人
	 */
	Integer applyAdd(TokenUserInfoDTO tokenUserInfoDTO, String contactId, String applyInfo);

	/**
	 * 移除联系人
	 */
	void removeUserContact(String userId, String contactId, UserContactStatusEnum statusEnum);

	void addContact4Robot(String userId);
}