package com.easychat.controller.admin;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.controller.ABaseController;
import com.easychat.entity.po.UserInfoBeauty;
import com.easychat.entity.query.UserInfoBeautyQuery;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.service.UserInfoBeautyService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

@RestController("adminUserInfoBeautyController")
@RequestMapping("/admin")
public class AdminUserInfoBeautyController extends ABaseController {

    @Resource
    private UserInfoBeautyService userInfoBeautyService;

    @RequestMapping("/loadBeautyAccountList")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO loadBeautyAccountList(UserInfoBeautyQuery userInfoBeautyQuery) {
        userInfoBeautyQuery.setOrderBy("id desc");
        PaginationResultVO resultVO = userInfoBeautyService.findListByPage(userInfoBeautyQuery);

        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/saveBeautyAccount")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO saveBeautyAccount(UserInfoBeauty userInfoBeauty) {

        userInfoBeautyService.saveBeautyAccount(userInfoBeauty);


        return getSuccessResponseVO(null);
    }

    @RequestMapping("/delBeautyAccount")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO delBeautyAccount(@NotNull Integer id) {

        userInfoBeautyService.deleteUserInfoBeautyById(Long.valueOf(id));


        return getSuccessResponseVO(null);
    }

}
