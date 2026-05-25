package com.easychat.controller.admin;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.controller.ABaseController;
import com.easychat.entity.config.Appconfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.SysSettingDTO;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.redis.RedisComponent;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

@RestController("adminSettingController")
@RequestMapping("/admin")
public class AdminSettingController extends ABaseController {

    @Resource
    private RedisComponent redisComponent;
    @Resource
    private Appconfig appConfig; // 系统配置

    @RequestMapping("/getSysSetting")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO getSysSetting() {

        SysSettingDTO sysSettingDTO = redisComponent.getSysSetting();
        return getSuccessResponseVO(sysSettingDTO);
    }

    @RequestMapping("/saveSysSetting")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO saveSysSetting(SysSettingDTO sysSettingDTO ,
                                MultipartFile robotFile,MultipartFile robotCover) throws IOException {

        if(robotFile != null){
            String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFile = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
            if(!targetFile.exists()){
                targetFile.mkdirs();
            }
            String filePath = targetFile.getPath() + "/" + Constants.ROBOT_UID + Constants.IMAGE_SUFFIX;
            // 临时文件 已经上传到服务器上面了？？
            robotFile.transferTo(new File(filePath));
            robotCover.transferTo(new File(filePath + Constants.COVER_IMAGE_SUFFIX));
        }
        redisComponent.saveSysSetting(sysSettingDTO); // 把修改的系统设置保存到Redis中
        return getSuccessResponseVO(null);
    }
}
