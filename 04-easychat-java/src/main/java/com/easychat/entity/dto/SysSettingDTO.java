package com.easychat.entity.dto;

import com.easychat.entity.constants.Constants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysSettingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer maxGroupCount = 5;
    private Integer maxGroupMemberCount = 500;
    private Integer maxImageSize = 2;
    private Integer maxVideoSize = 5;
    private Integer maxFileSize = 5;
    private String  robotUid = Constants.ROBOT_UID;
    private String  robotNickName = "小冰";
    private String robotWelcome = "欢迎来到EasyChat";


}
