package com.easychat.service;

import java.io.File;
import java.util.List;

import com.easychat.entity.dto.MessageSendDTO;
import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.entity.enums.MessageTypeEnum;
import com.easychat.entity.query.ChatMessageQuery;
import com.easychat.entity.po.ChatMessage;
import com.easychat.entity.vo.PaginationResultVO;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;


/**
 * 聊天消息表 业务接口
 */
public interface ChatMessageService {

	/**
	 * 根据条件查询列表
	 */
	List<ChatMessage> findListByParam(ChatMessageQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(ChatMessageQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<ChatMessage> findListByPage(ChatMessageQuery param);

	/**
	 * 新增
	 */
	Integer add(ChatMessage bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<ChatMessage> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<ChatMessage> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(ChatMessage bean,ChatMessageQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(ChatMessageQuery param);

	/**
	 * 根据MessageId查询对象
	 */
	ChatMessage getChatMessageByMessageId(Long messageId);


	/**
	 * 根据MessageId修改
	 */
	Integer updateChatMessageByMessageId(ChatMessage bean,Long messageId);


	/**
	 * 根据MessageId删除
	 */
	Integer deleteChatMessageByMessageId(Long messageId);

    /**
     * 保存消息
     * @param chatMessage
     * @param tokenUserInfoDTO
     * @return
     */
    MessageSendDTO saveMessage(ChatMessage chatMessage, TokenUserInfoDTO tokenUserInfoDTO);

	/**
	 * 保存消息文件
	 * @param userId
	 * @param messageId
	 * @param file
	 * @param cover
	 */
	void saveMessageFile(String userId, Long messageId, MultipartFile file, MultipartFile cover);

	File downloadFile(TokenUserInfoDTO userInfoDTO,Long fileId , Boolean showCover);
}