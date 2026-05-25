package com.easychat.controller.user;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.controller.ABaseController;
import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.entity.dto.UserContactSearchResultDTO;
import com.easychat.entity.enums.PageSize;
import com.easychat.entity.enums.ResponseCodeEnum;
import com.easychat.entity.enums.UserContactStatusEnum;
import com.easychat.entity.enums.UserContactTypeEnum;
import com.easychat.entity.po.User;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.query.UserContactApplyQuery;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.entity.vo.UserInfoVO;
import com.easychat.exception.BusinessException;
import com.easychat.service.UserContactApplyService;
import com.easychat.service.UserContactService;
import com.easychat.service.UserService;
import com.easychat.utils.CopyTools;
import jodd.util.ArraysUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController("userContactController")
@RequestMapping("/contact")
public class UserContactController extends ABaseController {

    @Resource
    private UserContactService userContactService;
    @Resource
    private UserService userService;
    @Resource
    private UserContactApplyService userContactApplyService;

    // 搜索联系人
    @RequestMapping("/search") // 按联系人id或群组id搜索对应的人信息
    @GlobalInterceptor
    public ResponseVO search(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);
        UserContactSearchResultDTO resultDTO =userContactService.searchContact(tokenUserInfoDTO.getUserId(), contactId);

        return getSuccessResponseVO(resultDTO);
    }

    // 发出好友申请或者群组申请 在user_contact_apply 这张联系表中添加 信息 关联 user_id 和 contact_id
    @RequestMapping("/applyAdd") // 申请添加 这里是登录的人对于搜索的人或者群组发出申请   applyInfo 是申请信息 "你好，我是xxxx"
    @GlobalInterceptor
    public ResponseVO applyAdd(HttpServletRequest request, @NotEmpty String contactId,@NotEmpty String applyInfo) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);
        Integer joinType = userContactService.applyAdd(tokenUserInfoDTO, contactId, applyInfo);

        return getSuccessResponseVO(joinType); // data就是joinType
    }

    // 加载好友申请或者群组申请列表
    @RequestMapping("/loadApply") // 加载申请列表
    @GlobalInterceptor
    public ResponseVO loadApply(HttpServletRequest request,Integer pageNo) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);

        UserContactApplyQuery applyQuery = new UserContactApplyQuery();
        applyQuery.setOrderBy("last_apply_time desc");
        applyQuery.setReceiveUserId(tokenUserInfoDTO.getUserId());
        applyQuery.setPageNo(pageNo);
        applyQuery.setPageSize(PageSize.SIZE15.getSize());
        applyQuery.setQueryContactInfo( true); // 要 查询联系人信息



        PaginationResultVO resultVO = userContactApplyService.findListByPage(applyQuery);

        return getSuccessResponseVO(resultVO);
    }

    // 处理联系人或群组申请列表
    @RequestMapping("/dealWithApply") // 处理联系人申请列表  主要是修改 user_contact_apply中的status
    @GlobalInterceptor
    public ResponseVO dealWithApply(HttpServletRequest request,@NotNull Integer applyId, @NotNull Integer status) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);


        this.userContactApplyService.dealWithApply(tokenUserInfoDTO.getUserId(), applyId, status);


        return getSuccessResponseVO(null);
    }

    // 获取联系人或群组列表
    @RequestMapping("/loadContact")
    @GlobalInterceptor
    public ResponseVO loadContact(HttpServletRequest request,@NotNull String contactType) {

        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByName(contactType); // 由枚举类型 获取枚举
        if(contactTypeEnum == null){
            throw  new BusinessException(ResponseCodeEnum.CODE_600);
        }

        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request); // 获取当前用户的信息

        UserContactQuery  userContactQuery = new UserContactQuery(); // 创建查询条件 联系人列表去添加
        userContactQuery.setUserId(tokenUserInfoDTO.getUserId()); // userId ---- contactId
        userContactQuery.setContactType(contactTypeEnum.getType());
        if(UserContactTypeEnum.USER == contactTypeEnum){ // 判断是不是 用户 其实这里用传进来的contactType == USER就行了
            userContactQuery.setQueryContactUserInfo(true); // 则设置要查询联系人信息

        }else if(UserContactTypeEnum.GROUP == contactTypeEnum){ // 群组  这里展示在我加入的群组列表中，因此要排除我的群组
            userContactQuery.setQueryGroupInfo(true); // 则设置要查询群组信息
            userContactQuery.setExcludeMyGroup(true); // 排除掉我的群组
        }
        userContactQuery.setOrderBy("last_update_time desc"); // 排序
        userContactQuery.setStatusArray(new Integer[]{ // 只查询好友、被黑名单、被删除的
                UserContactStatusEnum.FRIEND.getCode(),
                UserContactStatusEnum.BLACKLIST_BE.getCode(),
                UserContactStatusEnum.DEL_BE.getCode()
        });

        List<UserContact> contactList = userContactService.findListByParam(userContactQuery); // 得到最终的查询Query 在编写sql去查
        return getSuccessResponseVO(contactList);
    }

    // 获取联系人信息 不一定是好友
    @RequestMapping("/getContactInfo")
    @GlobalInterceptor
    public ResponseVO getContactInfo(HttpServletRequest request,@NotEmpty String contactId) {

        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);

        User user = userService.getUserByUserId(contactId); // 获取联系人 对于联系人而言 contactId 就是 userId
        UserInfoVO userInfoVO = CopyTools.copy(user, UserInfoVO.class);
        //userInfoVO.setContactStatus(UserContactStatusEnum.NOT_FRIEND.getCode());

        // 这里做映射 获取联系人状态Status 所以上面初始设置一个非好友的默认值
        UserContact userContact = userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDTO.getUserId(), contactId);
        if(userContact != null){ // 如果信息是存在的则变成好友 ？？？
            userInfoVO.setContactStatus(UserContactStatusEnum.FRIEND.getCode());
        }

        return getSuccessResponseVO(userInfoVO);
    }

    // 获取联系人的用户信息（主页展示） 一定是好友
    @RequestMapping("/getContactUserInfo")
    @GlobalInterceptor
    public ResponseVO getContactUserInfo(HttpServletRequest request, @NotEmpty String contactId) {

        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);

        UserContact userContact = userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDTO.getUserId(), contactId);
        if(userContact == null || !ArraysUtil.contains(new Integer[]{
                UserContactStatusEnum.FRIEND.getCode(), //好友
                UserContactStatusEnum.DEL_BE.getCode(), //被删除
                UserContactStatusEnum.BLACKLIST_BE.getCode() //被拉黑 我的好友列表中展示这三钟状态
        }, userContact.getStatus())){ // 如果信息是存在的则变成好友 ？？？
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        User user = userService.getUserByUserId(contactId); // 获取联系人 对于联系人而言 contactId 就是 userId
        UserInfoVO userInfoVO = CopyTools.copy(user, UserInfoVO.class);
        userInfoVO.setContactStatus(UserContactStatusEnum.FRIEND.getCode());

        return getSuccessResponseVO(userInfoVO);
    }

    // 查到好友信息之后就可以删除联系人
    @RequestMapping("/delContact")
    @GlobalInterceptor
    public ResponseVO delContact(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);

        userContactService.removeUserContact(tokenUserInfoDTO.getUserId(), contactId,UserContactStatusEnum.DEL);

        return getSuccessResponseVO(null);
    }

    // 查到好友信息之后就可以拉黑联系人
    @RequestMapping("/addContact2BlackList")
    @GlobalInterceptor
    public ResponseVO addContact2BlackList(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);

        userContactService.removeUserContact(tokenUserInfoDTO.getUserId(), contactId,UserContactStatusEnum.BLACKLIST);

        return getSuccessResponseVO( null);
    }
}
