package com.easychat.controller.admin;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.controller.ABaseController;
import com.easychat.entity.config.Appconfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.enums.AppUpdateFileTypeEnum;
import com.easychat.entity.po.AppUpdate;
import com.easychat.entity.vo.AppUpdateVO;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.service.AppUpdateService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.StringTools;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;

@RestController("updateController")
@RequestMapping("/update")
public class UpdateController extends ABaseController {

    @Resource
    private AppUpdateService appUpdateService;
    @Resource
    private Appconfig appConfig;

    @RequestMapping("checkVersion")
    @GlobalInterceptor
    public ResponseVO checkVersion(String appVersion,String uid){

        if(StringTools.isEmpty(appVersion)){
            return getSuccessResponseVO(null);
        }

        AppUpdate appUpdate = appUpdateService.getLastUpdate(appVersion, uid);
        if(appUpdate == null){
            return getSuccessResponseVO(null);
        }

        AppUpdateVO updateVO = CopyTools.copy(appUpdate, AppUpdateVO.class);
        if(AppUpdateFileTypeEnum.LOCAL.getType().equals(appUpdate.getFileType())){
            File file = new File(appConfig.getProjectFolder() + Constants.APP_UPDATE_FOLDER + appUpdate.getId()+ Constants.APP_EXE_SUFFIX);
            updateVO.setSize(file.length());
        }else{
            updateVO.setSize(0L);
        }
        updateVO.setUpdateList(Arrays.asList(appUpdate.getUpdateDescArray()));
        String fileName =  Constants.APP_NANE + appUpdate.getVersion() + Constants.APP_EXE_SUFFIX;
        updateVO.setFileName(fileName);
        return getSuccessResponseVO(updateVO);
    }
}
