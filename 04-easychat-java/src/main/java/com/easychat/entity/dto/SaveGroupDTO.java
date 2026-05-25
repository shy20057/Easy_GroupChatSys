package com.easychat.entity.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class SaveGroupDTO {

    private String groupId; // 群组id
    @NotEmpty
    private String groupName; // 群组名称
    private String groupNotice; // 群组公告
    @NotNull
    private Integer joinType; // 加群方式
    private MultipartFile avatarFile; // 群组头像
    private MultipartFile avatarCover; // 群组头像

}
