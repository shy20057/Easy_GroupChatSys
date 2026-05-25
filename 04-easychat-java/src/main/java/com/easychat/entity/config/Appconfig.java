package com.easychat.entity.config;

import com.easychat.utils.StringTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Description: 配置类
 * @Author: shy
 * @Date: 2020/5/31 16:05
 */
@Component("appConfig")
public class Appconfig {
    /*
    *  webSocket 端口
    * */
    @Value("${ws.port:}")
    private Integer wsPort;
    /*
    *  文件目录
    * */
    @Value("${project.folder:}")
    private String projectFolder;

    @Value("${admin.emails:}")
    private String adminEmails;

    public Integer getWsPort() {
        return wsPort;
    }

    public String getProjectFolder() {
        if(StringTools.isEmpty(projectFolder) && projectFolder.endsWith("/")){
            return projectFolder;
        }
        return projectFolder;
    }

    public String getAdminEmails() {
        return adminEmails;
    }
}
