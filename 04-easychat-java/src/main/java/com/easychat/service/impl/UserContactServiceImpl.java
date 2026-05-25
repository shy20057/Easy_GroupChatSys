package com.easychat.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.*;
import com.easychat.entity.enums.*;
import com.easychat.entity.po.*;
import com.easychat.entity.query.*;
import com.easychat.exception.BusinessException;
import com.easychat.mappers.*;
import com.easychat.redis.RedisComponent;
import com.easychat.utils.CopyTools;
import com.easychat.websocket.ChannelContextUtils;
import com.easychat.websocket.MessageHandler;
import jodd.util.ArraysUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.service.UserContactService;
import com.easychat.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 联系人 业务接口实现
 */
@Service("userContactService")
public class UserContactServiceImpl implements UserContactService {

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;
	@Resource
	private UserMapper<User, UserQuery> userMapper;
	@Resource
	private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;
	@Resource
	private UserContactApplyMapper<UserContactApply, UserContactApplyQuery> userContactApplyMapper;
    @Autowired
    private UserContactApplyServiceImpl userContactApplyService;
	@Resource
	private RedisComponent redisComponent;
	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;
	@Resource
	private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;
	@Resource
	private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;
	@Resource
	private ChannelContextUtils channelContextUtils;
	@Resource
	private MessageHandler messageHandler;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserContact> findListByParam(UserContactQuery param) {
		return this.userContactMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserContactQuery param) {
		return this.userContactMapper.selectCount(param);
	}
	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserContact> findListByPage(UserContactQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserContact> list = this.findListByParam(param);
		PaginationResultVO<UserContact> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserContact bean) {
		return this.userContactMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserContact> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserContact> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserContact bean, UserContactQuery param) {
		StringTools.checkParam(param);
		return this.userContactMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserContactQuery param) {
		StringTools.checkParam(param);
		return this.userContactMapper.deleteByParam(param);
	}

	/**
	 * 根据UserIdAndContactId获取对象
	 */
	@Override
	public UserContact getUserContactByUserIdAndContactId(String userId, String contactId) {
		return this.userContactMapper.selectByUserIdAndContactId(userId, contactId);
	}

	/**
	 * 根据UserIdAndContactId修改
	 */
	@Override
	public Integer updateUserContactByUserIdAndContactId(UserContact bean, String userId, String contactId) {
		return this.userContactMapper.updateByUserIdAndContactId(bean, userId, contactId);
	}

	/**
	 * 根据UserIdAndContactId删除
	 */
	@Override
	public Integer deleteUserContactByUserIdAndContactId(String userId, String contactId) {
		return this.userContactMapper.deleteByUserIdAndContactId(userId, contactId);
	}

	@Override
	public UserContactSearchResultDTO searchContact(String userId, String contactId) {

		UserContactTypeEnum typeEnum = UserContactTypeEnum.getByPrefix(contactId); // 判断类型 返回的是USER(0,"U","好友")这种枚举中的值 即USER
		if(typeEnum == null){
			return null;
		}
		UserContactSearchResultDTO resultDTO = new UserContactSearchResultDTO();
		switch (typeEnum){
			case USER:
				User user = this.userMapper.selectByUserId(contactId); // 这里的contactId就是userId
				if(user == null){
					return null;
				}
				resultDTO = CopyTools.copy(user, UserContactSearchResultDTO.class);
				break;

			case GROUP:
				GroupInfo groupInfo = this.groupInfoMapper.selectByGroupId(contactId);
				if(groupInfo == null){
					return null;
				}
				//resultDTO = CopyTools.copy(groupInfo, UserContactSearchResultDTO.class);
				resultDTO.setNickName(groupInfo.getGroupName());
				break;
		}

		resultDTO.setContactTypePrefix(typeEnum.getPrefix());
		resultDTO.setContactType(typeEnum.getType() == 0 ? "USER" : "GROUP");
		resultDTO.setContactId(contactId);

		if(userId.equals(contactId)){  // userId登录的这个人 contactId搜索的这个人 or 这个群
			resultDTO.setStatus(UserContactStatusEnum.FRIEND.getCode());
			return resultDTO;
		}
		// 查询是否是好友
		UserContact userContact = this.userContactMapper.selectByUserIdAndContactId(userId, contactId); // contactId是要搜索的人或者群，还是以DTO传更就显而意见
		resultDTO.setStatus(userContact == null ? 0 : userContact.getStatus());

		return resultDTO;
	}


	/**
	 * 申请添加联系人 发出好友申请
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer applyAdd(TokenUserInfoDTO tokenUserInfoDTO, String contactId, String applyInfo) {
		UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId); // 一样的哈，从传进来的contactId中确认是群id还是好友id
		if(contactTypeEnum == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		// 申请人
		String applyUserId = tokenUserInfoDTO.getUserId();
		// 默认申请信息
		applyInfo =StringTools.isEmpty(applyInfo) ? String.format(Constants.APPLY_INFO_TEMPLATE,tokenUserInfoDTO.getNickName()) : applyInfo;

		Long CurTime = System.currentTimeMillis();
		Integer joinType = null;
		String receiveUserId = contactId; // 接收方

		// 查询对方好友是否已经添加 如果已经拉黑无法添加  联系人与群组之间的status是一样的 好友 非好友 被拉黑 拉黑 。。。
		UserContact userContact = this.userContactMapper.selectByUserIdAndContactId(applyUserId,contactId);
		if(userContact != null &&
				ArraysUtil.contains(new Integer[]{
						UserContactStatusEnum.BLACKLIST_BE.getCode(),
						UserContactStatusEnum.BLACKLIST_BE_FIRST.getCode()
				},userContact.getStatus())){// 第二个参数是要检验的状态码 只要这个状态是是上面两个状态中的一种 就返回true
			throw new BusinessException("对方已经将你拉黑，无法添加");
		}

		// 状态判断完成之后 检验群组 额外做业务
		if(UserContactTypeEnum.GROUP == contactTypeEnum){
           GroupInfo groupInfo = this.groupInfoMapper.selectByGroupId(contactId);
		   if(groupInfo == null || GroupStatusEnum.DISSOLUTION.getStatus().equals(groupInfo.getStatus())){
                throw new BusinessException("群聊不存在或已解散");
		   }
		   receiveUserId = groupInfo.getGroupOwnerId(); // 群主id 群主就是接受消息的人
		   joinType = groupInfo.getJoinType(); // 加入方式
		}else{
			// 获取联系人对象
			User contactUser = this.userMapper.selectByUserId(contactId);
			if(contactUser == null){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			// 获取加入方式
			joinType = contactUser.getJoinType();
		}

        // 直接加入不用记录申请记录
		if(JoinTypeEnum.JOIN.getType().equals(joinType)){

			userContactApplyService.addContact(AddContactDTO.builder()
					.applyUserId(applyUserId)
					.contactId(contactId)
					.contactType(contactTypeEnum.getType())
					.applyInfo(applyInfo)
					.applyUserId(applyUserId)
					.receiveUserId(receiveUserId)
					.build());

			return joinType;
		}

		// 如果需要申请的话 这个时候使用 user_contact_apply表 用来记录申请的信息
		UserContactApply dbApply = this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserIdAndContactId(applyUserId,receiveUserId,contactId); // 查询是否已经申请
        if(dbApply == null){ // 未申请过，则添加申请信息到联系人申请表
			UserContactApply contactApply = new UserContactApply();
			contactApply.setApplyUserId(applyUserId);
			contactApply.setContactType(contactTypeEnum.getType()); // 好友申请

			contactApply.setLastApplyTime(CurTime);
			contactApply.setStatus(UserContactApplyStatusEnum.INIT.getStatus()); // 申请处理类型为：待处理
			contactApply.setApplyInfo(applyInfo);
			contactApply.setReceiveUserId(receiveUserId); // 接受方可能是用户id或者群id的群主id
			contactApply.setContactId(receiveUserId);
			this.userContactApplyMapper.insert(contactApply);
		}else{
			// 更新状态
			UserContactApply contactApply = new UserContactApply();
			contactApply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
			contactApply.setLastApplyTime(CurTime);
			contactApply.setApplyInfo(applyInfo);
			this.userContactApplyMapper.updateByApplyId(contactApply,dbApply.getApplyId());
		}

		// Netty 发送消息  dbApply（user_contact_apply表）为空或者处理状态不是待处理 ----> 也就是说之前没有发过申请 且 避免在申请状态是待处理时重复发送消息
		if(dbApply==null || !UserContactApplyStatusEnum.INIT.getStatus().equals(dbApply.getStatus())){

			MessageSendDTO messageSendDTO = MessageSendDTO.builder()
					.messageType(MessageTypeEnum.CONTACT_APPLY.getType()) //消息类型为好友申请消息
					.messageContent(applyInfo) // 消息内容
					.contactId(receiveUserId) // 接收方
					.build();

			//channelContextUtils.sendMsg(messageSendDTO,receiveUserId);
			messageHandler.sendMessage(messageSendDTO);

		}
		return joinType;

	}

	/**
	 * 移除好友
	 * @param userId
	 * @param contactId
	 * @param statusEnum
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeUserContact(String userId, String contactId, UserContactStatusEnum statusEnum) {
		// 移除好友 改变user_contact关系状态
		UserContact userContact = UserContact.builder().build();
		userContact.setStatus(statusEnum.getCode());
		userContactMapper.updateByUserIdAndContactId(userContact,userId,contactId);

		// 将好友中也移除自己
		UserContact friendContact = UserContact.builder().build();
		if(UserContactStatusEnum.DEL == statusEnum){ // 我删除好友
			friendContact.setStatus(UserContactStatusEnum.DEL_BE.getCode()); // 在好友列表就是被删除
		} else if (UserContactStatusEnum.BLACKLIST == statusEnum) {
			friendContact.setStatus(UserContactStatusEnum.BLACKLIST_BE.getCode());
		}
		userContactMapper.updateByUserIdAndContactId(friendContact,contactId,userId);// 反过来 反客为主 双向修改

		// ###P38 从我的好友列表缓存中删除好友
		redisComponent.removeUserContact(userId,contactId);

		//  从好友列表缓存中删除自己
		redisComponent.removeUserContact(contactId,userId);
	}

	 /*
	  添加机器人为好友
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addContact4Robot(String userId) {
		Date curDate = new Date();
		SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
		String contactId = sysSettingDTO.getRobotUid();
		String contactName = sysSettingDTO.getRobotNickName();
		String sendMessage = sysSettingDTO.getRobotWelcome();
		sendMessage = StringTools.cleanHtmlTag(sendMessage); // 清除html标签
		// 增加机器人为好友
		UserContact userContact = UserContact.builder()
				.userId(userId)
				.contactId(contactId)
				.contactType(UserContactTypeEnum.USER.getType())
				.createTime(curDate)
				.lastUpdateTime(curDate)
				.status(UserContactStatusEnum.FRIEND.getCode())
				.build();
		userContactMapper.insert(userContact);
		// 增加会话信息
		String sessionId = StringTools.getChatSessionId4User(new String[]{userId,contactId});
		ChatSession chatSession = ChatSession.builder()
				.sessionId(sessionId)
				.lastMessage(sendMessage)
				.lastReceiveTime(curDate.getTime())
				.build();

		chatSessionMapper.insert(chatSession);

		// 增加会话人信息
		ChatSessionUser chatSessionUser = ChatSessionUser.builder()
				.userId(userId)
				.contactId(contactId)
				.contactName(contactName)
				.sessionId(sessionId)
				.build();

		chatSessionUserMapper.insert(chatSessionUser);

		// 增加聊天消息
		ChatMessage chatMessage = ChatMessage.builder()
				.sessionId(sessionId)
				.messageType(MessageTypeEnum.CHAT.getType())
				.messageContent(sendMessage)
				.sendUserId(contactId)
				.sendUserNickName(contactName)
				.sendTime(curDate.getTime())
				.contactId(userId)
				.contactType(UserContactTypeEnum.USER.getType())
				.status(MessageStatusEnum.SENDED.getStatus())
				.build();

		chatMessageMapper.insert(chatMessage);

	}
}