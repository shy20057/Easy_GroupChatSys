package com.easychat.entity.dto;

import lombok.Data;

@Data
public class UserContactSearchResultDTO {
    private String contactId; // 联系人id
    private String contactType; // 联系人类型 好友，群组 USER GROUP
    private String contactTypePrefix;
    private String nickName;
    private Long avatarLastUpdate;
    private Integer status; // 状态 0:非好友 1:好友 2:已删除好友 3:被好友删除 4:已拉黑好友 5:被好友拉黑
//    private String statusName;
    private Integer sex;
    private String areaName;
}
