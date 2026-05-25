package com.easychat.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddContactDTO {
    private String applyUserId; // 申请人ID
    private String receiveUserId; // 接收人ID
    private String contactId; // 联系人ID 或者群组ID
    private Integer contactType; // 联系人类型
    private String applyInfo; // 申请信息

}
