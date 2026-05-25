package com.easychat.controller.user;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.controller.ABaseController;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.entity.po.User;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.entity.vo.UserInfoVO;
import com.easychat.service.UserService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.StringTools;
import com.easychat.websocket.ChannelContextUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.io.IOException;

@RestController("userInfoController")
@RequestMapping("/userInfo")
public class UserInfoController extends ABaseController {

    @Resource
    private UserService userService;
    @Resource
    private ChannelContextUtils channelContextUtils;

    // 获取用户信息 就是自己主页的信息
    @RequestMapping("/getUserInfo")
    @GlobalInterceptor
    public ResponseVO getUserInfo(HttpServletRequest request) {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);

        User user = userService.getUserByUserId(tokenUserInfoDTO.getUserId());
        UserInfoVO userInfoVO = CopyTools.copy(user, UserInfoVO.class);
        userInfoVO.setAdmin(tokenUserInfoDTO.getAdmin());

        return getSuccessResponseVO(userInfoVO);
    }

    // 修改用户信息
    @RequestMapping("/saveUserInfo")
    @GlobalInterceptor
    public ResponseVO saveUserInfo(HttpServletRequest request, User user,
                                   MultipartFile avatarFile,
                                   MultipartFile avatarCover) throws IOException {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);

        user.setUserId(tokenUserInfoDTO.getUserId()); // 设置用户id
        user.setPassword( null);
        user.setStatus( null);
        user.setCreateTime( null);
        user.setLastLoginTime( null); //不能让别人随便该，不然会击垮后端

        this.userService.updateUserInfo(user, avatarFile, avatarCover);

        return getUserInfo(request);
    }

    // 修改密码
    @RequestMapping("/updatePassword")
    @GlobalInterceptor
    public ResponseVO updatePassword(HttpServletRequest request,
               @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String password) throws IOException {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);

        User user = new User();
        user.setPassword(StringTools.encodeMd5( password)); // 因为数据表中是已经加密过的了，所以这里要加密一下 换成一样被加密的
        this.userService.updateUserByUserId(user, tokenUserInfoDTO.getUserId());
        //### P37 强制退出 重新登录
        channelContextUtils.closeContext(tokenUserInfoDTO.getUserId());


        return getSuccessResponseVO(null);

    }

    // 修改密码
    @RequestMapping("/logout")
    @GlobalInterceptor
    public ResponseVO logout(HttpServletRequest request) throws IOException {
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO(request);

        //### P37 退出登录 关闭ws链接
        channelContextUtils.closeContext(tokenUserInfoDTO.getUserId());


        return getSuccessResponseVO(null);

    }
}
