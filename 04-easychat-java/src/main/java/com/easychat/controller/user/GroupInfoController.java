package com.easychat.controller.user;

import java.io.IOException;
import java.util.List;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.controller.ABaseController;
import com.easychat.entity.dto.SaveGroupDTO;
import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.entity.enums.GroupStatusEnum;
import com.easychat.entity.enums.MessageTypeEnum;
import com.easychat.entity.enums.UserContactStatusEnum;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.query.GroupInfoQuery;
import com.easychat.entity.po.GroupInfo;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.entity.vo.GroupInfoVO;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.exception.BusinessException;
import com.easychat.service.GroupInfoService;
import com.easychat.service.UserContactService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;

/**
 *  Controller
 */
@RestController("groupInfoController")
@RequestMapping("/group")
@Validated
public class GroupInfoController extends ABaseController {

	@Resource
	private GroupInfoService groupInfoService;
	@Resource
	private UserContactService userContactService;

	@RequestMapping("/saveGroup")
	@GlobalInterceptor
	public ResponseVO saveGroup(HttpServletRequest request, SaveGroupDTO saveGroupDTO) throws IOException {


		TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request); // 我在登录的时候
        GroupInfo groupInfo = new GroupInfo();
		groupInfo.setGroupId(saveGroupDTO.getGroupId());
		groupInfo.setGroupName(saveGroupDTO.getGroupName());
		groupInfo.setGroupOwnerId(tokenUserInfoDTO.getUserId()); // 群主就是操作的这个人 就是登录令牌的归属人
		groupInfo.setJoinType(saveGroupDTO.getJoinType());
		groupInfo.setGroupNotice(saveGroupDTO.getGroupNotice());


		this.groupInfoService.saveGroup(groupInfo,saveGroupDTO);

		return getSuccessResponseVO(null);
	}

	// 加载我的群组
	@RequestMapping("/loadMyGroup")
	@GlobalInterceptor
	public ResponseVO loadMyGroup(HttpServletRequest request){
		TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request); // 拿到当前登录用户的信息
        GroupInfoQuery groupInfoQuery = new GroupInfoQuery();
		groupInfoQuery.setGroupOwnerId(tokenUserInfoDTO.getUserId());
		groupInfoQuery.setOrderBy("create_time desc"); // 分页相关配置
		List<GroupInfo> groupList = this.groupInfoService.findListByParam(groupInfoQuery);

		return getSuccessResponseVO(groupList);
	}

	/**
	 * 获取群组详情 GroupInfo
	 */
	@RequestMapping("/getGroupInfo")
	@GlobalInterceptor
	public ResponseVO getGroupInfo(HttpServletRequest request, @NotEmpty String groupId)  {

		GroupInfo groupInfo = getGroupDetailCommon(request, groupId);

		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		Integer memberCount = this.userContactService.findCountByParam(userContactQuery);
		groupInfo.setMemberCount(memberCount);

		return getSuccessResponseVO(groupInfo);
	}

	private GroupInfo getGroupDetailCommon(HttpServletRequest request, @NotEmpty String groupId){
		TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);
                                                               // ContactId 联系人id 或者群组id
		UserContact userContact = this.userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDTO.getUserId(), groupId);
		if(userContact == null || !UserContactStatusEnum.FRIEND.getCode().equals(userContact.getStatus())){
			throw new BusinessException("您不在该群聊或者该群聊不存在");
		}

		GroupInfo groupInfo = this.groupInfoService.getGroupInfoByGroupId(groupId);
		if(groupInfo == null || !GroupStatusEnum.NORMAL.getStatus().equals(groupInfo.getStatus())){
			throw new BusinessException("群聊不存在或者已解散");
		}
		return groupInfo;
	}

	@RequestMapping("/getGroupInfo4Chat")
	@GlobalInterceptor
	public ResponseVO getGroupInfo4Chat(HttpServletRequest request, @NotEmpty String groupId)  {

		GroupInfo groupInfo = getGroupDetailCommon(request, groupId);

		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		userContactQuery.setQueryUserInfo(true); // 查询用户信息
		userContactQuery.setOrderBy("create_time desc");
		userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getCode());
		List<UserContact> userContactList = this.userContactService.findListByParam(userContactQuery); // 群组里面的联系人

		Integer memberCount = userContactList == null ? 0 : userContactList.size();
		groupInfo.setMemberCount(memberCount);

		GroupInfoVO groupInfoVO = new GroupInfoVO();
		groupInfoVO.setGroupInfo(groupInfo);
		groupInfoVO.setUserContactList(userContactList);

		return getSuccessResponseVO(groupInfoVO);
	}

	@RequestMapping("/addOrRemoveGroupUser")
	@GlobalInterceptor
	public ResponseVO addOrRemoveGroupUser(HttpServletRequest request,
										   @NotEmpty String groupId,
										   @NotEmpty String selectContacts,
										   @NotEmpty Integer opType)  {

		TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);
		groupInfoService.addOrRemoveGroupUser(tokenUserInfoDTO, groupId, selectContacts, opType);
		return getSuccessResponseVO( null);
	}

	@RequestMapping("/leaveGroup")
	@GlobalInterceptor
	public ResponseVO leaveGroup(HttpServletRequest request,
										   @NotEmpty String groupId)  {

		TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);
		groupInfoService.leaveGroup(tokenUserInfoDTO.getUserId(), groupId, MessageTypeEnum.LEAVE_GROUP);
		return getSuccessResponseVO( null);
	}

	/**
	 * 解散群组
	 */
	@RequestMapping("/dissolutionGroup")
	@GlobalInterceptor
	public ResponseVO dissolutionGroup(HttpServletRequest request,
									   @NotEmpty String groupId){

		TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);
		groupInfoService.dissolutionGroup(tokenUserInfoDTO.getUserId(), groupId);
		return getSuccessResponseVO( null);
	}
}