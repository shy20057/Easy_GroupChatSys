package com.easychat.controller.admin;


import com.easychat.annotation.GlobalInterceptor;
import com.easychat.controller.ABaseController;
import com.easychat.entity.dto.SaveUpdateDTO;
import com.easychat.entity.dto.SysSettingDTO;
import com.easychat.entity.po.AppUpdate;
import com.easychat.entity.query.AppUpdateQuery;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.vo.ResponseVO;

import com.easychat.service.AppUpdateService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;


@RestController("adminAppUpdateController")
@RequestMapping("/admin")
public class AdminAppUpdateController extends ABaseController {

    @Resource
    private AppUpdateService appUpdateService;

    // 查询app更新列表
    @RequestMapping("/loadUpdateList")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO loadUpdateList(AppUpdateQuery appUpdateQuery) {
        appUpdateQuery.setOrderBy("id desc");
        PaginationResultVO<AppUpdate> resultVO = appUpdateService.findListByPage(appUpdateQuery);

        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/saveUpdate")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO saveUpdate(SaveUpdateDTO saveUpdateDTO) throws IOException {

        AppUpdate appUpdate = new AppUpdate();
        appUpdate.setVersion(saveUpdateDTO.getVersion());
        appUpdate.setUpdateDesc(saveUpdateDTO.getUpdateDesc());
        appUpdate.setVersion(saveUpdateDTO.getVersion());
        appUpdate.setFileType(saveUpdateDTO.getFileType());
        appUpdate.setOuterLink(saveUpdateDTO.getOuterLink());
        appUpdate.setId(saveUpdateDTO.getId());

        appUpdateService.saveUpdate(appUpdate,saveUpdateDTO.getFile());

        return getSuccessResponseVO(null);
    }

    @RequestMapping("/delUpdate")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO delUpdate(@NotNull Integer id) {



        appUpdateService.deleteAppUpdateById(id);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/postUpdate")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO delUpdate(@NotNull Integer id,@NotNull Integer status, String grayscaleUid) {
        appUpdateService.postUpdate(id, status, grayscaleUid);
        return getSuccessResponseVO(null);
    }


}
