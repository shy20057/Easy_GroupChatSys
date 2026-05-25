package com.easychat.controller.admin;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.controller.ABaseController;
import com.easychat.entity.query.UserQuery;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;


@RestController("adminUserInfoController")
@RequestMapping("/admin")
public class AdminUserInfoController extends ABaseController {

    @Resource
    private UserService userService;

    @RequestMapping("/loadUser")
    @GlobalInterceptor(checkAdmin = true) //  登录默认是true  检查管理员权限默认是false
    public ResponseVO loadUser(UserQuery userQuery) {

        userQuery.setOrderBy("create_time desc");
        PaginationResultVO resultVO = userService.findListByPage(userQuery);

        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/updateUserStatus")
    @GlobalInterceptor(checkAdmin = true) //  登录默认是true  检查管理员权限默认是false
    public ResponseVO updateUserStatus(@NotNull Integer status, @NotEmpty String userId) {

        userService.updateUserStatus(userId, status);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/forceOffLine")
    @GlobalInterceptor(checkAdmin = true) //  登录默认是true  检查管理员权限默认是false
    public ResponseVO forceOffLine(@NotNull Integer status, @NotEmpty String userId) {

        userService.forceOffLine(userId);
        return getSuccessResponseVO(null);
    }





}
