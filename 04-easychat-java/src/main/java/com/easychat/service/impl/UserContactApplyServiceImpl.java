package com.easychat.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easychat.entity.dto.AddContactDTO;
import com.easychat.entity.dto.MessageSendDTO;
import com.easychat.entity.dto.SysSettingDTO;
import com.easychat.entity.enums.*;
import com.easychat.entity.po.*;
import com.easychat.entity.query.*;
import com.easychat.exception.BusinessException;
import com.easychat.mappers.*;
import com.easychat.redis.RedisComponent;
import com.easychat.utils.CopyTools;
import com.easychat.websocket.ChannelContextUtils;
import com.easychat.websocket.MessageHandler;
import org.springframework.stereotype.Service;

import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.service.UserContactApplyService;
import com.easychat.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 联系人申请 业务接口实现
 */
@Service("userContactApplyService")
public class UserContactApplyServiceImpl implements UserContactApplyService {

	@Resource
	private UserContactApplyMapper<UserContactApply, UserContactApplyQuery> userContactApplyMapper;
	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper; // T P
	@Resource
	private RedisComponent redisComponent;
	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;
	@Resource
	private UserMapper<User, UserQuery> userMapper;
	@Resource
	private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;
	@Resource
	private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;
	@Resource
	private MessageHandler messageHandler;
	@Resource
	private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;
	@Resource
	private ChannelContextUtils channelContextUtils;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserContactApply> findListByParam(UserContactApplyQuery param) {
		return this.userContactApplyMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserContactApplyQuery param) {
		return this.userContactApplyMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserContactApply> findListByPage(UserContactApplyQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserContactApply> list = this.findListByParam(param);
		PaginationResultVO<UserContactApply> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserContactApply bean) {
		return this.userContactApplyMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserContactApply> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactApplyMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserContactApply> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactApplyMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserContactApply bean, UserContactApplyQuery param) {
		StringTools.checkParam(param);
		return this.userContactApplyMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserContactApplyQuery param) {
		StringTools.checkParam(param);
		return this.userContactApplyMapper.deleteByParam(param);
	}

	/**
	 * 根据ApplyId获取对象
	 */
	@Override
	public UserContactApply getUserContactApplyByApplyId(Integer applyId) {
		return this.userContactApplyMapper.selectByApplyId(applyId);
	}

	/**
	 * 根据ApplyId修改
	 */
	@Override
	public Integer updateUserContactApplyByApplyId(UserContactApply bean, Integer applyId) {
		return this.userContactApplyMapper.updateByApplyId(bean, applyId);
	}

	/**
	 * 根据ApplyId删除
	 */
	@Override
	public Integer deleteUserContactApplyByApplyId(Integer applyId) {
		return this.userContactApplyMapper.deleteByApplyId(applyId);
	}

	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId获取对象
	 */
	@Override
	public UserContactApply getUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId, String receiveUserId, String contactId) {
		return this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
	}

	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId修改
	 */
	@Override
	public Integer updateUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(UserContactApply bean, String applyUserId, String receiveUserId, String contactId) {
		return this.userContactApplyMapper.updateByApplyUserIdAndReceiveUserIdAndContactId(bean, applyUserId, receiveUserId, contactId);
	}

	/**
	 * 根据ApplyUserIdAndReceiveUserIdAndContactId删除
	 */
	@Override
	public Integer deleteUserContactApplyByApplyUserIdAndReceiveUserIdAndContactId(String applyUserId, String receiveUserId, String contactId) {
		return this.userContactApplyMapper.deleteByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
	}

	// 处理好友申请 ---> 同意 拒绝 拉黑
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void dealWithApply(String userId,Integer applyId,Integer status) {
		UserContactApplyStatusEnum statusEnum = UserContactApplyStatusEnum.getByStatus(status); // 根据前端传进来的状态码Integer获取枚举

		// 判断状态码是否为空或者为初始状态  初始状态要为0待处理我才能给他设置 同意 拒绝 拉黑的状态呀 所以这里传进的不是0 就是传参错误的
		if(statusEnum == null || UserContactApplyStatusEnum.INIT == statusEnum){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		// 判断申请ID是否为空 或者 接受方是不是当前登录的这个人
		UserContactApply applyInfo = this. userContactApplyMapper.selectByApplyId(applyId); // 主键须有前端传进来 进行查询
		if(applyInfo == null || !userId.equals(applyInfo.getReceiveUserId())){ // 判断查询结果是否为空 和 接收人ID是否一致
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		UserContactApply updateInfo = new UserContactApply();
		updateInfo.setStatus(statusEnum.getStatus());
		updateInfo.setLastApplyTime(System.currentTimeMillis());

		// 这里加了一个乐观锁
		// 原本的代码  userContactApplyMapper.updateByApplyId(updateInfo, applyId);
		// 对应的sql语句 update user_contact_apply set status = 1,last_apply_time = now where apply_id = 231244
		//                                               这里的status总是在发生变化，并发的话，会导致数据不一致
		// 加了乐观锁后： update user_contact_apply set status = 1,last_apply_time = now where apply_id = 231244 and status = 0
		// （这样可以保证status走数据更新完成之后在有界面走进数据库改变status的状态）
		// 后端 与 数据库有延迟 导致的并发风险  关键点： 加一层where判断 去判断status状态更新完成没有
		UserContactApplyQuery applyQuery = new UserContactApplyQuery();
		applyQuery.setApplyId(applyId);
		applyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());

		Integer count = userContactApplyMapper.updateByParam(updateInfo, applyQuery);


		if(count == 0){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		if(UserContactApplyStatusEnum.PASS.getStatus().equals(status)){
			AddContactDTO addContactDTO = AddContactDTO.builder()
					.applyUserId(applyInfo.getApplyUserId())
					.contactId(applyInfo.getContactId())
					.contactType(applyInfo.getContactType())
					.receiveUserId(applyInfo.getReceiveUserId())
					.applyInfo(applyInfo.getApplyInfo())
					.build();

			this.addContact(addContactDTO);
			return;
		}

		if(UserContactApplyStatusEnum.REJECT.getStatus().equals(status)){
			//TODO 删除申请记录
			return;
		}

		// 拉黑 核心获取user_contact这张表的中对应数据 并根据得到的数据去找到响应的行信息 把该行的status修改为拉黑
		if(UserContactApplyStatusEnum.BLACKLIST.getStatus().equals(status)){
			Date curDate = new Date();

			UserContact userContact = new UserContact();
			userContact.setUserId(applyInfo.getApplyUserId());
			userContact.setContactId(applyInfo.getContactId());
			userContact.setCreateTime(curDate);
			userContact.setStatus(UserContactStatusEnum.BLACKLIST_BE_FIRST.getCode());
			userContact.setLastUpdateTime(curDate);
			userContactMapper.insertOrUpdate(userContact); // 插入或者修改 只需传进一个实体类

		}

	}

	// 添加联系人 与 添加机器人好友业务逻辑差不多所以认为应该在UserContactServiceImpl里面但我比较懒
	@Override
	public void addContact(AddContactDTO addContactDTO) {

		// 群聊人数
		if(UserContactTypeEnum.GROUP.getType().equals(addContactDTO.getContactType())){
			// 构造Query
			UserContactQuery userContactQuery = new UserContactQuery();
			userContactQuery.setContactId(addContactDTO.getContactId()); // 联系人ID 或者群组ID
			userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getCode()); // 联系人 或者群组

			Integer count = userContactMapper.selectCount(userContactQuery);

			SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
			if(count >= sysSettingDTO.getMaxGroupMemberCount()){
				throw new BusinessException("成员已经满了，无法加入");
			}
		}
		Date curDate = new Date();
        // 同意，双方添加好友
		List<UserContact> contactList = new ArrayList<>();
		// 申请人添加对方
		UserContact userContact = new UserContact();
		userContact.setUserId(addContactDTO.getApplyUserId());
		userContact.setContactId(addContactDTO.getContactId());
		userContact.setContactType(addContactDTO.getContactType());
		userContact.setCreateTime(curDate);
		userContact.setLastUpdateTime(curDate);
		userContact.setStatus(UserContactStatusEnum.FRIEND.getCode());
		contactList.add(userContact);

		// 如果是申请好友的接收人 添加申请人 群组不用添加对方为好友
		if(UserContactTypeEnum.USER.getType().equals(addContactDTO.getContactType())){
			userContact = new UserContact();
			userContact.setUserId(addContactDTO.getContactId()); // 把此时登录的这个人的userId设置为contactId 就是我成了接受方
			userContact.setContactId(addContactDTO.getApplyUserId());// 这里把对方赋予UserId 就是对方变成了发送方
			userContact.setContactType(addContactDTO.getContactType());
			userContact.setCreateTime(curDate);
			userContact.setLastUpdateTime(curDate);
			userContact.setStatus(UserContactStatusEnum.FRIEND.getCode());
			contactList.add(userContact);
		}
		// 批量插入
		userContactMapper.insertOrUpdateBatch(contactList);

		if(UserContactTypeEnum.USER.getType().equals(addContactDTO.getContactType())){
			redisComponent.addUserContact(addContactDTO.getReceiveUserId(), addContactDTO.getApplyUserId()); // 添加对方为好友
		}

		redisComponent.addUserContact(addContactDTO.getApplyUserId(), addContactDTO.getContactId()); // 添加对方为好友

		/*  应该是从这里开始是p30的内容 处理接收用户申请的逻辑 */
		// 创建会话
		String sessionId = null;
		if(UserContactTypeEnum.USER.getType().equals(addContactDTO.getContactType())){
			sessionId = StringTools.getChatSessionId4User(new String[]{addContactDTO.getApplyUserId(),addContactDTO.getContactId()});
		}else{
			sessionId = StringTools.getChatSessionId4Group(addContactDTO.getContactId());
		}

		List<ChatSessionUser> chatSessionUserList = new ArrayList<>();

		if(UserContactTypeEnum.USER.getType().equals(addContactDTO.getContactType())){ // 如果是用户
            // 创建会话
			ChatSession chatSession = new ChatSession();
			chatSession.setSessionId(sessionId);
			chatSession.setLastMessage(addContactDTO.getApplyInfo());
			chatSession.setLastReceiveTime(curDate.getTime());
			this.chatSessionMapper.insertOrUpdate(chatSession);

			ChatSessionUser applySessionUser = new ChatSessionUser();
			applySessionUser.setUserId(addContactDTO.getApplyUserId());
			applySessionUser.setContactId(addContactDTO.getContactId());
			applySessionUser.setSessionId(sessionId);
			User contactUser = this.userMapper.selectByUserId(addContactDTO.getContactId());
			applySessionUser.setContactName(contactUser.getNickName());
			chatSessionUserList.add(applySessionUser);

			// 接收人session
			ChatSessionUser contactSessionUser = new ChatSessionUser();
			contactSessionUser.setUserId(addContactDTO.getContactId());
			contactSessionUser.setContactId(addContactDTO.getApplyUserId());
			contactSessionUser.setSessionId(sessionId);
			User applyUser = this.userMapper.selectByUserId(addContactDTO.getApplyUserId());
			contactSessionUser.setContactName(applyUser.getNickName());
			chatSessionUserList.add(contactSessionUser);

			this.chatSessionUserMapper.insertOrUpdateBatch(chatSessionUserList);

			// 记录消息表
			ChatMessage chatMessage = ChatMessage.builder()
					.sessionId(sessionId)
					.messageType(MessageTypeEnum.ADD_FRIEND.getType())
					.messageContent(addContactDTO.getApplyInfo())
					.sendUserNickName(applyUser.getNickName())
					.sendTime(curDate.getTime())
					.contactId(addContactDTO.getContactId())
					.contactType(UserContactTypeEnum.USER.getType())
					.build();

			this.chatMessageMapper.insert(chatMessage);


			MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage, MessageSendDTO.class);
			// 发送接收 还有 申请的人
			messageHandler.sendMessage(messageSendDTO);

			// 发送申请人 发送人就是接受人 联系人就是申请人
			messageSendDTO.setMessageType(MessageTypeEnum.ADD_FRIEND_SELF.getType());
			messageSendDTO.setContactId(addContactDTO.getApplyUserId());
			messageSendDTO.setExtendData(contactUser);
			messageHandler.sendMessage(messageSendDTO);

		}else{
             // ### P31 --- 让接收者收到群申请信息
			 // 加入群组
			ChatSessionUser chatSessionUser = new ChatSessionUser();
			chatSessionUser.setUserId(addContactDTO.getApplyUserId());
			chatSessionUser.setContactId(addContactDTO.getContactId());

			GroupInfo groupInfo = this.groupInfoMapper.selectByGroupId(addContactDTO.getContactId());
			chatSessionUser.setContactName(groupInfo.getGroupName());
			chatSessionUser.setSessionId(sessionId);
			this.chatSessionUserMapper.insertOrUpdate(chatSessionUser);


			User appyUser = this.userMapper.selectByUserId(addContactDTO.getApplyUserId());
			String sendMessage = String.format(MessageTypeEnum.ADD_GROUP.getInitMessage(),appyUser.getNickName());

			//增加session信息
			ChatSession chatSession = new ChatSession();
			chatSession.setSessionId(sessionId);
			chatSession.setLastReceiveTime(curDate.getTime());
			chatSession.setLastMessage(sendMessage);
			this.chatSessionMapper.insertOrUpdate(chatSession);

			//增加聊天消息
			ChatMessage chatMessage = ChatMessage.builder()
					.sessionId(sessionId)
					.messageType(MessageTypeEnum.ADD_GROUP.getType())
					.messageContent(sendMessage)
					.sendTime(curDate.getTime())
					.contactId(addContactDTO.getContactId())
					.contactType(UserContactTypeEnum.GROUP.getType())
					.status(MessageStatusEnum.SENDED.getStatus())
					.build();

			this.chatMessageMapper.insert(chatMessage);



			// 将群组添加到联系人
			redisComponent.addUserContact(addContactDTO.getApplyUserId(), addContactDTO.getContactId());

			// 将联系人通道添加到群组通道
			channelContextUtils.addUser2Group(addContactDTO.getApplyUserId(), addContactDTO.getContactId());

			// 发送群消息
			MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage, MessageSendDTO.class);
			messageSendDTO.setContactId(addContactDTO.getContactId());

			// 获取群组成员数量

			UserContactQuery userContactQuery = UserContactQuery.builder()
					.contactId(addContactDTO.getContactId())
					.status(UserContactStatusEnum.FRIEND.getCode())
					.build();

			Integer memberCount = this.userContactMapper.selectCount(userContactQuery);

			messageSendDTO.setMemberCount(memberCount);
			messageSendDTO.setContactName(groupInfo.getGroupName());
			// 发消息
			messageHandler.sendMessage(messageSendDTO);


		}

	}


}