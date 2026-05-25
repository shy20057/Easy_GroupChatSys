package com.easychat.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easychat.entity.config.Appconfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.MessageSendDTO;
import com.easychat.entity.dto.SysSettingDTO;
import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.entity.enums.*;
import com.easychat.entity.po.ChatSession;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.query.*;
import com.easychat.exception.BusinessException;
import com.easychat.mappers.ChatSessionMapper;
import com.easychat.mappers.UserContactMapper;
import com.easychat.redis.RedisComponent;
import com.easychat.utils.CopyTools;
import com.easychat.utils.DateUtil;
import com.easychat.utils.JsonUtils;
import com.easychat.websocket.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easychat.entity.po.ChatMessage;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.mappers.ChatMessageMapper;
import com.easychat.service.ChatMessageService;
import com.easychat.utils.StringTools;
import org.springframework.web.multipart.MultipartFile;


/**
 * 聊天消息表 业务接口实现
 */
@Service("chatMessageService")
@Slf4j
public class ChatMessageServiceImpl implements ChatMessageService {

	@Resource
	private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery>  chatSessionMapper;

	@Resource
	private MessageHandler messageHandler;
    @Autowired
    private Appconfig appConfig;

	@Resource
	private UserContactMapper<UserContact,UserContactQuery> userContactMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<ChatMessage> findListByParam(ChatMessageQuery param) {
		return this.chatMessageMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(ChatMessageQuery param) {
		return this.chatMessageMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<ChatMessage> findListByPage(ChatMessageQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<ChatMessage> list = this.findListByParam(param);
		PaginationResultVO<ChatMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(ChatMessage bean) {
		return this.chatMessageMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<ChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatMessageMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<ChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatMessageMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(ChatMessage bean, ChatMessageQuery param) {
		StringTools.checkParam(param);
		return this.chatMessageMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(ChatMessageQuery param) {
		StringTools.checkParam(param);
		return this.chatMessageMapper.deleteByParam(param);
	}

	/**
	 * 根据MessageId获取对象
	 */
	@Override
	public ChatMessage getChatMessageByMessageId(Long messageId) {
		return this.chatMessageMapper.selectByMessageId(messageId);
	}

	/**
	 * 根据MessageId修改
	 */
	@Override
	public Integer updateChatMessageByMessageId(ChatMessage bean, Long messageId) {
		return this.chatMessageMapper.updateByMessageId(bean, messageId);
	}

	/**
	 * 根据MessageId删除
	 */
	@Override
	public Integer deleteChatMessageByMessageId(Long messageId) {
		return this.chatMessageMapper.deleteByMessageId(messageId);
	}


	@Override
	public MessageSendDTO saveMessage(ChatMessage chatMessage, TokenUserInfoDTO tokenUserInfoDTO) {

		// 如果不是机器人回复 判断好友状态
		if(!Constants.ROBOT_UID.equals(tokenUserInfoDTO.getUserId())){
			List<String> contactList = redisComponent.getContactList(tokenUserInfoDTO.getUserId());
			contactList = JsonUtils.convertArray2List(contactList);
			if(!contactList.contains(chatMessage.getContactId())){
				UserContactTypeEnum userContactTypeEnum = UserContactTypeEnum.getByPrefix(chatMessage.getContactId());
				if(userContactTypeEnum == UserContactTypeEnum.USER){
					throw new BusinessException(ResponseCodeEnum.CODE_902);
				}else{
					throw new BusinessException(ResponseCodeEnum.CODE_903);
				}
			}
		}

		// ### P34 发送聊天消息02
		String sessionId = null;
		String sendUserId = tokenUserInfoDTO.getUserId();
		String contactId = chatMessage.getContactId();

		UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);

		if(UserContactTypeEnum.USER == contactTypeEnum){
			sessionId = StringTools.getChatSessionId4User(new String[]{sendUserId,contactId});
		}else{
		    sessionId = StringTools.getChatSessionId4Group(contactId);
		}

		chatMessage.setSessionId(sessionId);
		Long curTime = System.currentTimeMillis();
		chatMessage.setSendTime(curTime);

		MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(chatMessage.getMessageType());
		if(messageTypeEnum == null || !ArrayUtils.contains(new Integer[]{MessageTypeEnum.CHAT.getType(),MessageTypeEnum.MEDIA_CHAT.getType()}, chatMessage.getMessageType())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		Integer status = MessageTypeEnum.MEDIA_CHAT == messageTypeEnum ? MessageStatusEnum.SENDING.getStatus() : MessageStatusEnum.SENDED.getStatus();
		chatMessage.setStatus(status);

		String messageContent = StringTools.cleanHtmlTag(chatMessage.getMessageContent());
		chatMessage.setMessageContent(messageContent);

		// 更新对话
		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(messageContent);
		if(UserContactTypeEnum.GROUP==contactTypeEnum){
			chatSession.setLastMessage(tokenUserInfoDTO.getNickName()+":"+messageContent);
		}
		chatSession.setLastReceiveTime(curTime);
		chatSessionMapper.updateBySessionId(chatSession, sessionId);

		// 记录消息表
		chatMessage.setSendUserId(sendUserId);
		chatMessage.setSendUserNickName(tokenUserInfoDTO.getNickName());
		chatMessage.setContactType(contactTypeEnum.getType());

		chatMessageMapper.insert(chatMessage);

		MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage, MessageSendDTO.class);

		if(Constants.ROBOT_UID.equals(contactId)){
			SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
			TokenUserInfoDTO robot = new TokenUserInfoDTO();
			robot.setUserId(sysSettingDTO.getRobotUid());
			robot.setNickName(sysSettingDTO.getRobotNickName());

			ChatMessage robotMessage = new ChatMessage();
			robotMessage.setContactId(sendUserId);

			// 这里可以对接AI 实现聊天
			robotMessage.setMessageContent("你好呀，有什么需要我帮助的吗？");
			robotMessage.setMessageType(MessageTypeEnum.CHAT.getType());
			saveMessage(robotMessage, robot);

		}else{
			messageHandler.sendMessage(messageSendDTO);

		}

		return messageSendDTO;
	}

	// ### P35 发送消息03 处理文件上传
	@Override
	public void saveMessageFile(String userId, Long messageId, MultipartFile file, MultipartFile cover) {
		ChatMessage chatMessage = chatMessageMapper.selectByMessageId(messageId);
		if(chatMessage == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(!chatMessage.getSendUserId().equals(userId)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
		String fileSuffix = StringTools.getFileSuffix(file.getOriginalFilename());
		if(!StringTools.isEmpty(fileSuffix)
		    && ArrayUtils.contains(Constants.IMAGE_SUFFIX_LIST, fileSuffix.toUpperCase())
		    && file.getSize() > sysSettingDTO.getMaxImageSize()*Constants.FILE_SIZE_MB){

			throw new BusinessException(ResponseCodeEnum.CODE_600);

		}else if(!StringTools.isEmpty(fileSuffix)
				&& ArrayUtils.contains(Constants.VIDEO_SUFFIX_LIST, fileSuffix.toUpperCase())
				&& file.getSize() > sysSettingDTO.getMaxVideoSize()*Constants.FILE_SIZE_MB){

			throw new BusinessException(ResponseCodeEnum.CODE_600);

		} else if(!StringTools.isEmpty(fileSuffix)
				&& !ArrayUtils.contains(Constants.IMAGE_SUFFIX_LIST, fileSuffix.toUpperCase())
				&& !ArrayUtils.contains(Constants.VIDEO_SUFFIX_LIST, fileSuffix.toUpperCase())
		        && file.getSize() > sysSettingDTO.getMaxFileSize()*Constants.FILE_SIZE_MB) {

			throw new BusinessException(ResponseCodeEnum.CODE_600);

		}

		String fileName = file.getOriginalFilename();
		String fileExtName = StringTools.getFileSuffix(fileName);
		String fileRealName = messageId + fileExtName;
		String month = DateUtil.format(new Date(chatMessage.getSendTime()),DateTimePatternEnum.YYYY_MM.getPattern());
		File folder = new File(appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+month);
		if(!folder.exists()){
			folder.mkdirs();
		}

		File uploadFile = new File(folder.getPath()+"/"+fileRealName); // 上传文件
        try {
            file.transferTo(uploadFile);
			cover.transferTo(new File(uploadFile.getPath() + Constants.COVER_IMAGE_SUFFIX));

        } catch (IOException e) {
            log.error("上传文件失败");
			throw new BusinessException("文件上传失败");
        }

		ChatMessage uploadInfo = new ChatMessage();
		uploadInfo.setStatus(MessageStatusEnum.SENDED.getStatus());

		ChatMessageQuery messageQuery = new ChatMessageQuery();
		messageQuery.setMessageId(messageId);
		messageQuery.setStatus(MessageStatusEnum.SENDING.getStatus());
		chatMessageMapper.updateByParam(uploadInfo, messageQuery);
		// 乐观锁
		// update chat_message set status = 1 where message_id = #{messageId} and status = 0
		// 无乐观锁
		// update chat_message set status = 1 where message_id = #{messageId}

		MessageSendDTO messageSendDTO = new MessageSendDTO();
		messageSendDTO.setStatus(MessageStatusEnum.SENDED.getStatus());
		messageSendDTO.setMessageId(messageId);
		messageSendDTO.setMessageType(MessageTypeEnum.FILE_UPLOAD.getType());
		messageSendDTO.setContactId(chatMessage.getContactId());
		messageHandler.sendMessage(messageSendDTO);


    }

	// ### P36 从服务器中下载文件04
	@Override
	public File downloadFile(TokenUserInfoDTO userInfoDTO, Long messageId, Boolean showCover) {
		ChatMessage message = chatMessageMapper.selectByMessageId(messageId);
		String contactId = message.getContactId();
		UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
		if(contactTypeEnum == UserContactTypeEnum.USER && !userInfoDTO.getUserId().equals(message.getContactId())){ //保证联系人是用户 并且是下载自己联系人的图
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(contactTypeEnum == UserContactTypeEnum.GROUP){
			UserContactQuery userContactQuery = new UserContactQuery();
			userContactQuery.setUserId(userInfoDTO.getUserId());
			userContactQuery.setContactType(UserContactTypeEnum.GROUP.getType());
			userContactQuery.setContactId(contactId);
			userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getCode());
			Integer contactCount = userContactMapper.selectCount(userContactQuery);
			if(contactCount == 0){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
		}

		String month = DateUtil.format(new Date(message.getSendTime()),DateTimePatternEnum.YYYY_MM.getPattern());
		File folder = new File(appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+month);
		if(!folder.exists()){
			folder.mkdirs();
		}
		String fileName = message.getFileName();
		String fileExtName = StringTools.getFileSuffix(fileName);
		String fileRealName = messageId + fileExtName;
		if(showCover!= null && showCover){
			fileRealName = fileRealName + Constants.COVER_IMAGE_SUFFIX;
		}

		File file = new File(folder.getPath() + "/" + fileRealName);
		if(!file.exists()){
			log.info("文件不存在{}",messageId);
			throw new BusinessException(ResponseCodeEnum.CODE_602);
		}

		return file;
	}


}
