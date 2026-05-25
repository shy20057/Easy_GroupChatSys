package com.easychat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import com.easychat.entity.dto.MessageSendDTO;
import com.easychat.entity.enums.MessageTypeEnum;
import com.easychat.entity.enums.UserContactStatusEnum;
import com.easychat.entity.enums.UserContactTypeEnum;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.mappers.UserContactMapper;
import com.easychat.websocket.MessageHandler;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.easychat.entity.enums.PageSize;
import com.easychat.entity.query.ChatSessionUserQuery;
import com.easychat.entity.po.ChatSessionUser;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.query.SimplePage;
import com.easychat.mappers.ChatSessionUserMapper;
import com.easychat.service.ChatSessionUserService;
import com.easychat.utils.StringTools;


/**
 * 会话用户 业务接口实现
 */
@Service("chatSessionUserService")
public class ChatSessionUserServiceImpl implements ChatSessionUserService {

	@Resource
	private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;
	@Resource
	@Lazy
	private MessageHandler messageHandler;
	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<ChatSessionUser> findListByParam(ChatSessionUserQuery param) {
		return this.chatSessionUserMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(ChatSessionUserQuery param) {
		return this.chatSessionUserMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<ChatSessionUser> findListByPage(ChatSessionUserQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<ChatSessionUser> list = this.findListByParam(param);
		PaginationResultVO<ChatSessionUser> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(ChatSessionUser bean) {
		return this.chatSessionUserMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<ChatSessionUser> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatSessionUserMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<ChatSessionUser> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatSessionUserMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(ChatSessionUser bean, ChatSessionUserQuery param) {
		StringTools.checkParam(param);
		return this.chatSessionUserMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(ChatSessionUserQuery param) {
		StringTools.checkParam(param);
		return this.chatSessionUserMapper.deleteByParam(param);
	}

	/**
	 * 根据UserIdAndContactId获取对象
	 */
	@Override
	public ChatSessionUser getChatSessionUserByUserIdAndContactId(String userId, String contactId) {
		return this.chatSessionUserMapper.selectByUserIdAndContactId(userId, contactId);
	}

	/**
	 * 根据UserIdAndContactId修改
	 */
	@Override
	public Integer updateChatSessionUserByUserIdAndContactId(ChatSessionUser bean, String userId, String contactId) {
		return this.chatSessionUserMapper.updateByUserIdAndContactId(bean, userId, contactId);
	}

	/**
	 * 根据UserIdAndContactId删除
	 */
	@Override
	public Integer deleteChatSessionUserByUserIdAndContactId(String userId, String contactId) {
		return this.chatSessionUserMapper.deleteByUserIdAndContactId(userId, contactId);
	}

	/**
	 * 修改冗余信息 P32
	 */
	@Override
	public void updateRedundantIfo(String contactName,String contactId){
		ChatSessionUser updateInfo = new ChatSessionUser();
		updateInfo.setContactName(contactName);

		ChatSessionUserQuery chatSessionUserQuery = new ChatSessionUserQuery();
		chatSessionUserQuery.setContactId(contactId);
		this.chatSessionUserMapper.updateByParam(updateInfo, chatSessionUserQuery);

		 UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
		 if(contactTypeEnum == UserContactTypeEnum.GROUP){
			 // 修改群昵称，发送ws消息
			 MessageSendDTO messageSendDTO = new MessageSendDTO();
			 messageSendDTO.setContactType(UserContactTypeEnum.getByPrefix(contactId).getType());
			 messageSendDTO.setContactId(contactId);
			 messageSendDTO.setExtendData(contactName);
			 messageSendDTO.setMessageType(MessageTypeEnum.CONTACT_NAME_UPDATE.getType());
			 messageHandler.sendMessage(messageSendDTO);
		 }else{

			 UserContactQuery userContactQuery = new UserContactQuery();
			 userContactQuery.setContactId(contactId);
			 userContactQuery.setContactType(UserContactTypeEnum.USER.getType());
			 userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getCode());
			 List<UserContact> userContactList = this.userContactMapper.selectList(userContactQuery);
			 for(UserContact userContact : userContactList){
				 MessageSendDTO messageSendDTO = new MessageSendDTO();
				 messageSendDTO.setContactType(contactTypeEnum.getType());
				 messageSendDTO.setContactId(userContact.getUserId());
				 messageSendDTO.setExtendData(contactName);
				 messageSendDTO.setMessageType(MessageTypeEnum.CONTACT_NAME_UPDATE.getType());
				 messageSendDTO.setSendUserId(contactId);
				 messageSendDTO.setSendUserNickName(contactName);
				 messageHandler.sendMessage(messageSendDTO);
			 }
		 }



	}


}