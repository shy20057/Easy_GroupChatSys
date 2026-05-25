package com.easychat.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easychat.entity.config.Appconfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.*;
import com.easychat.entity.enums.*;
import com.easychat.entity.po.*;
import com.easychat.entity.query.*;
import com.easychat.exception.BusinessException;
import com.easychat.mappers.*;
import com.easychat.redis.RedisComponent;
import com.easychat.service.ChatSessionUserService;
import com.easychat.service.UserContactService;
import com.easychat.utils.CopyTools;
import com.easychat.websocket.ChannelContextUtils;
import com.easychat.websocket.MessageHandler;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.service.GroupInfoService;
import com.easychat.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 *  业务接口实现
 */
@Service("groupInfoService")
public class GroupInfoServiceImpl implements GroupInfoService {

	@Resource
	private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper; // 关联User表和Contact表的Mapper层

	@Resource
	private Appconfig appConfig;

	@Resource
    private ChatSessionMapper<ChatSession,ChatSessionQuery> chatSessionMapper;

	@Resource
	private ChatSessionUserMapper<ChatSessionUser,ChatSessionUserQuery> chatSessionUserMapper;

	@Resource
	private ChannelContextUtils channelContextUtils;
    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private MessageHandler messageHandler;
	@Resource
	private ChatSessionUserService chatSessionUserService;

	@Resource
	private UserContactService userContactService;
    @Resource
    private UserContactApplyServiceImpl userContactApplyService;

	@Resource
	private UserMapper<User,UserQuery> userMapper;
	@Resource
	@Lazy
	private GroupInfoService groupInfoService;


	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<GroupInfo> findListByParam(GroupInfoQuery param) {
		return this.groupInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(GroupInfoQuery param) {
		return this.groupInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<GroupInfo> findListByPage(GroupInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);

		// 根据条件Param查询列表
		List<GroupInfo> list = this.findListByParam(param);
		PaginationResultVO<GroupInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(GroupInfo bean) {
		return this.groupInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<GroupInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.groupInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<GroupInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.groupInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(GroupInfo bean, GroupInfoQuery param) {
		StringTools.checkParam(param);
		return this.groupInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(GroupInfoQuery param) {
		StringTools.checkParam(param);
		return this.groupInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据GroupId获取对象
	 */
	@Override
	public GroupInfo getGroupInfoByGroupId(String groupId) {
		return this.groupInfoMapper.selectByGroupId(groupId);
	}

	/**
	 * 根据GroupId修改
	 */
	@Override
	public Integer updateGroupInfoByGroupId(GroupInfo bean, String groupId) {
		return this.groupInfoMapper.updateByGroupId(bean, groupId);
	}

	/**
	 * 根据GroupId删除
	 */
	@Override
	public Integer deleteGroupInfoByGroupId(String groupId) {
		return this.groupInfoMapper.deleteByGroupId(groupId);
	}


	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveGroup(GroupInfo groupInfo, SaveGroupDTO saveGroupDTO) throws IOException {

		Date curDate = new Date();

       if(!StringTools.isEmpty(groupInfo.getGroupId())){ // groupId不为空 先计算当前有多少个群组 如果还有空位则继续创建

		   GroupInfoQuery groupInfoQuery = new GroupInfoQuery();
		   groupInfoQuery.setGroupOwnerId(groupInfo.getGroupOwnerId());
		   Integer count = this.groupInfoMapper.selectCount(groupInfoQuery); // 这样比较正规 而且关键是在Query的原因是因为他新增了分页的属性
		   SysSettingDTO sysSettingDTO = redisComponent.getSysSetting(); // 获取系统设置
		   if(count >= sysSettingDTO.getMaxGroupCount()){
			   throw new BusinessException("最多支持创建"+sysSettingDTO.getMaxGroupCount()+"个群聊");
		   }

		   if(saveGroupDTO.getAvatarFile() == null){
			   throw new BusinessException(ResponseCodeEnum.CODE_600); // 图片没有
		   }

		   groupInfo.setCreateTime(curDate);
		   groupInfo.setGroupId(StringTools.getGroupId());
		   this.groupInfoMapper.insert(groupInfo);

		   // 将群组添加为联系人 （这里的联系人有两层含义：联系人 or 联系群组）
		   UserContact userContact = new UserContact();
		   userContact.setStatus(UserContactStatusEnum.FRIEND.getCode());
		   userContact.setContactType(UserContactTypeEnum.GROUP.getType());
		   userContact.setContactId(groupInfo.getGroupId());
		   userContact.setUserId(groupInfo.getGroupOwnerId());
		   userContact.setCreateTime(curDate);
		   userContact.setLastUpdateTime(curDate);
		   this.userContactMapper.insert(userContact);

		   /*
		    P31的代码 --- 接收群组申请
		    和p30接收用于消息的逻辑就差不多了
		    */
		   // ###创建会话
		   String sessionId = StringTools.getChatSessionId4Group(groupInfo.getGroupId());  // sessionId是根据联系人用户或群组通过Md5加密得到的
		   ChatSession chatSession = new ChatSession();
		   chatSession.setSessionId(sessionId);
		   chatSession.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
		   chatSession.setLastReceiveTime(curDate.getTime());
           this.chatSessionMapper.insertOrUpdate(chatSession);

		   ChatSessionUser chatSessionUser = new ChatSessionUser();
		   chatSessionUser.setUserId(groupInfo.getGroupOwnerId());
		   chatSessionUser.setContactId(groupInfo.getGroupId());
		   chatSessionUser.setContactName(groupInfo.getGroupName());
		   chatSessionUser.setSessionId(sessionId);
		   this.chatSessionUserMapper.insert(chatSessionUser);

		   // 创建消息
		   ChatMessage chatMessage = ChatMessage.builder()
				   .sessionId(sessionId)
				   .messageType(MessageTypeEnum.GROUP_CREATE.getType())
				   .messageContent(MessageTypeEnum.GROUP_CREATE.getInitMessage())
				   .sendTime(curDate.getTime())
				   .contactId(groupInfo.getGroupId())
				   .contactType(UserContactTypeEnum.GROUP.getType())
				   .status(MessageStatusEnum.SENDED.getStatus())
				   .build();

		   chatMessageMapper.insert(chatMessage);

		   // 将群组添加到联系人
		   redisComponent.addUserContact(groupInfo.getGroupOwnerId(), groupInfo.getGroupId());

		   // 将联系人通道添加到群组通道
		   channelContextUtils.addUser2Group(groupInfo.getGroupOwnerId(), groupInfo.getGroupId());

		   // ###发送ws消息
		   chatSessionUser.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
		   chatSessionUser.setLastReceiveTime(curDate.getTime());
		   chatSessionUser.setMemberCount(1);

		   MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage,MessageSendDTO.class);
		   messageSendDTO.setExtendData(chatSessionUser);
		   messageSendDTO.setLastMessage(chatSessionUser.getLastMessage());

		   messageHandler.sendMessage(messageSendDTO);

	   }else{
		   GroupInfo dbInfo = this.groupInfoMapper.selectByGroupId(groupInfo.getGroupId());
		   if(!dbInfo.getGroupOwnerId().equals(groupInfo.getGroupOwnerId())){
			   throw new BusinessException(ResponseCodeEnum.CODE_600);
		   }
		   this.groupInfoMapper.updateByGroupId(groupInfo, groupInfo.getGroupId());// 根据ID修改数据

		   //### P32
		   //  更新相关表冗余信息
		   // 修改群昵称
		   String contactNameUpdate = null;
		   if(null != dbInfo.getGroupName() && !dbInfo.getGroupName().equals(groupInfo.getGroupName())){
			   contactNameUpdate = dbInfo.getGroupName();
		   }
		   if(contactNameUpdate == null){
			   return;
		   }

		   chatSessionUserService.updateRedundantIfo(contactNameUpdate,groupInfo.getGroupId());


	   }
	   // 保存群组头像
	   if(null == saveGroupDTO.getAvatarFile()){
		   return;
	   }

	   String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE; // 文件保存的根目录 + file
	   File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME); // 文件保存的目录 + file/avatar
	   if(!targetFileFolder.exists()){
		   targetFileFolder.mkdirs();
	   }
	   String filePath = targetFileFolder.getPath()+ "/"+groupInfo.getGroupId() + Constants.IMAGE_SUFFIX;
	   saveGroupDTO.getAvatarFile().transferTo(new File(filePath));
	   saveGroupDTO.getAvatarCover().transferTo(new File(filePath + Constants.COVER_IMAGE_SUFFIX));

	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void dissolutionGroup(String groupOwnerId, String groupId) {
		GroupInfo dbInfo = this.groupInfoMapper.selectByGroupId(groupId);
		// 保证是从这个前端传过来的和数据库查出来的一样的 避免走接口漏洞
		if(dbInfo == null || !dbInfo.getGroupOwnerId().equals(groupOwnerId)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		// 删除群组
		GroupInfo updateInfo = new GroupInfo();
		updateInfo.setStatus(GroupStatusEnum.DISSOLUTION.getStatus());
		this.groupInfoMapper.updateByGroupId(updateInfo, groupId);

		// 更新联系人信息 (or 删除群组成员)
		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		userContactQuery.setContactType(UserContactTypeEnum.GROUP.getType());

		UserContact updateUserContact = new UserContact();
		updateUserContact.setStatus(UserContactStatusEnum.DEL.getCode());
		this.userContactMapper.updateByParam(updateUserContact, userContactQuery);

		// ### P37 移除相关群员的联系人缓存
		List<UserContact> userContactList = this.userContactMapper.selectList(userContactQuery);
		for(UserContact userContact : userContactList){
			redisComponent.removeUserContact(userContact.getUserId(),userContact.getContactId());
		}

		// ### P37 发消息 1，更新会话信息 2，记录群消息 3，发送解散通知消息
		String sessionId = StringTools.getChatSessionId4Group(groupId); // MD5加密
		Date curDate = new Date();
		String messageContent = MessageTypeEnum.DISSOLUTION_GROUP.getInitMessage();

		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(messageContent);
		chatSession.setLastReceiveTime(curDate.getTime());
		chatSessionMapper.updateBySessionId(chatSession, sessionId);

		ChatMessage chatMessage = ChatMessage.builder()
				.sessionId(sessionId)
				.messageType(MessageTypeEnum.DISSOLUTION_GROUP.getType())
				.messageContent(messageContent)
				.sendTime(curDate.getTime())
				.contactId(groupId)
				.contactType(UserContactTypeEnum.GROUP.getType())
				.status(MessageStatusEnum.SENDED.getStatus())
				.build();

		chatMessageMapper.insert(chatMessage);
		MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage,MessageSendDTO.class);
		messageHandler.sendMessage(messageSendDTO);
	}

	@Override
	public void addOrRemoveGroupUser(TokenUserInfoDTO tokenUserInfoDTO, String groupId, String contactIds, Integer opType) {
		GroupInfo groupInfo = groupInfoMapper.selectByGroupId(groupId);
		if(null == groupInfo || !groupInfo.getGroupOwnerId().equals(tokenUserInfoDTO.getUserId())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		String[] contactIdList = contactIds.split(",");
		for(String contactId: contactIdList){
			if(Constants.ZERO.equals(opType)){
                groupInfoService.leaveGroup(contactId,groupId,MessageTypeEnum.REMOVE_GROUP);
			}else{
				AddContactDTO addContactDTO = AddContactDTO.builder()
						.applyUserId(contactId)
						.receiveUserId(null)
						.contactId(groupId)
						.contactType(UserContactTypeEnum.GROUP.getType())
						.applyInfo(null)
						.build();
				userContactApplyService.addContact(addContactDTO);
			}
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void leaveGroup(String userId, String groupId, MessageTypeEnum messageTypeEnum) {
		GroupInfo groupInfo = groupInfoMapper.selectByGroupId(groupId);
		if(null == groupInfo){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(groupInfo.getGroupOwnerId().equals(userId)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		Integer count = userContactMapper.deleteByUserIdAndContactId(userId, groupId);
		if(count == 0){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		User user = userMapper.selectByUserId(userId);

		String sessionId =StringTools.getChatSessionId4Group(groupId);
		Date curDate = new Date();
		String messageContent = String.format(messageTypeEnum.getInitMessage(),user.getNickName());

		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(messageContent);
		chatSession.setLastReceiveTime(curDate.getTime());
		chatSessionMapper.updateBySessionId(chatSession, sessionId);

		ChatMessage chatMessage = ChatMessage.builder()
				.sessionId(sessionId)
				.messageType(messageTypeEnum.getType())
				.messageContent(messageContent)
				.sendTime(curDate.getTime())
				.contactId(groupId)
				.contactType(UserContactTypeEnum.GROUP.getType())
				.status(MessageStatusEnum.SENDED.getStatus())
				.build();

		chatMessageMapper.insert(chatMessage);
		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getCode());
		Integer memberCount = userContactMapper.selectCount(userContactQuery);
		MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage,MessageSendDTO.class);

		messageSendDTO.setExtendData(userId);
		messageSendDTO.setMemberCount(memberCount);
		messageHandler.sendMessage(messageSendDTO);
	}
}